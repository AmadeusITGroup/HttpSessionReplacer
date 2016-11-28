package com.amadeus.session;

/**
 * Implementations of this interface produce {@link RepositoryBackedSession}
 * instances based on passed {@link SessionData}.
 */
public interface SessionFactory {
  /**
   * Builds instance of session based on passed {@link SessionData}.
   *
   * @param sessionData
   *          the descriptor of the session to build
   * @return a new instance of {@link RepositoryBackedSession}
   */
  RepositoryBackedSession build(SessionData sessionData);

  /**
   * Callback to associated {@link SessionManager} with this instance of factory.
   *
   * @param sessionManager
   *          {@link SessionManager} to use with this factory.
   */
  void setSessionManager(SessionManager sessionManager);

  /**
   * Called by session manager to notify session factory that session id has
   * changed. It is up to session factory to implement any needed logic. E.g. if
   * it uses cache it can rename it here.
   *
   * @param sessionData
   *          the descriptor of the session whose id changes
   */
  void sessionIdChange(SessionData sessionData);

  /**
   * Called by each session when it is committed. Used when local cache is
   * active. This insures that when last concurrent call has been terminated,
   * session is evicted from cache.
   *
   * @param session
   *          the session being committed
   */
  void committed(RepositoryBackedSession session);
}
