package com.amadeus.session.sample;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Annotated filter example
 */
public final class FinalFilter implements Filter {
  public boolean init;
  public boolean destroy;
  @Override
  public void destroy() {
    destroy = true;
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
    init = true;
  }

  @Override
  public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
      throws IOException, ServletException {
    throw new UnsupportedOperationException("doFilter()");
  }

}
