package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.RedisSessionRepository.extractSessionId;
import static redis.clients.util.SafeEncoder.encode;

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
  private static final Long ONE = Long.valueOf(1L);
  /**
   * 10 second cleanup interval
   */
  private static final int REGULAR_CLEANUP_INTERVAL = 10;

  private final RedisFacade redis;
  private final RedisSessionRepository repository;
  private final byte[] sessionToExpireKey;
  private ScheduledFuture<?> cleanupFuture;

  /**
   * Creates instance of ZRANGE (sorted set) based expiration management
   *
   * @param redis
   *          facade to redis library
   * @param redisSession
   *          session repository
   * @param namespace
   *          the namespace of the sessions
   */
  SortedSetSessionExpirationManagement(RedisFacade redis, RedisSessionRepository redisSession, String namespace) {
    super();
    this.redis = redis;
    this.repository = redisSession;
    this.sessionToExpireKey = encode(ALLSESSIONS_KEY + namespace);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.repository.redis.ExpirationManagement#sessionDeleted(
   * com.amadeus.session.RepositoryBackedSession)
   */
  @Override
  public void sessionDeleted(SessionData session) {
    redis.zrem(sessionToExpireKey, repository.sessionKey(session.getId()));
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.repository.redis.ExpirationManagement#manageExpiration(
   * com.amadeus.session.RepositoryBackedSession)
   */
  @Override
  public void sessionTouched(SessionData session) {
    byte[] sessionKey = repository.sessionKey(session.getId());
    int sessionExpireInSeconds = session.getMaxInactiveInterval();

    // If session doesn't expire, then remove expire key and persist session
    if (sessionExpireInSeconds <= 0) {
      redis.persist(sessionKey);
      redis.zadd(sessionToExpireKey, Double.MAX_VALUE, sessionKey);
    } else {
      // If session expires, then add session key to expirations cleanup
      // instant, set expire on
      // session and set expire on session expiration key
      redis.zadd(sessionToExpireKey, session.expiresAt(), sessionKey);
      redis.expire(sessionKey, sessionExpireInSeconds + SESSION_PERSISTENCE_SAFETY_MARGIN);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.ExpirationManagement#
   * cleanupExpiredSessionsTask(com.amadeus.session.SessionManager)
   */
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

      logger.info("Cleaning up sessions expiring at {}", now);
      Set<byte[]> sessionsToExpire = redis.zrangeByScore(sessionToExpireKey, 0, now);
      if (sessionsToExpire != null && !sessionsToExpire.isEmpty()) {
        for (byte[] session : sessionsToExpire) {
          // There is no reason to mark the below as
          // findbugs:VA_PRIMITIVE_ARRAY_PASSED_TO_OBJECT_VARARG,
          // as zrem signature explicitly expects byte[] varargs.
          if (ONE.equals(redis.zrem(sessionToExpireKey, session))) { // NOSONAR
            String sessionId = extractSessionId(encode(session));

            logger.debug("Starting cleanup of session '{}'", sessionId);
            sessionManager.delete(sessionId, true);
          }
        }
      }
    }
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
    redis.zrem(sessionToExpireKey, repository.sessionKey(sessionData.getOldSessionId()));
    redis.zadd(sessionToExpireKey, sessionData.expiresAt(), repository.sessionKey(sessionData.getId()));
  }
}