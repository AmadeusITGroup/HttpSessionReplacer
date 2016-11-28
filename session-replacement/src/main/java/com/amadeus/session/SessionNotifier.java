package com.amadeus.session;

/**
 * Implementations of this interface can react on changes of the session
 * attributes or session state. For example, this enables notifying different
 * session and attribute listeners in case of JEE servlet sessions.
 */
public interface SessionNotifier {
  /**
   * Invoked after a session has been created.
   *
   * @param session
   *          the session that is being created
   */
  void sessionCreated(RepositoryBackedSession session);

  /**
   * Invoked when a session will be destroyed.
   *
   * @param session
   *          the session that is being destroyed
   * @param shutdown
   *          set to <code>true</code> when called during application shutdown
   */
  void sessionDestroyed(RepositoryBackedSession session, boolean shutdown);

  /**
   * Invoked when an attribute is added to session.
   *
   * @param session
   *          where the operation takes place
   * @param key
   *          name of the attribute
   * @param value
   *          value of the attribute
   */
  void attributeAdded(RepositoryBackedSession session, String key, Object value);

  /**
   * Invoked when an attribute value is replaced with a new one in the session.
   *
   * @param session
   *          where the operation takes place
   * @param key
   *          name of the attribute
   * @param replacedValue
   *          old value of the attribute
   */
  void attributeReplaced(RepositoryBackedSession session, String key, Object replacedValue);

  /**
   * Invoked when an attribute is removed from the session.
   *
   * @param session
   *          where the operation takes place
   * @param key
   *          name of the attribute
   * @param removedValue
   *          old value of the attribute
   */
  void attributeRemoved(RepositoryBackedSession session, String key, Object removedValue);

  /**
   * Invoked when an attribute is being stored in repository.
   *
   * @param session
   *          where the operation takes place
   * @param key
   *          name of the attribute
   * @param value
   *          value of the attribute
   */
  void attributeBeingStored(RepositoryBackedSession session, String key, Object value);

  /**
   * Invoked when an attribute has been retrieved from repository.
   *
   * @param session
   *          where the operation takes place
   * @param key
   *          name of the attribute
   * @param value
   *          value of the attribute
   */
  void attributeHasBeenRestored(RepositoryBackedSession session, String key, Object value);

  /**
   * Invoked when the session id changes.
   *
   * @param session
   *          where the operation takes place
   * @param oldId
   *          old session id
   */
  void sessionIdChanged(RepositoryBackedSession session, String oldId);
}