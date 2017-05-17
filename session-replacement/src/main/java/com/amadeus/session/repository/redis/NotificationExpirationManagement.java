package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.WrappedException;
import com.amadeus.session.repository.redis.RedisFacade.ResponseFacade;
import com.amadeus.session.repository.redis.RedisFacade.TransactionRunner;

/**
 * A strategy for expiring session instances. This performs several operations:
 *
 * Several keys are used for each session. Main key contains session data.
 * Another key is created and used only for session expire event. The session
 * expire event will trigger the cleanup of the session. The third key is using
 * minutes as key index and contains a set of indexes of all sessions that
 * expire in the minute preceding the minute specified by index key. Following
 * is the discussion of the algorithm sourced from Spring Session project.
 *
 * Redis has no guarantees of when an expired session event will be fired. In
 * order to ensure expired session events are processed in a timely fashion the
 * expiration (rounded to the nearest minute) is mapped to all the sessions that
 * expire at that time. Whenever {@link #cleanExpiredSessions()} is invoked, the
 * sessions for the previous minute are then accessed to ensure they are deleted
 * if expired. All nodes are running a task that does this monitoring, but only
 * the first node that polls Redis for a given key will be responsible for
 * session expiration as we SMEMBERS and DEL on the key within single Redis
 * transaction.
 *
 * In some instances the {@link #cleanExpiredSessions()} method may not be not
 * invoked for a specific time. For example, this may happen when a server is
 * restarted. To account for this, the expiration on the Redis session is also
 * set. For example, if none of the nodes was active during minute following
 * session expiration, the check will not be done, and sessions will silently
 * expire.
 *
 * In stickiness scenario only the node owning the session will delete session
 * on expire event. When using node stickiness, the expire keys contain also
 * node identifier. When listener receives event that this key expires, it
 * checks if key prefix matches the node's one. If it is the case, the session
 * will be deleted. We need to handle also the case when the owner node doesn't
 * receive this notification (e.g. the node is down, there was network issue,
 * Redis servers are busy). For this reason we add `forced-expirations` key that
 * is set one minute after the `expirations` key. It has almost same semantics
 * and logic, with the only difference being that the key is different and it is
 * set to expire one minute later.
 */
class NotificationExpirationManagement implements RedisExpirationStrategy {
  static Logger logger = LoggerFactory.getLogger(NotificationExpirationManagement.class);

  private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

  private static final int SPOP_BULK_SIZE = 1000;

  // 300 seconds margin
  static final int SESSION_PERSISTENCE_SAFETY_MARGIN = 300;
  private static final byte[] EMPTY_STRING = encode("");
  static final String DEFAULT_SESSION_EXPIRE_PREFIX = "com.amadeus.session:expire:";
  static final byte[] DEFAULT_SESSION_EXPIRE_PREFIX_BUF = encode(DEFAULT_SESSION_EXPIRE_PREFIX);
  /**
   * After this number of milliseconds, forget that there was an issue with
   * connectivity. 377 is 14th Fibonacci's number
   */
  private static final long RESET_RETRY_THRESHOLD = SECONDS.toMillis(377);
  /**
   * Exponential back-off is based on Fibonacci numbers - i.e. wait 0 seconds, 1
   * second, 1 second, 3 second etc.
   */
  private static final int[] FIBONACCI_DELAY_PATTERN = new int[] { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233 };
  /**
   * Maximum number of retries before aborting exponential back-off.
   */
  private static final int MAX_CONNECTION_ERRORS = FIBONACCI_DELAY_PATTERN.length;

  private final RedisFacade redis;
  private final RedisSessionRepository repository;
  private final String keyExpirePrefix;
  private final String expirationsPrefix;
  private final String forcedExpirationsPrefix;
  private final String namespace;
  private final String owner;
  private final boolean sticky;
  private ExpirationListener expirationListener;
  private ScheduledFuture<?> cleanupFuture;
  private ScheduledFuture<?> forceCleanupFuture;

