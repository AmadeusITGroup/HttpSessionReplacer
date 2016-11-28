package com.amadeus.session.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Helper class used to find if container supports Servlet 3.x and Servlet 3.1 Api.
 */
final class ServletLevel {
  static final boolean isServlet3 = isServlet3();   // NOSONAR Prefer this naming convention here
  static final boolean isServlet31 = isServlet31(); // NOSONAR Prefer this naming convention here

  /*
   * Hide constructor
   */
  private ServletLevel() {
  }

  /**
   * Returns true if the Servlet 3 APIs are detected.
   *
   * @return true if the Servlet 3 API is detected
   */
  private static boolean isServlet3() {
    try {
      ServletRequest.class.getMethod("startAsync");
      return true;
    } catch (NoSuchMethodException e) { // NOSONAR no method, so no servlet 3
      return false;
    }
  }

  /**
   * Returns true if the Servlet 3.1 APIs are detected.
   *
   * @return true if the Servlet 3.1 API is detected
   */
  private static boolean isServlet31() {
    try {
      HttpServletRequest.class.getMethod("changeSessionId");
      return true;
    } catch (NoSuchMethodException e) { // NOSONAR no method, so no servlet 3.1
      return false;
    }
  }
}
