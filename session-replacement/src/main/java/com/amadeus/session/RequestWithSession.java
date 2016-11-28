package com.amadeus.session;

/**
 * Implementations of request that can be managed by SessionManager must
 * implement this interface.
 * <p>
 * Its methods allow retrieval/creation of session and getting and setting of
 * attributes associated to request.
 */
public interface RequestWithSession {

  /**
   * Returns the {@link RepositoryBackedSession} associated with this request.
   * If there is no associated session and <code>create</code> is
   * <code>true</code>, returns a new session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid
   * {@link RepositoryBackedSession}, this method returns <code>null</code>.
   *
   * <p>
   * As with HttpSession from servlet specification, to make sure the session is
   * properly maintained, you must call this method before the response is
   * committed. Depending on implementation, if caller asked to create a new
   * session when the response is committed, an IllegalStateException can be
   * thrown.
   *
   * @param create
   *          <code>true</code> to create a new session for this request if
   *          necessary; <code>false</code> to return <code>null</code> if
   *          there's no current session
   *
   * @return the {@link RepositoryBackedSession} associated with this request or
   *         <code>null</code> if <code>create</code> is <code>false</code> and
   *         the request has no valid session
   *
   */
  RepositoryBackedSession getRepositoryBackedSession(boolean create);

  /**
   * Returns value of the attribute.
   *
   * @param key
   *          attribute key
   * @return attribute value
   */
  Object getAttribute(String key);

  /**
   * Sets the value of attribute
   *
   * @param key
   *          attribute key
   * @param value
   *          new value for the attribute
   */
  void setAttribute(String key, Object value);

  /**
   * Returns true if id has been retrieved
   *
   * @return true if id has been retrieved
   */
  boolean isIdRetrieved();

  /**
   * Id that has been retrieved from request.
   *
   * @return Id that has been retrieved from request.
   */
  String getRequestedSessionId();

  /**
   * Sets id that has been retrieved from request.
   *
   * @param id
   *          the id that has been retrieved from request.
   */
  void setRequestedSessionId(String id);

  /**
   * Returns <code>true</code> if repository was checked to see if it contains
   * the session corresponding to request.
   *
   * @return <code>true</code> if repository was checked to see if it contains
   *         the session corresponding to request.
   */
  boolean isRepositoryChecked();

  /**
   * Marks that repository was checked to see if it contains the session
   * corresponding to request.
   */
  void repositoryChecked();
}
