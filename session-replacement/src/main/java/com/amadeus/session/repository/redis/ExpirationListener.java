package com.amadeus.session.repository.redis;

import static redis.clients.util.SafeEncoder.encode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionManager;

import redis.clients.jedis.BinaryJedisPubSub;

/**
 * This class listens to expiration events coming from Redis, and when the event
 * specifies a key starting with
 * {@link RedisSessionRepository#DEFAULT_SESSION_EXPIRE_PREFIX}, it tries to
 * expire corresponding session
 */
class ExpirationListener extends BinaryJedisPubSub {
  private static Logger logger = LoggerFactory.getLogger(ExpirationListener.class);
  // We subscribe to expired events
  private static final String SUBSCRIPTION_PATTERN = "__keyevent@*__:expired";
  private final SessionManager sessionManager;
  // Suffix for expired notifications
  private static final byte[] EXPIRED_SUFFIX = encode(":expired");
  private final byte[] keyPrefix;
  private boolean subsrcibed;

  /**
   * Standard constructor.
   *
   * @param sessionManager
   *          session manager to call when session expires
   * @param keyPrefix
   *          the prefix of the keys
   */
  ExpirationListener(SessionManager sessionManager, String keyPrefix) {
    super();
    this.sessionManager = sessionManager;
    this.keyPrefix = encode(keyPrefix);
  }

  @Override
  public void onPMessage(byte[] pattern, byte[] channelBuf, byte[] message) {
    // Only accept messages expiration notification channel
    // and only those that match our key prefix.
    if (channelBuf == null || message == null) {
      return;
    }
    if (!isExpiredChannel(channelBuf)) {
      return;
    }
    if (!isExpireKey(message)) {
      return;
    }

    String body = encode(message);
    if (logger.isDebugEnabled()) {
      logger.debug("Got notification for channel: '{}', body: '{}'", encode(channelBuf), body);
    }

    String sessionId = RedisSessionRepository.extractSessionId(body);
    logger.info("Session expired event for sessionId: '{}'", sessionId);

    // We run session delete in another thread, otherwise we would block
    // listener.
    sessionManager.deleteAsync(sessionId, true);
  }

  /**
   * Checks if channel identifies expired notification channel.
   *
   * @param channelBuf
   *          array containing channel information
   * @return <code>true</code> if channel identifies expired notifications
   *         channel
   */
  private boolean isExpiredChannel(byte[] channelBuf) {
    int suffixLength = EXPIRED_SUFFIX.length;
    int channelPos = channelBuf.length - suffixLength;
    if (channelPos <= 0) {
      return false;
    }
    for (int i = 0; i < suffixLength; i++, channelPos++) {
      if (channelBuf[channelPos] != EXPIRED_SUFFIX[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if passed message contains an expire key.
   *
   * @param message
   *          array that might contain the key
   * @return <code>true</code> if message contained expire key
   */
  private boolean isExpireKey(byte[] message) {
    int prefixLength = keyPrefix.length;
    if (message.length < prefixLength) {
      return false;
    }
    for (int i = 0; i < prefixLength; i++) {
      if (message[i] != keyPrefix[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Starts subscription to redis notifications. This is a blocking operation
   * and the thread which called this method will block on opened socket.
   *
   * @param redis
   *          facade to redis library
   */
  void start(RedisFacade redis) {
    subsrcibed = true;
    redis.psubscribe(this, SUBSCRIPTION_PATTERN);
  }

  /**
   * Stops subscription to redis notifications. Call to this method will unblock
   * thread waiting on PSUBSCRIBE.
   */
  void close() {
    if (subsrcibed) {
      punsubscribe(encode(SUBSCRIPTION_PATTERN));
      subsrcibed = false;
    }
  }
}
