package com.amadeus.session.servlet2x;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

abstract class BaseServlet extends HttpServlet {
  private static final long serialVersionUID = 1;

  protected void trace(HttpServletRequest request) {
    log("Request: " + request.getRequestURI() + " session: " + request.getRequestedSessionId() + " testCase="
        + request.getParameter("testCase"));

  }
}
