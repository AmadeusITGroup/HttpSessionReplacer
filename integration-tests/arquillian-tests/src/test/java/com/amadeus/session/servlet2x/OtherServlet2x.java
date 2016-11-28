package com.amadeus.session.servlet2x;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used for integration tests
 */
public class OtherServlet2x extends BaseServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    trace(request);
    PrintWriter w = response.getWriter();
    w.println("Previous value of attribute: " + request.getSession().getAttribute("A"));
    request.getSession(true).setAttribute("A", "C");
    w.println("New value of attribute: " + request.getSession().getAttribute("A"));
    w.append("Served at: ").append(request.getContextPath()).append(" ");
    response.flushBuffer();
  }
}
