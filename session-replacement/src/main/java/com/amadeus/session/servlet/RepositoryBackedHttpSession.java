package com.amadeus.session.servlet;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;

/**
 * This class implements {@link HttpSession} backed by
 * {@link RepositoryBackedSession}.
 * <p>
 * When there are concurrent uses of the HttpSession in multiple requests,
 * {@link RepositoryBackedHttpSessionWrapper} is used as facade to primary
 * instance.
 */
@SuppressWarnings({ "unchecked", "deprecation" })
class RepositoryBackedHttpSession extends RepositoryBackedSession implements HttpSession {
  private final ServletContext servletContext;

  /**
   * Standard constructor for for HTTP sessions.
   *
   * @param servletContext
   *          the current servlet context
   * @param sessionData
   *          the session data describing the session
   * @param sessionManager
   *          the session manager to use
   * @param factory
   *          the factory to notify when session is committed
   */
  RepositoryBackedHttpSession(ServletContext servletContext, SessionData sessionData, SessionManager sessionManager,
      HttpSessionFactory factory) {
    super(sessionData, sessionManager, factory);
    this.servletContext = servletContext;
  }

  /**
   * Copy constructor.
   *
   * @param wrapped
   */
  public RepositoryBackedHttpSession(RepositoryBackedHttpSession wrapped) {
    super(wrapped);
    this.servletContext = wrapped.servletContext;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  @Override
  public HttpSessionContext getSessionContext() {
    throw new UnsupportedOperationException("getSessionContext() is deprecated");
  }

  @Override
  public void putValue(String key, Object value) {
    setAttribute(key, value);
  }

  @Override
  public void removeValue(String key) {
    removeAttribute(key);
  }

  @Override
  public Object getValue(String key) {
    return getAttribute(key);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public String[] getValueNames() {
    // This method is deprecated, and the implementation is not an efficient one
    ArrayList<String> valueNames = new ArrayList<>();
    for (Enumeration names = getAttributeNames(); names.hasMoreElements();) {
      valueNames.add((String)names.nextElement());
    }
    return valueNames.toArray(new String[] {});
  }
}
