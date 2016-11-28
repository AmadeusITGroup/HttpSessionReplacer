package com.amadeus.session.servlet.it;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

abstract class BaseTestServlet extends HttpServlet {
  private static final long serialVersionUID = 1;

  protected void log(HttpServletRequest request) {
    log("Request: " + request.getRequestURI() + " session: " + request.getRequestedSessionId() + " testCase="
        + request.getParameter("testCase"));

  }
}