  NotificationExpirationManagement(RedisFacade redis, RedisSessionRepository redisSession, String namespace,
      String owner, String keyPrefix, boolean sticky) {
    super();
    this.redis = redis;
    this.repository = redisSession;
    this.sticky = sticky;
    this.namespace = namespace;
    this.owner = owner;
    if (sticky) {
      this.forcedExpirationsPrefix = keyPrefix + "forced-expirations:";
      keyExpirePrefix = constructKeyExpirePrefix(owner);
    } else {
      this.forcedExpirationsPrefix = null;
      keyExpirePrefix = DEFAULT_SESSION_EXPIRE_PREFIX + ":" + namespace + ":";
    }
    expirationsPrefix = keyPrefix + "expirations:";
  }

  private String constructKeyExpirePrefix(String sessionOwner) {
    return DEFAULT_SESSION_EXPIRE_PREFIX + ":" + sessionOwner + ":" + namespace + ":";
  }

  @Override
  public void sessionDeleted(SessionData session) {
    long expireCleanupInstant = roundUpToNextMinute(session.expiresAt());
    byte[] expireKey = getExpirationsKey(expireCleanupInstant);
    redis.srem(expireKey, repository.sessionKey(session.getId()));
    byte[] sessionExpireKey = getSessionExpireKey(session.getId());
    redis.del(sessionExpireKey);
  }

  @Override
  public void sessionTouched(SessionData session) {
    new ExpirationManagement().manageExpiration(session);
  }

  /**
   * Cleans sessions that where left "hanging", i.e. they were not deleted when
   * expire event was triggered. The hanging session are expired on the node
   * that first discovered the hanging session, and not necessarily the node
   * that owned the session.
   */
  final class CleanHangingSessionsTask implements Runnable {
    private final SessionManager sessionManager;

    CleanHangingSessionsTask(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
      long prevMin = roundDownMinute(System.currentTimeMillis());

      logger.info("Cleaning up sessions expiring at {}", prevMin);
      byte[] key = getForcedExpirationsKey(prevMin);
      Set<byte[]> sessionsToExpire = getKeysToExpire(key);
      if (sessionsToExpire == null || sessionsToExpire.isEmpty()) {
        return;
      }
      for (byte[] session : sessionsToExpire) {
        if (logger.isDebugEnabled()) {
          logger.debug("Cleaning-up session {}", new String(session));
        }
        // check if session is active
        if (redis.exists(repository.getSessionKey(session))) {
          // We run session delete in another thread, otherwise we would
          // block this thread listener.
          sessionManager.deleteAsync(encode(session), true);
        }
      }
    }
  }

  /**
   * This class checks if there are any sessions which should have expired in
   * previous minutes, but for which there were no processed notification. If
   * such sessions are found, the expiration notification is triggered using
   * EXISTS command.
   */
  final class TriggerExpiredSessionsTask implements Runnable {
    @Override
    public void run() {
      long prevMin = roundDownMinute(System.currentTimeMillis());

      logger.info("Triggering up sessions expiring at {}", prevMin);
      byte[] key = getExpirationsKey(prevMin);
      Set<byte[]> sessionsToExpire = getKeysToExpire(key);
      if (sessionsToExpire == null || sessionsToExpire.isEmpty()) {
        return;
      }
      for (byte[] session : sessionsToExpire) {
        if (logger.isDebugEnabled()) {
          logger.debug("Expiring session {}", new String(session));
        }
        byte[] sessionExpireKey = getSessionExpireKey(encode(session));
        // Exists will trigger expire event. See explanation of the
        // algorithm for the details.
        redis.exists(sessionExpireKey);
      }
    }
  }

  /**
   * Helper class that implements expiration logic
   */
  final class ExpirationManagement {
    private long expireCleanupInstant;
    private byte[] sessionKey;
    private int sessionExpireInSeconds;
    private byte[] expirationsKey;
    long forceCleanupInstant;
    byte[] forceExpirationsKey;

