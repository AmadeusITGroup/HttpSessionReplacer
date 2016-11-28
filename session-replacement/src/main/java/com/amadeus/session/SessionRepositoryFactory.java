package com.amadeus.session;

/**
 * Implementations of this class create instances of {@link SessionRepository}
 * for the given configuration.
 */
public interface SessionRepositoryFactory {
  /**
   * Returns new repository instance based on configuration.
   *
   * @param sessionConfiguration
   *          configuration for the session management
   * @return new repository instance
   */
  SessionRepository repository(SessionConfiguration sessionConfiguration);

  /**
   * Returns <code>true</code> if repository enforces distribution of sessions
   * (i.e. stores sessions remotely).
   *
   * @return <code>true</code> if repository enforces distribution of sessions
   */
  boolean isDistributed();
}
