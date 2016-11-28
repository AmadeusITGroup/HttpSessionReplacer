package com.amadeus.session.sample;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class MockFilterConfig implements FilterConfig {
  ServletContext servletContext;
  String filterName;

  @Override
  public String getFilterName() {
    return filterName;
  }

  @Override
  public String getInitParameter(String arg0) {
    return null;
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return Collections.emptyEnumeration();
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

}
