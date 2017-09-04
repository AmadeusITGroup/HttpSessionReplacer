package com.amadeus.session.servlet;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionRepository;
import com.amadeus.session.SessionTracking;
import com.amadeus.session.repository.inmemory.InMemoryRepositoryFactory;

@SuppressWarnings("javadoc")
public class TestSessionHelpers {

  private ServletContext servletContext;
  private SessionHelpers sessionHelpers;
  private SessionManager sessionManager;
  private SessionConfiguration sessionConfiguration;

  @Before
  public void enableSession() {
    servletContext = mock(ServletContext.class);
    sessionHelpers = new SessionHelpers();
    when(servletContext.getAttribute(SessionHelpers.SESSION_HELPERS)).thenReturn(sessionHelpers);
    sessionManager = mock(SessionManager.class);
    sessionConfiguration = new SessionConfiguration();
    when(sessionManager.getConfiguration()).thenReturn(sessionConfiguration);
    when(servletContext.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(sessionManager);
  }

  @Test
  public void testGetTracking() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    SessionTracking tracking = SessionHelpers.getTracking(servletContext, sessionConfiguration);
    assertTrue("Expecting instance of CookieSessionTracking", tracking instanceof CookieSessionTracking);
    sessionConfiguration.setSessionTracking("URL");
    tracking = SessionHelpers.getTracking(servletContext, sessionConfiguration);
    assertTrue("Expecting instance of UrlSessionTracking", tracking instanceof UrlSessionTracking);
    sessionConfiguration.setSessionTracking(CookieSessionTracking.class.getName());
    tracking = SessionHelpers.getTracking(servletContext, sessionConfiguration);
    assertTrue("Expecting instance of CookieSessionTracking", tracking instanceof CookieSessionTracking);
    assertTrue(tracking instanceof CookieSessionTracking);
    verify(servletContext).getClassLoader();
  }

