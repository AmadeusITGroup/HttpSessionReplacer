package com.amadeus.session;

import static com.amadeus.session.SessionConfiguration.DEFAULT_SESSION_ID_LENGTH;
import static com.amadeus.session.SessionConfiguration.SESSION_ID_LENGTH;

import java.security.SecureRandom;

/**
 * Generates id consisting of random character strings of a given length in bytes.
 *
 * Characters in string are one of following:
 * <code>ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_</code>
 */
public class RandomIdProvider implements SessionIdProvider {

  private static final int DEFAULT_ID_LENGTH = 30;

  private final SecureRandom random = new SecureRandom();

  private int length;

  /**
   * Default id length is 30 bytes.
   */
  public RandomIdProvider() {
    this(DEFAULT_ID_LENGTH);
  }

  /**
   * Creates provider that generates id string encoding length bytes.
   *
   * @param length
   */
  RandomIdProvider(int length) {
    this.length = length;
  }

  @Override
  public String newId() {
    final byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return new String(Base64MaskingHelper.encode(bytes));
  }

  @Override
  public String readId(String value) {
    if (value == null) {
      return null;
    }
    String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      return null;
    }
    if (trimmedValue.length() != getLengthInCharacters()) {
      return null;
    }
    return trimmedValue;
  }

  @Override
  public void configure(SessionConfiguration configuration) {
    String sessionIdLength = configuration.getAttribute(SESSION_ID_LENGTH, DEFAULT_SESSION_ID_LENGTH);
    length = Integer.parseInt(sessionIdLength);
  }

  int getLengthInCharacters() {
    return Base64MaskingHelper.getLengthInCharacters(length);
  }
}
