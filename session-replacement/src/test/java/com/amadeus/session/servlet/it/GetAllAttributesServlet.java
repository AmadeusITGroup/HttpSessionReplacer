package com.amadeus.session.servlet.it;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used for integration tests
 */
@WebServlet("/GetAllAttributes")
public class GetAllAttributesServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetAllAttributesServlet() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log("Request: " + request.getRequestURI() + " session: " + request.getRequestedSessionId() + " testCase="
        + request.getParameter("testCase"));
    PrintWriter w = response.getWriter();
    ArrayList<String> list = Collections.list(request.getSession().getAttributeNames());
    Collections.sort(list);
    w.println("Attributes in session: " + list);
    w.println("Encoded url: " + response.encodeURL("/"));
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
