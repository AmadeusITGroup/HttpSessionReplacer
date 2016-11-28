package com.amadeus.session.servlet;

import javax.servlet.ServletContext;

import com.amadeus.session.DefaultSessionFactory;
import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.SessionData;

/**
 * This class creates session that is supported on HTTP protocol. It provides
 * support for caching and sharing session instances between concurrent
 * requests.
 */
class HttpSessionFactory extends DefaultSessionFactory {
  private final ServletContext servletContext;

  /**
   * Standard constructor.
   *
   * @param servletContext
   *          the servlet context for which sessions are created
   */
  HttpSessionFactory(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * Wraps old session object into new one
   *
   * @param oldSession
   *          session to wrap
   * @return wrapped session
   */
  @Override
  protected RepositoryBackedSession wrapSession(RepositoryBackedSession oldSession) {
    return new RepositoryBackedHttpSession((RepositoryBackedHttpSession)oldSession);
  }

  /**
   * Creates new memory representation of session based on session data.
   *
   * @param sessionData
   *          the session descriptor
   * @return local facade to session
   */
  @Override
  protected RepositoryBackedHttpSession newSessionObject(SessionData sessionData) {
    return new RepositoryBackedHttpSession(servletContext, sessionData, sessionManager, this);
  }
}
