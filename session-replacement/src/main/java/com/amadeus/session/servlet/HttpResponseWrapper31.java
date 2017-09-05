package com.amadeus.session.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import com.amadeus.session.ResponseWithSessionId;

/**
 * Allows ensuring that the session is propagated if the response is committed.
 * Each of the methods that can commit response will invoke
 * {@link #propagateSession()} method to insure that session id has been
 * propagated.
 */
class HttpResponseWrapper31 extends HttpResponseWrapper implements ResponseWithSessionId {

  /**
   * Default constructor
   *
   * @param request
   *          the wrapped request
   * @param response
   *          the response to be wrapped
   */
  HttpResponseWrapper31(HttpRequestWrapper request, HttpServletResponse response) {
    super(request, response);
  }

  @Override
  protected SaveSessionServletOutputStream wrapOutputStream(ServletOutputStream outputStream) {
    return new SaveSessionServlet31OutputStream(outputStream);
  }


  @Override
  public void setContentLengthLong(long len) {
    contentLength = len;
    super.setContentLengthLong(len);
  }

  /**
   * Ensures that session is indeed committed when calling methods that commit the response.
   * We delegate all methods to  the original {@link javax.servlet.ServletOutputStream} to
   * ensure that the behavior is as close as possible to the original one.
   *
   * Based on Spring Session code.
   */
  class SaveSessionServlet31OutputStream extends SaveSessionServletOutputStream {
    SaveSessionServlet31OutputStream(ServletOutputStream delegate) {
      super(delegate);
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      delegate.setWriteListener(writeListener);
    }
  }
}