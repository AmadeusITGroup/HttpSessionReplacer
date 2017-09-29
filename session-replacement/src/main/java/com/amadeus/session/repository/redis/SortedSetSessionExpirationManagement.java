package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;

/**
 * A strategy for expiring session instances based on Redis Sorted Set (ZRANGE).
 * <p>
 * In this strategy, two keys are used for each session. Main key contains
 * session data and is managed by {@link RedisSessionRepository}. The second key
 * is the sorted set key where all sessions are stored using expiration
 * expiration time as the score.
 * <p>
 * A task is run periodically (every second) and retrieves all sessions that
 * have expired up to the moment.
 * <p>
 * Following risks are possible:
 * <ul>
 * <li>For long running requests, session may expire before request completes. A
 * node not operating the session can then remove session data from Redis. To
 * mitigate the issue application can call various methods that effectively
 * trigger session 'touch' thus pushing the expiry date further in the future.
 * <li>Two nodes may initiate session expire at similar times and may request
 * delete of same session. Delete process should implement logic that performs
 * session delete atomically.
 * <ul>
 */
class SortedSetSessionExpirationManagement implements RedisExpirationStrategy {
  private static Logger logger = LoggerFactory.getLogger(SortedSetSessionExpirationManagement.class.getName());
  static final String ALLSESSIONS_KEY = "com.amadeus.session:all-sessions-set:";
  static final int SESSION_PERSISTENCE_SAFETY_MARGIN = (int)TimeUnit.MINUTES.toSeconds(5);
  private static final long SESSION_PERSISTENCE_SAFETY_MARGIN_MILLIS = TimeUnit.MINUTES.toMillis(5);
  private static final Long ONE = Long.valueOf(1L);
  /**
   * 10 second cleanup interval
   */
  private static final int REGULAR_CLEANUP_INTERVAL = 10;

  private final RedisFacade redis;
  private final RedisSessionRepository repository;
  private final byte[] sessionToExpireKey;
  private final boolean sticky;
  private ScheduledFuture<?> cleanupFuture;
  private final String owner;
  private final byte[] ownerAsBytes;

  /**
   * Creates instance of ZRANGE (sorted set) based expiration management
   *
   * @param redis
   *          facade to redis library
   * @param redisSession
   *          session repository
   * @param namespace
   *          the namespace of the sessions
   * @param sticky
   *          true if sessions are sticky to server
   * @param owner
   *          the node that is owner of this expire management
   */
  SortedSetSessionExpirationManagement(RedisFacade redis, RedisSessionRepository redisSession, String namespace, 
                                       boolean sticky, String owner) {
    super();
    this.redis = redis;
    this.repository = redisSession;
    this.sessionToExpireKey = encode(ALLSESSIONS_KEY + namespace);
    this.sticky = sticky;
    this.owner = owner;
    this.ownerAsBytes = owner != null ? SafeEncoder.encode(owner) : null;
  }

  @Override
  public void sessionDeleted(SessionData session) {
    if (sticky) {
      if (session.getPreviousOwner() != null && !owner.equals(session.getPreviousOwner())) {
        redis.zrem(sessionToExpireKey, encode(session.getId() + ":" + session.getPreviousOwner()));
      }
    } 
    redis.zrem(sessionToExpireKey, sortedSetElem(session.getId()));
  }

  private byte[] sortedSetElem(String id) {
    if (!sticky) {
      return encode(id);
    }
    return sortedSetOwnerElem(id);
  }

  private byte[] sortedSetOwnerElem(String id) {
    return encode(id + ":" + owner);
  }

  @Override
  public void sessionTouched(SessionData session) {
    byte[] sessionKey = repository.sessionKey(session.getId());
    int sessionExpireInSeconds = session.getMaxInactiveInterval();

    // If session doesn't expire, then remove expire key and persist session
    if (sessionExpireInSeconds <= 0) {
      redis.persist(sessionKey);
      redis.zadd(sessionToExpireKey, Double.MAX_VALUE, sortedSetElem(session.getId()));
    } else {
      // If session expires, then add session key to expirations cleanup
      // instant, set expire on
      // session and set expire on session expiration key
      redis.zadd(sessionToExpireKey, session.expiresAt(), sortedSetElem(session.getId()));
      redis.expire(sessionKey, sessionExpireInSeconds + SESSION_PERSISTENCE_SAFETY_MARGIN);
    }
  }

