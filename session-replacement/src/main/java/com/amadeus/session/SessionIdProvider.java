package com.amadeus.session;

/**
 * Implementations of this interface are responsible for generating new session
 * ids and parsing/cleaning the received ones.
 */
public interface SessionIdProvider {

  /**
   * Generates new session id.
   *
   * @return new session id
   */
  String newId();

  /**
   * Returns cleaned session id or <code>null</code> if value has invalid id
   * format.
   *
   * @param value
   *          id read from input
   * @return cleaned session id or <code>null</code> if value has invalid id
   *         format
   */
  String readId(String value);

  /**
   * Configures session id provider.
   *
   * @param configuration
   *          the session configuration
   */
  void configure(SessionConfiguration configuration);
}
