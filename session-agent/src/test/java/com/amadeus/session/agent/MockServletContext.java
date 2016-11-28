package com.amadeus.session.agent;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

public class MockServletContext implements ServletContext {

  @Override
  public Dynamic addFilter(String arg0, String arg1) {
    // Mock method
    return null;
  }

  @Override
  public Dynamic addFilter(String arg0, Filter arg1) {
    // Mock method
    return null;
  }

  @Override
  public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
    // Mock method
    return null;
  }

  @Override
  public void addListener(String arg0) {
    // Mock method

  }

  @Override
  public <T extends EventListener> void addListener(T arg0) {
    // Mock method

  }

  @Override
  public void addListener(Class<? extends EventListener> arg0) {
    // Mock method

  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
    // Mock method
    return null;
  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
    // Mock method
    return null;
  }

  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
    // Mock method
    return null;
  }

  @Override
  public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
    // Mock method
    return null;
  }

  @Override
  public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
    // Mock method
    return null;
  }

  @Override
  public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
    // Mock method
    return null;
  }

  @Override
  public void declareRoles(String... arg0) {
    // Mock method

  }

  @Override
  public Object getAttribute(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    // Mock method
    return null;
  }

  @Override
  public ClassLoader getClassLoader() {
    // Mock method
    return null;
  }

  @Override
  public ServletContext getContext(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public String getContextPath() {
    // Mock method
    return null;
  }

  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    // Mock method
    return null;
  }

  @Override
  public int getEffectiveMajorVersion() {
    // Mock method
    return 0;
  }

  @Override
  public int getEffectiveMinorVersion() {
    // Mock method
    return 0;
  }

  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    // Mock method
    return null;
  }

  @Override
  public FilterRegistration getFilterRegistration(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    // Mock method
    return null;
  }

  @Override
  public String getInitParameter(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    // Mock method
    return null;
  }

  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    // Mock method
    return null;
  }

  @Override
  public int getMajorVersion() {
    // Mock method
    return 0;
  }

  @Override
  public String getMimeType(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public int getMinorVersion() {
    // Mock method
    return 0;
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public String getRealPath(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public URL getResource(String arg0) throws MalformedURLException {
    // Mock method
    return null;
  }

  @Override
  public InputStream getResourceAsStream(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public Set<String> getResourcePaths(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public String getServerInfo() {
    // Mock method
    return null;
  }

  @Override
  public Servlet getServlet(String arg0) throws ServletException {
    // Mock method
    return null;
  }

  @Override
  public String getServletContextName() {
    // Mock method
    return null;
  }

  @Override
  public Enumeration<String> getServletNames() {
    // Mock method
    return null;
  }

  @Override
  public ServletRegistration getServletRegistration(String arg0) {
    // Mock method
    return null;
  }

  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    // Mock method
    return null;
  }

  @Override
  public Enumeration<Servlet> getServlets() {
    // Mock method
    return null;
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    // Mock method
    return null;
  }

  @Override
  public String getVirtualServerName() {
    // Mock method
    return null;
  }

  @Override
  public void log(String arg0) {
    // Mock method

  }

  @Override
  public void log(Exception arg0, String arg1) {
    // Mock method

  }

  @Override
  public void log(String arg0, Throwable arg1) {
    // Mock method

  }

  @Override
  public void removeAttribute(String arg0) {
    // Mock method

  }

  @Override
  public void setAttribute(String arg0, Object arg1) {
    // Mock method

  }

  @Override
  public boolean setInitParameter(String arg0, String arg1) {
    // Mock method
    return false;
  }

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
    // Mock method

  }

}