  @Override
  public void startExpiredSessionsTask(final SessionManager sessionManager) {
    Runnable task = new CleanupTask(sessionManager);
    // Interval of polling is either 1/10th of the maximum inactive interval, or
    // every REGULAR_CLEANUP_INTERVAL (10) seconds, whichever is smaller
    long interval = Math.min(sessionManager.getConfiguration().getMaxInactiveInterval() / 10 + 1, // NOSONAR
        REGULAR_CLEANUP_INTERVAL);
    if (interval <= 0) {
      interval = REGULAR_CLEANUP_INTERVAL;
    }
    logger.debug("Cleanup interval for sessions is {}", interval);
    cleanupFuture = sessionManager.schedule("redis.expiration-cleanup", task, interval);
  }

  /**
   * Private class used to perform session expiration. It will retrieve
   * ZRANGEBYSCORE with values up to now instant, and then for each retrieved
   * key, it will expire it.
   */
  final class CleanupTask implements Runnable {
    
    private final SessionManager sessionManager;

    CleanupTask(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
      long now = System.currentTimeMillis();
      long start = sticky ? now - SESSION_PERSISTENCE_SAFETY_MARGIN_MILLIS : 0;
      
      logger.debug("Cleaning up sessions expiring at {}", now);
      expireSessions(start, now, !sticky);
      if (sticky) {
        expireSessions(0, start, true);
      }
    }

    /**
     * Retrieves session keys from sorted set
     * 
     * @param start
     *          earliest instant for session to retrieve
     * @param end
     *          latest instant for session to retrieve
     * @param forceExpire
     *          if set to true, sessions are expired even if they don't belong to this node
     */
    private void expireSessions(long start, long end, boolean forceExpire) {
      Set<byte[]> sessionsToExpire = redis.zrangeByScore(sessionToExpireKey, start, end);
      if (sessionsToExpire != null && !sessionsToExpire.isEmpty()) {
        for (byte[] session : sessionsToExpire) {
          if (forceExpire || sessionOwned(session)) {
            if (ONE.equals(redis.zrem(sessionToExpireKey, session))) { // NOSONAR
              String sessionId = extractSessionId(session);
  
              logger.debug("Starting cleanup of session '{}'", sessionId);
              sessionManager.delete(sessionId, true);
            }
          }
        }
      }      
    }
  }

  /**
   * Extracts session id from byte array stripping owner node if it
   * was present.
   * 
   * @param session
   *          byte array containing session
   * @return session id as string
   */
  private String extractSessionId(byte[] session) {
    if (sticky) {
      for (int i=0; i<session.length; i++) {
        if (session[i] == ':') {
          return encode(session, 0, i);
        }
      }
      logger.warn("Unable to retrieve session id from expire key {}", encode(session));
      // Missing session owner, assume whole array is session id
    }
    return encode(session);
    
  }
  /**
   * Checks if passed message belongs to this node.
   *
   * @param session
   *          array that contains session and owner
   * @return <code>true</code> if session is owned by this node
   */
  private boolean sessionOwned(byte[] session) {
    if (!sticky) {
      return false;
    }
    if (session.length < ownerAsBytes.length + 1) {
      return false;
    }
    for (int i = ownerAsBytes.length-1, j = session.length-1; i >= 0; i--, j--) {
      if (session[j] != ownerAsBytes[i]) {
        return false;
      }
    }
    if (session[session.length - ownerAsBytes.length - 1] != ':') {
      return false;
    }
    return true;
  }

  @Override
  public void close() {
    if (cleanupFuture != null) {
      cleanupFuture.cancel(true);
      cleanupFuture = null;
    }
  }

  @Override
  public void sessionIdChange(SessionData sessionData) {
    redis.zrem(sessionToExpireKey, sortedSetElem(sessionData.getOldSessionId()));
    redis.zadd(sessionToExpireKey, sessionData.expiresAt(), sortedSetElem(sessionData.getId()));
  }
}