    void manageExpiration(SessionData session) {
      prepareKeys(session);
      manageCleanupKeys(session);
      manageSessionFailover(session);
      byte[] sessionExpireKey = getSessionExpireKey(session.getId());

      // If session doesn't expire, then remove expire key and persist session
      if (sessionExpireInSeconds <= 0) {
        redis.del(sessionExpireKey);
        redis.persist(sessionKey);
      } else {
        // If session expires, then add session key to expirations cleanup
        // instant, set expire on
        // session and set expire on session expiration key
        redis.sadd(expirationsKey, sessionKey);
        redis.expireAt(expirationsKey,
            MILLISECONDS.toSeconds(expireCleanupInstant) + SESSION_PERSISTENCE_SAFETY_MARGIN);
        if (sticky) {
          redis.sadd(forceExpirationsKey, sessionKey);
          redis.expireAt(forceExpirationsKey,
              MILLISECONDS.toSeconds(forceCleanupInstant) + SESSION_PERSISTENCE_SAFETY_MARGIN);
        }
        redis.setex(sessionExpireKey, sessionExpireInSeconds, EMPTY_STRING);
        redis.expire(sessionKey, sessionExpireInSeconds + SESSION_PERSISTENCE_SAFETY_MARGIN);
      }
    }

    private void manageSessionFailover(SessionData session) {
      // If stickiness is active, and there was failover, we need to delete
      // previous session expire key
      if (sticky && !owner.equals(session.getPreviousOwner())) {
        redis.del(getSessionExpireKey(session.getPreviousOwner(), session.getId()));
      }
    }

    /**
     * Sets up all keys used during expiration management. Those are session
     * key, key for session cleanup and optionally clean for forced session
     * cleanup when using sticky sessions.
     */
    private void prepareKeys(SessionData session) {
      sessionKey = repository.sessionKey(session.getId());
      sessionExpireInSeconds = session.getMaxInactiveInterval();
      expireCleanupInstant = roundUpToNextMinute(session.expiresAt());
      expirationsKey = getExpirationsKey(expireCleanupInstant);
      if (sticky) {
        forceCleanupInstant = roundUpToNextMinute(expireCleanupInstant);
        forceExpirationsKey = getForcedExpirationsKey(forceCleanupInstant);
      } else {
        forceCleanupInstant = 0;
        forceExpirationsKey = null;
      }
    }

    private void manageCleanupKeys(SessionData session) {
      if (!session.isNew()) {
        long originalCleanupInstant = roundUpToNextMinute(session.getOriginalLastAccessed());
        if (expireCleanupInstant != originalCleanupInstant) {
          byte[] originalExpirationsKey = getExpirationsKey(originalCleanupInstant);
          redis.srem(originalExpirationsKey, sessionKey);
          if (sticky) {
            long originalForceCleanupInstant = roundUpToNextMinute(expireCleanupInstant);
            byte[] originalForcedExpirationsKey = getForcedExpirationsKey(originalForceCleanupInstant);
            redis.srem(originalForcedExpirationsKey, sessionKey);
          }
        } else if (sessionExpireInSeconds <= 0) {
          // If session doesn't expire, remove it from expirations key
          redis.srem(expirationsKey, sessionKey);
          if (sticky) {
            redis.srem(forceExpirationsKey, sessionKey);
          }
        }
      }
    }
  }

  /**
   * Rounds up time to next minute (and 0 seconds).
   *
   * @param timeInMs
   *          time to round up
   * @return timestamp of next minute
   */
  private static long roundUpToNextMinute(long timeInMs) {
    Calendar date = Calendar.getInstance();
    date.setTimeInMillis(timeInMs);
    date.add(Calendar.MINUTE, 1);
    date.clear(Calendar.SECOND);
    date.clear(Calendar.MILLISECOND);
    return date.getTimeInMillis();
  }

