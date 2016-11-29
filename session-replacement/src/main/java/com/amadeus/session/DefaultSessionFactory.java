package com.amadeus.session;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link SessionFactory} that allows creation of generic
 * sessions. It is meant for use with sessions not linked to the servlet engines
 * and <code>HttpServletRequest</code>. For servlet engines use
 * <code>com.amadeus.session.servlet.HttpSessionFactory</code>.
 */
public class DefaultSessionFactory implements SessionFactory {
  private static final Logger logger = LoggerFactory.getLogger(DefaultSessionFactory.class);
  protected SessionManager sessionManager;
  private ConcurrentHashMap<String, RepositoryBackedSession> cachedSessions;
  private boolean useCached;

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    useCached = sessionManager.getConfiguration().isAllowedCachedSessionReuse();
    if (useCached) {
      cachedSessions = new ConcurrentHashMap<>();
    }
  }

  @Override
  public RepositoryBackedSession build(SessionData sessionData) {
    if (useCached) {
      // If we use cached sessions, check if the session is in memory
      RepositoryBackedSession session = cachedSessions.get(sessionData.getId());
      if (session == null) {
        // if not, it's a new session
        session = newSessionObject(sessionData);
        RepositoryBackedSession oldSession = cachedSessions.putIfAbsent(sessionData.getId(), session);
        if (oldSession != null) {
          // If we had race condition, and someone added new session to cache,
          // use what is in cache
          session = wrapSession(oldSession);
        }
      } else {
        // Wrap cached session to avoid leaking of commit from one to another.
        session = wrapSession(session);
      }
      return session;
    }
    return newSessionObject(sessionData);
  }

  /**
   * Wraps old session object into a new one. This method can be overridden in a
   * subclass to provide wrapping of sessions in different implementations (e.g.
   * HttpSession).
   *
   * @param oldSession
   *          session to wrap
   * @return wrapped session
   */
  protected RepositoryBackedSession wrapSession(RepositoryBackedSession oldSession) {
    return new RepositoryBackedSession(oldSession);
  }

  /**
   * Creates new memory representation of session based on session data.
   *
   * @param sessionData
   *          the session descriptor
   * @return local facade to session
   */
  protected RepositoryBackedSession newSessionObject(SessionData sessionData) {
    return new RepositoryBackedSession(sessionData, sessionManager, this);
  }

  /**
   * Called by each session when it is committed. Used when local cache is
   * active. This insures that when last concurrent call has been terminated,
   * session is evicted from cache.
   *
   * @param session
   *          the session being committed
   */
  @Override
  public void committed(RepositoryBackedSession session) {
    if (useCached) {
      synchronized (session) {
        if (session.getConcurrentUses() <= 0) {
          cachedSessions.remove(session.getId());
        }
      }
    }
  }

  @Override
  public void sessionIdChange(SessionData sessionData) {
    if (useCached) {
      // If session is cached we need to change session id in local cache.
      RepositoryBackedSession session = cachedSessions.remove(sessionData.getOriginalId());
      if (session != null) {
        SessionData cachedSessionData = session.getSessionData();
        // We need to update cachedSessionData only if it is not same as
        // sessionData
        if (cachedSessionData != sessionData) { // NOSONAR
          synchronized (cachedSessionData) {
            if (!cachedSessionData.isIdChanged()) {
              cachedSessionData.setNewSessionId(sessionData.getId());
            } else {
              logger.warn("Session id was already switched for session in cache: {}, new session should be: {}",
                  cachedSessionData, sessionData);
              return;
            }
          }
        }
        cachedSessions.put(sessionData.getId(), session);
      } else {
        logger.warn("Session id during session id change was not present in the cache: {}.", sessionData);
      }
    }
  }
}