  @Test
  public void testGetRepositoryDefault() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    SessionRepository repository = SessionHelpers.repository(servletContext, sessionConfiguration);
    assertEquals("com.amadeus.session.repository.inmemory.InMemoryRepository", repository.getClass().getName());
  }

  @Test
  public void testTimestamp() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setSessionTracking("URL");
    sessionConfiguration.setTimestampSufix(true);
    SessionTracking tracking = SessionHelpers.getTracking(servletContext, sessionConfiguration);
    assertTrue(tracking.newId().indexOf("!") > 0);

    sessionConfiguration.setTimestampSufix(false);
    tracking = SessionHelpers.getTracking(servletContext, sessionConfiguration);
    assertTrue(tracking.newId().indexOf("!") < 0);
  }

  @Test
  public void testGetRepositoryByKeyExisting() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    Map<String, String> map = new HashMap<>();
    map.put("IN", InMemoryRepositoryFactory.class.getName());
    map.put("BAD", "com.not.existing.class.Bad");
    when(servletContext.getAttribute(Attributes.PROVIDERS)).thenReturn(map);
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setRepositoryFactory("IN");
    SessionRepository repository = SessionHelpers.repository(servletContext, sessionConfiguration);
    assertEquals("com.amadeus.session.repository.inmemory.InMemoryRepository", repository.getClass().getName());
  }

  @Test(expected=IllegalArgumentException.class)
  public void testGetRepositoryByKeyBadClass() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    Map<String, String> map = new HashMap<>();
    map.put("IN", InMemoryRepositoryFactory.class.getName());
    map.put("BAD", "com.not.existing.class.Bad");
    when(servletContext.getAttribute(Attributes.PROVIDERS)).thenReturn(map);
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setRepositoryFactory("BAD");
    SessionHelpers.repository(servletContext, sessionConfiguration);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testGetRepositoryByKeyBadId() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    Map<String, String> map = new HashMap<>();
    map.put("IN", InMemoryRepositoryFactory.class.getName());
    map.put("BAD", "com.not.existing.class.Bad");
    when(servletContext.getAttribute(Attributes.PROVIDERS)).thenReturn(map);
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setRepositoryFactory("BAD_ID");
    SessionHelpers.repository(servletContext, sessionConfiguration);
  }

  @Test
  public void testInitConf() {
    when(servletContext.getContextPath()).thenReturn("my-context");
    SessionConfiguration configuration = SessionHelpers.initConf(servletContext);
    assertNotNull(configuration);
    verify(servletContext).setAttribute(SessionHelpers.SESSION_CONFIGURATION, configuration);
    assertEquals("my-context", configuration.getNamespace());
  }

  @Test
  public void testInitConfFromServletContext() {
    SessionConfiguration sc = new SessionConfiguration();
    when(servletContext.getAttribute(SessionHelpers.SESSION_CONFIGURATION)).thenReturn(sc);
    SessionConfiguration configuration = SessionHelpers.initConf(servletContext);
    assertEquals(sc, configuration);
    verify(servletContext, never()).setAttribute(Mockito.eq(SessionHelpers.SESSION_CONFIGURATION), any());
  }

  @Test
  public void testPrepareResponse() {
    ServletResponse response = mock(ServletResponse.class);
    HttpResponseWrapper responseWrapped = mock(HttpResponseWrapper.class);
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    when(request.getServletContext()).thenReturn(servletContext);
    when(request.getResponse()).thenReturn(responseWrapped);
    ServletResponse result = SessionHelpersFacade.prepareResponse(request , response, null);
    assertSame(responseWrapped, result);
  }

  @Test
  public void testTrackingFromEnum() throws InstantiationException, IllegalAccessException {
    SessionTracking tracking = SessionHelpers.trackingFromEnum("URL");
    assertTrue("Expecting instance of UrlSessionTracking", tracking instanceof UrlSessionTracking);
    tracking = SessionHelpers.trackingFromEnum("DEFAULT");
    assertTrue("Expecting instance of CookieSessionTracking", tracking instanceof CookieSessionTracking);
    tracking = SessionHelpers.trackingFromEnum("COOKIE");
    assertTrue("Expecting instance of CookieSessionTracking", tracking instanceof CookieSessionTracking);
    tracking = SessionHelpers.trackingFromEnum("BAD");
    assertNull(tracking);
  }

  @Test
  public void testInitSessionManagement() {
    when(servletContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    SessionHelpersFacade.initSessionManagement(servletContext);
    ArgumentCaptor<SessionManager> sessionManager = ArgumentCaptor.forClass(SessionManager.class);
    verify(servletContext).setAttribute(eq(Attributes.SESSION_MANAGER), sessionManager.capture());
    assertNotNull(sessionManager.getValue());
    sessionManager.getValue().close();
  }

  @Test
  public void testCommitRequest() {
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    when(request.getServletContext()).thenReturn(servletContext);
    ServletRequest oldRequest = mock(ServletRequest.class);
    SessionHelpersFacade.commitRequest(request, oldRequest, null);
    verify(request).commit();
  }
  @Test
  public void testDontCommitNonReWrappedRequest() {
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    when(request.getServletContext()).thenReturn(servletContext);
    SessionHelpersFacade.commitRequest(request, request, null);
    verify(request, never()).commit();
  }

  @Test
  public void testCommitReWrappedRequest() {
    HttpRequestWrapper oldRequest = mock(HttpRequestWrapper.class);
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    when(request.getServletContext()).thenReturn(servletContext);
    SessionHelpersFacade.commitRequest(request, oldRequest, null);
    verify(request).commit();
    verify(oldRequest, never()).commit();
  }

  @Test
  public void testDontCommitNonWrappped() {
    ServletRequest request = mock(ServletRequest.class);
    when(request.getServletContext()).thenReturn(servletContext);
    HttpRequestWrapper oldRequest = mock(HttpRequestWrapper.class);
    SessionHelpersFacade.commitRequest(request, oldRequest, null);
    verify(oldRequest, never()).commit();
  }

  @Test
  public void testPrepareRequestNonHttp() {
    ServletRequest request = mock(ServletRequest.class);
    when(request.getServletContext()).thenReturn(servletContext);
    ServletResponse response = mock(ServletResponse.class);
    ServletContext originalServletContext = mock(ServletContext.class);
    ServletRequest result = SessionHelpersFacade.prepareRequest(request, response, originalServletContext);
    assertNotNull(result);
    assertSame(request, result);
  }

  @Test
  public void testPrepareRequestNotInitializedFilter() {
    ServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletContext()).thenReturn(servletContext);
    ServletResponse response = mock(HttpServletResponse.class);
    ServletRequest result = SessionHelpersFacade.prepareRequest(request, response, null);
    assertNotNull(result);
    assertNotSame(request, result);
    assertTrue(result instanceof HttpRequestWrapper);
    verify(request).setAttribute(eq(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE), refEq(result));
  }

  @Test
  public void testPrepareRequest() {
    ServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletContext()).thenReturn(servletContext);
    ServletResponse response = mock(HttpServletResponse.class);
    ServletContext originalServletContext = mock(ServletContext.class);
    when(originalServletContext.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(sessionManager);
    ServletRequest result = SessionHelpersFacade.prepareRequest(request, response, originalServletContext);
    assertNotNull(result);
    assertNotSame(request, result);
    assertTrue(result instanceof HttpRequestWrapper);
    verify(request).setAttribute(eq(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE), refEq(result));
  }

  @Test
  public void testPrepareAlreadyWappedRequestInSameContext() {
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    when(request.getServletContext()).thenReturn(servletContext);
    ServletResponse response = mock(HttpServletResponse.class);
    ServletContext originalServletContext = mock(ServletContext.class);
    when(originalServletContext.getAttribute(SessionHelpers.SESSION_HELPERS)).thenReturn(new SessionHelpers());
    when(request.getServletContext()).thenReturn(originalServletContext);
    when(request.getAttribute(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE)).thenReturn(request);
    ServletRequest result = SessionHelpersFacade.prepareRequest(request, response, originalServletContext);
    assertNotNull(result);
    assertSame(request, result);
    assertTrue(result instanceof HttpRequestWrapper);
  }

  @Test
  public void testPreparePreviouslyWappedRequestButRewrappedOnTop() {
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    ServletResponse response = mock(HttpServletResponse.class);
    ServletContext originalServletContext = mock(ServletContext.class);
    when(request.getServletContext()).thenReturn(originalServletContext);
    ServletRequest reWrappedRequest = mock(HttpServletRequest.class);
    ServletContext another = mock(ServletContext.class);
    when(another.getAttribute(SessionHelpers.SESSION_HELPERS)).thenReturn(new SessionHelpers());
    when(reWrappedRequest.getServletContext()).thenReturn(another);
    when(reWrappedRequest.getAttribute(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE)).thenReturn(request);
    ServletRequest result = SessionHelpersFacade.prepareRequest(reWrappedRequest, response, originalServletContext);
    assertNotNull(result);
    assertNotSame(request, result);
    assertSame(reWrappedRequest, result);
  }

  @Test
  public void testPrepareWappedRequestDifferentServletContext() {
    HttpRequestWrapper request = mock(HttpRequestWrapper.class);
    ServletResponse response = mock(HttpServletResponse.class);
    ServletContext originalServletContext = mock(ServletContext.class);
    originalServletContext.setAttribute(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE, request);
    when(originalServletContext.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(sessionManager);
    ServletRequest reWrappedRequest = mock(HttpServletRequest.class);
    when(reWrappedRequest.getAttribute(SessionHelpers.REQUEST_WRAPPED_ATTRIBUTE)).thenReturn(request);
    ServletContext another = mock(ServletContext.class);
    when(another.getAttribute(SessionHelpers.SESSION_HELPERS)).thenReturn(new SessionHelpers());
    SessionManager anotherSessionManager = mock(SessionManager.class);
    SessionConfiguration anotherSessionConfiguration = new SessionConfiguration();
    when(sessionManager.getConfiguration()).thenReturn(anotherSessionConfiguration);
    when(another.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(anotherSessionManager);
    when(reWrappedRequest.getServletContext()).thenReturn(another);
    ServletRequest result = SessionHelpersFacade.prepareRequest(reWrappedRequest, response, originalServletContext);
    assertNotNull(result);
    assertNotSame(reWrappedRequest, result);
    assertNotSame(request, result);
    assertTrue(result instanceof HttpRequestWrapper);
  }

  @Test
  public void testOnAddListener() {
    ServletContextDescriptor scd = new ServletContextDescriptor(servletContext);
    when(servletContext.getAttribute(Attributes.SERVLET_CONTEXT_DESCRIPTOR)).thenReturn(scd);
    sessionHelpers.onAddListener(servletContext, "Dummy");
    assertTrue(scd.getHttpSessionListeners().isEmpty());
    assertTrue(scd.getHttpSessionIdListeners().isEmpty());
    assertTrue(scd.getHttpSessionAttributeListeners().isEmpty());
    HttpSessionListener listener = mock(HttpSessionListener.class);
    HttpSessionIdListener idListener = mock(HttpSessionIdListener.class);
    HttpSessionAttributeListener attributeListener = mock(HttpSessionAttributeListener.class);
    HttpSessionListener multiListener = mock(HttpSessionListener.class,
        withSettings().extraInterfaces(HttpSessionAttributeListener.class));
    HttpSessionAttributeListener attributeMultiListener = (HttpSessionAttributeListener)multiListener;
    sessionHelpers.onAddListener(servletContext, listener);
    assertThat(scd.getHttpSessionListeners(), hasItem(listener));
    assertTrue(scd.getHttpSessionIdListeners().isEmpty());
    assertTrue(scd.getHttpSessionAttributeListeners().isEmpty());
    sessionHelpers.onAddListener(servletContext, idListener);
    assertThat(scd.getHttpSessionListeners(), hasItem(listener));
    assertThat(scd.getHttpSessionIdListeners(), hasItem(idListener));
    assertTrue(scd.getHttpSessionAttributeListeners().isEmpty());
    sessionHelpers.onAddListener(servletContext, attributeListener);
    assertThat(scd.getHttpSessionListeners(), hasItem(listener));
    assertThat(scd.getHttpSessionIdListeners(), hasItem(idListener));
    assertThat(scd.getHttpSessionAttributeListeners(), hasItem(attributeListener));
    sessionHelpers.onAddListener(servletContext, multiListener);
    assertThat(scd.getHttpSessionListeners(), hasItem(listener));
    assertThat(scd.getHttpSessionListeners(), hasItem(multiListener));
    assertThat(scd.getHttpSessionIdListeners(), hasItem(idListener));
    assertThat(scd.getHttpSessionAttributeListeners(), hasItem(attributeListener));
    assertThat(scd.getHttpSessionAttributeListeners(), hasItem(attributeMultiListener));
  }

  @Test
  public void testFindListenersByIntercepting() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession()).thenReturn(session);
    SessionHelpers.findListenersByIntercepting(servletContext, request);
    ArgumentCaptor<String> attribute = ArgumentCaptor.forClass(String.class);
    verify(session).setAttribute(attribute.capture(), any(String.class));
    verify(session).removeAttribute(attribute.getValue());
    verify(session).invalidate();
    verify(servletContext).setAttribute(SessionHelpers.INTROSPECTING_LISTENERS, Boolean.TRUE);
  }

  @Test
  public void testInterceptHttpListener() {
    EventListener caller = mock(EventListener.class);
    HttpSessionEvent event = mock(HttpSessionEvent.class);
    @SuppressWarnings("unchecked")
    Set<Object> listenerSet = mock(Set.class);
    HttpSession session = mock(HttpSession.class);
    when(session.getServletContext()).thenReturn(servletContext);
    when(servletContext.getAttribute(SessionHelpers.INTROSPECTING_LISTENERS)).thenReturn(listenerSet);
    when(event.getSession()).thenReturn(session);
    sessionHelpers.interceptHttpListener(caller, event);
    verify(listenerSet).add(caller);
  }

  @Test
  public void testInterceptHttpListenerAlreadyWrapped() {
    EventListener caller = mock(EventListener.class);
    HttpSessionEvent event = mock(HttpSessionEvent.class);
    @SuppressWarnings("unchecked")
    Set<Object> listenerSet = mock(Set.class);
    HttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getServletContext()).thenReturn(servletContext);
    when(servletContext.getAttribute(SessionHelpers.INTROSPECTING_LISTENERS)).thenReturn(listenerSet);
    when(event.getSession()).thenReturn(session);
    sessionHelpers.interceptHttpListener(caller, event);
    verify(listenerSet, never()).add(caller);
  }

  @Test
  public void testInterceptHttpListenerAlreadyIntercepted() {
    EventListener caller = mock(EventListener.class);
    HttpSessionEvent event = mock(HttpSessionEvent.class);
    @SuppressWarnings("unchecked")
    Set<Object> listenerSet = mock(Set.class);
    // Let's assume we intercepted the caller
    listenerSet.add(caller);
    HttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getServletContext()).thenReturn(servletContext);
    when(servletContext.getAttribute(SessionHelpers.INTROSPECTING_LISTENERS)).thenReturn(listenerSet);
    when(event.getSession()).thenReturn(session);
    sessionHelpers.interceptHttpListener(caller, event);
    // Called only once - in this method, not in interceptHttpListener
    verify(listenerSet, times(1)).add(caller);
  }
}
