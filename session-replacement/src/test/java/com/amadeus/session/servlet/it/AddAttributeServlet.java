package com.amadeus.session.servlet.it;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SampleServlet
 */
@WebServlet("/AddAttributeServlet")
public class AddAttributeServlet extends BaseTestServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log(request);
    PrintWriter w = response.getWriter();
    w.println("Previous value of attribute B: " + request.getSession(true).getAttribute("B"));
    request.getSession(true).setAttribute("B", "D");
    w.println("New value of attribute B: " + request.getSession().getAttribute("B"));
    w.append("Served at: ").append(request.getContextPath()).append(" ");
    response.flushBuffer();
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
