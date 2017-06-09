package com.amadeus.session.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionManager;

/**
 * Wrapper for {@link HttpServletRequest} that implements storing of sessions in
 * repository. This class implements following commit logic: propagate session
 * to response, store session in repository, perform cleanups as request
 * processing has finished.
 */
class HttpRequestWrapper extends HttpServletRequestWrapper implements RequestWithSession {
  private static final Logger logger = LoggerFactory.getLogger(HttpRequestWrapper.class);
  RepositoryBackedHttpSession session;
  boolean committed;
  private HttpResponseWrapper response;
  private final HttpRequestWrapper embeddedRequest;
  private final SessionManager manager;
  private final ServletContext servletContext;
  private boolean propagateOnCreate;
  private boolean propagated;
  private boolean idRetrieved;
  private String retrievedId;
  private boolean repositoryChecked;

  /**
   * Creates request wrapper from original requests.
   *
   * @param req
   *          original servlet request
   * @param servletContext
   */
  public HttpRequestWrapper(HttpServletRequest req, ServletContext servletContext) {
    super(req);
    this.servletContext = servletContext;
    manager = (SessionManager)servletContext.getAttribute(Attributes.SESSION_MANAGER);
    ServletRequest originalRequest = req;
    while (originalRequest instanceof ServletRequestWrapper) {
      if (originalRequest instanceof HttpRequestWrapper) {
        break;
      }
      originalRequest = ((ServletRequestWrapper)originalRequest).getRequest();
    }

    embeddedRequest = (originalRequest instanceof HttpRequestWrapper) ? (HttpRequestWrapper)originalRequest : null;
  }

  @Override
  public RepositoryBackedHttpSession getSession() {
    return getSession(true);
  }

  @Override
  public RepositoryBackedHttpSession getSession(boolean create) {
    if (embeddedRequest != null) {
      // If there is embedded HttpRequestWrapper, create session there if needed
      embeddedRequest.getSession(create);
    }
    return getRepositoryBackedSession(create);
  }

  /**
   * Returns {@link SessionManager} used in this request.
   *
   * @return session manager used for this request
   */
  public SessionManager getManager() {
    return manager;
  }

  /**
   * Returns responses associated to this request.
   *
   * @return responses associated to this request
   */
  public HttpResponseWrapper getResponse() {
    return response;
  }

  /**
   * Sets response associated to this request
   * @param response
   */
  public void setResponse(HttpResponseWrapper response) {
    this.response = response;
  }

  /**
   * Callback to propagate session to response.
   * @return returns <code>true</code> if session was stored
   */
  boolean propagateSession() {
    if (getRepositoryBackedSession(false) == null) {
      setPropagateOnCreate(true);
      return false;
    }
    return doPropagateAndStoreIfFirstWrapper();
  }

  private boolean doPropagateAndStoreIfFirstWrapper() {
    if (embeddedRequest == null && (!propagated || isDirty())) {
      manager.propagateSession(this, response);
      storeSession();
      propagated = true;
      return true;
    }
    return false;
  }

  private boolean isDirty() {
    return session != null && session.isDirty();
  }

  void retrieveSessionIfNeeded(boolean create) {
    if (embeddedRequest != null) {
      embeddedRequest.retrieveSessionIfNeeded(create);
    }
    if (session == null || !session.isValid()) {
      session = (RepositoryBackedHttpSession)manager.getSession(this, create, getEmbededdSessionId());
    }
  }

  private String getEmbededdSessionId() {
    if (embeddedRequest != null && embeddedRequest.session != null && embeddedRequest.session.isValid()) {
      return embeddedRequest.session.getId();
    }
    return null;
  }

  /**
   * Callback to commit session.
   */
  public void commit() {
    doCommit();
  }

  /**
   * Implementation of commit.
   */
  void doCommit() {
    if (committed) {
      return;
    }
    // we propagate the session, and that will trigger storage
    if (!propagateSession()) {
      storeSession();
    }
    committed = true;
    manager.requestFinished();
  }

  /**
   * Stores session if it was created
   */
  void storeSession() {
    retrieveSessionIfNeeded(false);

    if (session != null) {
      try {
        session.commit();
      } catch (Exception e) { // NOSONAR - some error occured, log it
        logger.warn("cannot store session: {}", session, e);
      }
    } else {
      logger.debug("session was null, nothing to commit");
    }
  }

  @Override
  public RepositoryBackedHttpSession getRepositoryBackedSession(boolean create) {
    if (committed) {
      throw new IllegalStateException("Session was already committed.");
    }
    boolean wasNotCreated = session == null;
    retrieveSessionIfNeeded(create);
    // Only propagate session if this is the "outer" session, i.e. the one that
    // created the session closest to the client
    if (wasNotCreated && propagateOnCreate) {
      doPropagateAndStoreIfFirstWrapper();
    }
    return session;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  /**
   * Get wrapped request if it is an {@link HttpRequestWrapper}.
   * Returns <code>null</code> if wrapped wrapped request was
   * not HttpRequestWrapper.
   * @return
   */
  public HttpRequestWrapper getEmbeddedRequest() {
    return embeddedRequest;
  }

  /**
   * Returns <code>true</code> if session should be propagated on create.
   * @return
   */
  public boolean isPropagateOnCreate() {
    return propagateOnCreate;
  }

  /**
   * Controls if session should be propagated on create. Session should be
   * propagated on request if {@link #propagateSession()} method was called
   * (i.e. if there an event occurred that requires session propagation).
   * @param propagate
   */
  public void setPropagateOnCreate(boolean propagate) {
    this.propagateOnCreate = propagate;
    if (embeddedRequest != null) {
      embeddedRequest.setPropagateOnCreate(true);
    }
  }

  @Override
  public boolean isIdRetrieved() {
    return idRetrieved;
  }

  @Override
  public void setRequestedSessionId(String id) {
    idRetrieved = true;
    retrievedId = id;
  }

  @Override
  public String getRequestedSessionId() {
    if (!idRetrieved) {
      getSession(false);
    }
    return retrievedId;
  }

  @Override
  public boolean isRepositoryChecked() {
    return repositoryChecked;
  }

  @Override
  public void repositoryChecked() {
    repositoryChecked = true;
  }

  /**
   * Called to encode URL. This is used when URL session propagation is used.
   * @param url
   * @return
   */
  public String encodeURL(String url) {
    return manager.encodeUrl(this, url);
  }

  @Override
  public String changeSessionId() {
    retrieveSessionIfNeeded(false);
    if (session == null) {
      throw new IllegalStateException("There is no session associated with the request.");
    }
    manager.switchSessionId(session);
    return session.getId();
  }
}
