package com.amadeus.session.servlet;

import static com.amadeus.session.servlet.SessionHelpersFacade.commitRequest;
import static com.amadeus.session.servlet.SessionHelpersFacade.initSessionManagement;
import static com.amadeus.session.servlet.SessionHelpersFacade.prepareRequest;
import static com.amadeus.session.servlet.SessionHelpersFacade.prepareResponse;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;

/**
 * Filter that wraps the httpRequest to enable Http Session caching.
 *
 * Note that it won't wrap the request twice even if the filter is called two
 * times.
 *
 * @see ServletRequestWrapper
 */
public class SessionFilter implements Filter {
  ServletContext servletContext;

  /**
   * Initializes session management based on repository for current servlet
   * context.
   *
   * @param config
   *          The filter configuration.
   */
  @Override
  public void init(FilterConfig config) {
    initForSession(config);
  }

  /**
   * Initializes session management based on repository for current servlet
   * context. This method is internal method for session management.
   *
   * @param config
   *          The filter configuration.
   */
  public void initForSession(FilterConfig config) {
    if (servletContext == null) {
      servletContext = config.getServletContext();
      initSessionManagement(servletContext);
    }
  }

  /**
   * Implements wrapping of HTTP request and enables handling of sessions based
   * on repository.
   *
   * @param originalRequest
   *          The request to wrap
   * @param originalResponse
   *          The response.
   * @param chain
   *          The filter chain.
   * @throws IOException
   *           If such exception occurs in chained filters.
   * @throws ServletException
   *           If such exception occurs in chained filters.
   */
  @Override
  public void doFilter(ServletRequest originalRequest, ServletResponse originalResponse, FilterChain chain)
      throws IOException, ServletException {
    ServletRequest request = prepareRequest(originalRequest, originalResponse, servletContext);
    ServletResponse response = prepareResponse(request, originalResponse, servletContext);

    try {
      // Call next filter in chain
      chain.doFilter(request, response);
    } finally {
      // Commit the session. Implementation expects that request has been
      // wrapped and that originalRequest is not an OffloadSessionHttpServletRequest
      commitRequest(request, originalRequest, servletContext);
    }
  }

  /**
   * No specific processing is done when this filter is being taken out of
   * service.
   */
  @Override
  public void destroy() {
    // Do nothing
  }
}