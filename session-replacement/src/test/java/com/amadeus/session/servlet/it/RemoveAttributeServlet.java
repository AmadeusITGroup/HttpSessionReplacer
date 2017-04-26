package com.amadeus.session.servlet.it;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used for integration tests
 */
@WebServlet("/RemoveAttributeServlet")
public class RemoveAttributeServlet extends BaseTestServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log(request);
    ServletOutputStream w = response.getOutputStream();
    log("Got output stream " + w);
    w.println("Previous value of attribute: " + request.getSession().getAttribute("A"));
    request.getSession(true).removeAttribute("A");
    w.println("New value of attribute: " + request.getSession().getAttribute("A"));
    w.println("Encoded url: " + response.encodeURL("/"));
    w.print("Served at: ");
    w.print(request.getContextPath());
    w.print(" ");
    w.flush();
//    PrintWriter w = response.getWriter();
//    log("Got writer w");
//    w.println("Previous value of attribute: " + request.getSession().getAttribute("A"));
//    request.getSession(true).removeAttribute("A");
//    w.println("New value of attribute: " + request.getSession().getAttribute("A"));
//    w.append("Served at: ").append(request.getContextPath()).append(" ");
//    w.flush();
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
