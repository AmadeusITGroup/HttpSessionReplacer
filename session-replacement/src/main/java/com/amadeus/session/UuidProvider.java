package com.amadeus.session;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates session id based on UUID.
 */
public class UuidProvider implements SessionIdProvider {
  private static final Logger logger = LoggerFactory.getLogger(UuidProvider.class);

  @Override
  public String newId() {
    return UUID.randomUUID().toString();
  }

  @Override
  public String readId(String value) {
    try {
      return UUID.fromString(value).toString();
    } catch (Exception e) { // NOSONAR If exception it is not valid UUID
      logger.info("Cookie value vas not a valid UUID: {}", value);
      return null;
    }
  }

  @Override
  public void configure(SessionConfiguration configuration) {
    // No specific configuration exists
  }
}
