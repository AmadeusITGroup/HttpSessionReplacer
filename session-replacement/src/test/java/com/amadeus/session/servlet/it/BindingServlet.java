package com.amadeus.session.servlet.it;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * Used for integration tests. Changes session id.
 */
@WebServlet("/BindingServlet")
public class BindingServlet  extends BaseTestServlet {
  private static final long serialVersionUID = 1L;

  private static BindingListener myBinding = new BindingListener();

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log(request);
    PrintWriter w = response.getWriter();
    w.println("Binding calls: " + myBinding);
    request.getSession(true).setAttribute("Binding", myBinding);
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    System.out.println(request.getSession().getId());
    w.println("New value of attribute: " + request.getSession().getAttribute("Binding"));
    w.println("Encoded url: " + response.encodeURL("/"));
    w.append("Served at: ").append(request.getContextPath()).append(" ");
  }

  static class BindingListener implements HttpSessionBindingListener {
      static int totalBounds;
      static int totalUnbounds;
      public void valueBound(HttpSessionBindingEvent event) {
        totalBounds++;
      }

      public void valueUnbound(HttpSessionBindingEvent event) {
        totalUnbounds++;
      }

      @Override
      public String toString() {
          return "" + totalBounds + ":" + totalUnbounds;
      }
  } 
}
