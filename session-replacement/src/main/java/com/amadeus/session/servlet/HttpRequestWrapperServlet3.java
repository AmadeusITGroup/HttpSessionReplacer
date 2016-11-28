package com.amadeus.session.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.amadeus.session.RequestWithSession;

/**
 * Wrapper for {@link HttpServletRequest} that implements storing of sessions in
 * repository. This class implements following commit logic: propagate session
 * to response, store session in repository, perform cleanups as request
 * processing has finished.
 */
class HttpRequestWrapperServlet3 extends HttpRequestWrapper implements RequestWithSession {
  private boolean async;


  /**
   * Creates request wrapper from original requests.
   *
   * @param req
   *          original servlet request
   * @param servletContext
   */
  public HttpRequestWrapperServlet3(HttpServletRequest req, ServletContext servletContext) {
    super(req, servletContext);
  }

  @Override
  public AsyncContext startAsync() {
    AsyncContext ac = super.startAsync();
    ac.addListener(new SessionCommitListener());
    async = true;
    return ac;
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
    AsyncContext ac = super.startAsync(servletRequest, servletResponse);
    ac.addListener(new SessionCommitListener());
    async = true;
    return ac;
  }

  @Override
  public void commit() {
    if (!async) {
      super.commit();
    }
  }

  /**
   * Callback for async requests that performs commit when async processing has
   * been completed.
   */
  class SessionCommitListener implements AsyncListener {
    SessionCommitListener() {
    }

    @Override
    public void onComplete(AsyncEvent event) {
      HttpRequestWrapperServlet3.this.doCommit();
    }

    @Override
    public void onError(AsyncEvent event) {
      // Do nothing
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
      // Do nothing
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      // Do nothing
    }
  }
}
