package com.amadeus.session.servlet2x;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test servlet for 2.x
 */
public class InvalidateSession2x extends BaseServlet {

  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    trace(request);
    PrintWriter w = response.getWriter();
    if (request.getSession(false) != null) {
      String id = request.getSession(false).getId();
      request.getSession(false).invalidate();
      w.println("Invalidated");
      w.println(request.getSession().getId().equalsIgnoreCase(id) ? "sessionIdShouldNotBeTheSame"
        : "newSessionIdAfterInvalidate");
    } else {
      w.println("No session");
    }
    response.flushBuffer();
  }
}
