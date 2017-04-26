package com.amadeus.session.servlet.it;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used for integration tests
 */
@WebServlet("/OtherServlet")
public class OtherServlet extends BaseTestServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log(request);
    PrintWriter w = response.getWriter();
    try {
      w.println("Previous value of attribute: " + request.getSession().getAttribute("A"));
      request.getSession(true).setAttribute("A", "C");
      w.println("New value of attribute: " + request.getSession().getAttribute("A"));
      w.append("Served at: ").append(request.getContextPath()).append(" ");
      response.flushBuffer();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
