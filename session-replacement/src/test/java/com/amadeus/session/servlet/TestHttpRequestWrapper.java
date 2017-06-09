package com.amadeus.session.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionManager;

@SuppressWarnings("javadoc")
public class TestHttpRequestWrapper {

  private static final String NEW_SESSION_ID = "11";
  private static final String SESSION_ID     = "10";
  private ServletContext      servletContext;
  private SessionManager      sessionManager;

  @Before
  public void setUp() throws Exception {
    servletContext = mock(ServletContext.class);
    sessionManager = mock(SessionManager.class);
    when(servletContext.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(sessionManager);
  }

  @Test
  public void testHttpRequestWrapper() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedSimple, servletContext);
    assertNull(req.getEmbeddedRequest());
    assertEquals(wrappedSimple, req.getRequest());
    assertEquals(servletContext, req.getServletContext());
    assertEquals(sessionManager, req.getManager());
  }

  @Test
  public void testHttpRequestWrapperOfWrappedRequest() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedSimple);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedComplex, servletContext);
    assertNull(req.getEmbeddedRequest());
    assertEquals(wrappedComplex, req.getRequest());
  }

  @Test
  public void testHttpRequestWrapperOfHttpRequestWrapper() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper wrappedHttpRequestWrapper = mock(HttpRequestWrapper.class);
    when(wrappedHttpRequestWrapper.getRequest()).thenReturn(wrappedSimple);
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedHttpRequestWrapper);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedComplex, servletContext);
    assertEquals(wrappedHttpRequestWrapper, req.getEmbeddedRequest());
    assertEquals(wrappedComplex, req.getRequest());
  }

  @Test
  public void testGetSession() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    RepositoryBackedHttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    HttpRequestWrapper wrappedHttpRequestWrapper = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    wrappedHttpRequestWrapper.session = session;
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedHttpRequestWrapper);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedComplex, servletContext);
    req.getSession();
    verify(wrappedHttpRequestWrapper).getSession(true);
    verify(wrappedHttpRequestWrapper).getSession(true);
  }
  
  @Test
  public void testNewSessionIdCreatedIfSessionWasInvalidated() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    RepositoryBackedHttpSession invalidSession = mock(RepositoryBackedHttpSession.class);
    RepositoryBackedHttpSession newSession = mock(RepositoryBackedHttpSession.class);
    when(newSession.getId()).thenReturn(NEW_SESSION_ID);
    when(invalidSession.getId()).thenReturn(SESSION_ID);
    when(invalidSession.isValid()).thenReturn(false);
    HttpRequestWrapper wrappedHttpRequestWrapper = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    wrappedHttpRequestWrapper.session = invalidSession;
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedHttpRequestWrapper);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedComplex, servletContext);
    when(sessionManager.getSession(req, true, SESSION_ID)).thenReturn(invalidSession);
    when(sessionManager.getSession(req, true, null)).thenReturn(newSession);
    RepositoryBackedHttpSession session2 = req.getSession();
    Assert.assertNotNull(session2);
    assertEquals(NEW_SESSION_ID, session2.getId());
  }

  @Test
  public void testCommitWithSession() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    RepositoryBackedHttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    HttpRequestWrapper req = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    req.session = session;
    req.commit();
    verify(req).propagateSession();
    verify(req).storeSession();
    verify(sessionManager).requestFinished();
  }

  @Test
  public void testEncodeUrl() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedSimple, servletContext);
    req.encodeURL("someUrl");
    verify(sessionManager).encodeUrl(req, "someUrl");
  }

  @Test
  public void testCommitWrapped() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    RepositoryBackedHttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    HttpRequestWrapper wrappedHttpRequestWrapper = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    wrappedHttpRequestWrapper.session = session;
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedHttpRequestWrapper);
    HttpRequestWrapper req = spy(new HttpRequestWrapper(wrappedComplex, servletContext));
    req.commit();
    verify(req).propagateSession();
    verify(req).storeSession();
    verify(sessionManager).requestFinished();
  }

  @Test
  public void testCommitted() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    RepositoryBackedHttpSession session = mock(RepositoryBackedHttpSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    HttpRequestWrapper wrappedHttpRequestWrapper = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    wrappedHttpRequestWrapper.session = session;
    HttpServletRequest wrappedComplex = new HttpServletRequestWrapper(wrappedHttpRequestWrapper);
    HttpRequestWrapper req = spy(new HttpRequestWrapper(wrappedComplex, servletContext));
    req.committed = true;
    req.commit();
  }


  @Test(expected=IllegalStateException.class)
  public void testGetSessionOnCommitted() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedSimple, servletContext);
    req.committed = true;
    req.getRepositoryBackedSession(false);
  }

  @Test(expected=IllegalStateException.class)
  public void testChangeSessionIdNoSession() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedSimple, servletContext);
    req.changeSessionId();
  }

  @Test
  public void testChangeSessionId() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = new HttpRequestWrapper(wrappedSimple, servletContext);
    RepositoryBackedHttpSession session = mock(RepositoryBackedHttpSession.class);
    when(sessionManager.getSession(req, false, null)).thenReturn(session);
    when(session.getId()).thenReturn(SESSION_ID);
    String id = req.changeSessionId();
    assertEquals(SESSION_ID, id);
    verify(sessionManager).switchSessionId(session);
    verify(session).getId();
  }

  @Test
  public void testGetRequestedSessionId() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    String id = req.getRequestedSessionId();
    assertEquals(null, id);
    verify(req).getSession(false);
  }

  @Test
  public void testGetSetRequestedSessionId() {
    HttpServletRequest wrappedSimple = mock(HttpServletRequest.class);
    HttpRequestWrapper req = spy(new HttpRequestWrapper(wrappedSimple, servletContext));
    req.setRequestedSessionId(SESSION_ID);
    String id = req.getRequestedSessionId();
    assertEquals(SESSION_ID, id);
    verify(req, never()).getSession(false);
  }
}