  /**
   * Rounds up time to minute that precedes time instant.
   *
   * @param timeInMs
   *          time instant
   * @return timestamp of previous minute
   */
  static long roundDownMinute(long timeInMs) {
    Calendar date = Calendar.getInstance();
    date.setTimeInMillis(timeInMs);
    date.clear(Calendar.SECOND);
    date.clear(Calendar.MILLISECOND);
    return date.getTimeInMillis();
  }

  @Override
  public void startExpiredSessionsTask(final SessionManager sessionManager) {
    sessionManager.submit(null, new SubscriptionRunner(sessionManager));

    // The task that triggers clean up session for which the expire notification
    // were
    // not received by nodes.
    Runnable taskTriggerExpiration = new TriggerExpiredSessionsTask();
    cleanupFuture = sessionManager.schedule("redis.expiration-cleanup", taskTriggerExpiration, ONE_MINUTE);
    if (sticky) {
      // When we have sticky sessions, we perform also second pass to capture
      // sessions
      // that were not cleaned by the node that last accessed them
      Runnable taskForceExpiration = new CleanHangingSessionsTask(sessionManager);
      forceCleanupFuture = sessionManager.schedule("redis.force-cleanup", taskForceExpiration, ONE_MINUTE);
    }
  }

  Set<byte[]> getKeysToExpire(byte[] key) {
    // In Redis 3.2 we use SPOP to get bulk of keys to expire
    if (!redis.supportsMultiSpop()) {
      return redis.transaction(key, smembersAndDel(key)).get();
    } else {
      Set<byte[]> res = redis.spop(key, SPOP_BULK_SIZE);
      if (res == null || res.isEmpty() || res.size() < SPOP_BULK_SIZE) {
        redis.del(key);
      }
      return res;
    }
  }

  /**
   * Creates Redis transaction that invokes atomically SMEMBERS and DEL on the
   * given key
   *
   * @param key
   *          the key for Redis set
   * @return result of SMEMBERS call
   */
  static TransactionRunner<Set<byte[]>> smembersAndDel(final byte[] key) {
    return new RedisFacade.TransactionRunner<Set<byte[]>>() {
      @Override
      public RedisFacade.ResponseFacade<Set<byte[]>> run(RedisFacade.TransactionFacade transaction) {
        ResponseFacade<Set<byte[]>> result = transaction.smembers(key);
        transaction.del(key);
        return result;
      }
    };
  }

  /**
   * Builds key whose expire pub event is used to expire session
   *
   * @param id
   *          session id
   * @return key as byte array
   */
  byte[] getSessionExpireKey(String id) {
    return encode(new StringBuilder(keyExpirePrefix.length() + id.length() + 1).append(keyExpirePrefix).append('{')
        .append(id).append('}').toString());
  }

  /**
   * Builds key whose expire pub event is used to expire session
   *
   * @param owner
   *          node that was owner of the session
   * @param id
   *          session id
   * @return key as byte array
   */
  byte[] getSessionExpireKey(String owner, String id) {
    String ownerBasedPrefix = constructKeyExpirePrefix(owner);
    return encode(new StringBuilder(ownerBasedPrefix.length() + id.length() + 1).append(ownerBasedPrefix).append('{')
        .append(id).append('}').toString());
  }
  /**
   * Builds expirations key for the instant in milliseconds. Key is used to
   * clean up sessions for which we didn't receive expire event.
   *
   * @param instant
   *          the instant for which to create key
   * @return key as byte array
   */
  private byte[] getExpirationsKey(long instant) {
    String exp = Long.toString(instant);
    return encode(
        new StringBuilder(expirationsPrefix.length() + exp.length()).append(expirationsPrefix).append(exp).toString());
  }

  /**
   * Builds forced expirations key for the instant in milliseconds. Key is used
   * to clean up sessions for which we didn't receive expire event as the owner
   * node is down
   *
   * @param instant
   *          the instant for which to create key
   * @return key as byte array
   */
  private byte[] getForcedExpirationsKey(long instant) {
    String exp = Long.toString(instant);
    return encode(new StringBuilder(forcedExpirationsPrefix.length() + exp.length()).append(forcedExpirationsPrefix)
        .append(exp).toString());
  }

