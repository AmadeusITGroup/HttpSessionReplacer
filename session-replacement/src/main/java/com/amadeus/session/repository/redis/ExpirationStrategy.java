package com.amadeus.session.repository.redis;

/**
 * Strategy used to detect expired sessions.
 */
public enum ExpirationStrategy {
  /**
   * Strategy based on expire notifications
   */
  @Deprecated
  PUBSUB,
  /**
   * Strategy based on expire notifications
   */
  NOTIF,
  /**
   * Strategy based on redis ZRANGE
   */
  ZRANGE
}
