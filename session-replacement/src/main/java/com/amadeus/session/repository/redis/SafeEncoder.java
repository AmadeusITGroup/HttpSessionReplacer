package com.amadeus.session.repository.redis;

import java.nio.charset.StandardCharsets;

/**
 * Similar to implmentation in jedis
 */
final class SafeEncoder {

  private SafeEncoder() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Encodes string in byte array using UTF-8.
   *
   * @param str
   *          string to encode
   * @return encoded byte array
   */
  public static byte[] encode(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("value sent to redis cannot be null");
    }
    return str.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Encodes string from byte array using UTF-8.
   *
   * @param data
   *          byte array
   * @return decoded sring
   */
  public static String encode(final byte[] data) {
    return new String(data, StandardCharsets.UTF_8);
  }
}