  /**
   * Subscription task that listens to redis expiration notification. If an
   * exception while listening to notifications occurs, a exponential back-off
   * strategy is applied. The task will retry to establish connection in
   * increasing interval until it succeeds or until maximum number of retries
   * has been made.
   */
  class SubscriptionRunner implements Runnable {

    private final SessionManager sessionManager;
    int attempt;
    long lastConnect;

    SubscriptionRunner(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
      logger.info("Registering subscriber for expiration events.");
      lastConnect = System.currentTimeMillis();
      attempt = 0;
      while (true) {
        try {
          // Currently listening to all databases __keyevent@*:expire
          expirationListener = new ExpirationListener(sessionManager, keyExpirePrefix);
          expirationListener.start(redis);
          logger.info("Stopped subscribing for expiration events.");
          return;
        } catch (Exception e) { // NOSONAR
          if (Thread.interrupted()) {
            return;
          }
          if (redis.isRedisException(e) && e.getCause() instanceof InterruptedException) {
            logger.warn("Interrupted subscribtion for expiration events.");
            return;
          }
          retryOnException(e);
          if (Thread.interrupted()) {
            return;
          }
        }
      }
    }

    /**
     * When an exception occurs we will retry connect with a back-off strategy.
     *
     * @param e
     *          exception that occurred
     */
    void retryOnException(Exception e) {
      logger.error("Failure during subscribing to redis events. Will be retrying...", e);
      long instant = System.currentTimeMillis();
      long delta = instant - lastConnect;
      // If last connectivity was long time ago, forget it.
      if (delta > RESET_RETRY_THRESHOLD) {
        attempt = 0;
      } else {
        attempt++;
        if (attempt >= MAX_CONNECTION_ERRORS) {
          logger.error(
              "Unable to connect to redis servers after trying {} times. " + "Stopped listening to expiration events.",
              attempt);
          throw new IllegalStateException("Stopped listening to expiration events.", e);
        } else {
          doWait();
          // Assume this connectivity will succeed.
          lastConnect = instant;
        }
      }
    }

    /**
     * Wait using exponential back-off strategy.
     */
    void doWait() {
      try {
        Thread.sleep(getDelay());
      } catch (InterruptedException e) { // NOSONAR sleep was interrupted,
        throw new WrappedException(e);
      }
    }

    long getDelay() {
      return SECONDS.toMillis(FIBONACCI_DELAY_PATTERN[attempt]);
    }
  }

  @Override
  public void close() {
    if (expirationListener != null) {
      expirationListener.close(redis);
      expirationListener = null;
    }
    if (cleanupFuture != null) {
      cleanupFuture.cancel(true);
      cleanupFuture = null;
    }
    if (forceCleanupFuture != null) {
      forceCleanupFuture.cancel(true);
      forceCleanupFuture = null;
    }
  }

  @Override
  public void sessionIdChange(SessionData session) {
    redis.rename(getSessionExpireKey(session.getOldSessionId()), getSessionExpireKey(session.getId()));
    // Update clean-up sets
    long expireCleanupInstant = roundUpToNextMinute(session.expiresAt());
    byte[] expirationsKey = getExpirationsKey(expireCleanupInstant);
    byte[] sessionKey = repository.sessionKey(session.getId());
    byte[] oldSessionKey = repository.sessionKey(session.getOldSessionId());
    redis.srem(expirationsKey, oldSessionKey);
    redis.sadd(expirationsKey, sessionKey);
    if (sticky) {
      long forceCleanupInstant = roundUpToNextMinute(expireCleanupInstant);
      byte[] forceExpirationsKey = getForcedExpirationsKey(forceCleanupInstant);
      redis.srem(forceExpirationsKey, oldSessionKey);
      redis.sadd(forceExpirationsKey, sessionKey);
    }

  }
}