package com.amadeus.session.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
public class TestCookieSessionTracking {

  private CookieSessionTracking cookieSessionTracking;

  @Before
  public void setup() {
    cookieSessionTracking = new CookieSessionTracking();
  }

  @Test
  public void testEncodeUrl() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("asession");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(request.getRepositoryBackedSession(false)).thenReturn(session);
    when(session.isValid()).thenReturn(Boolean.FALSE);
    String url = cookieSessionTracking.encodeUrl(request, "http://www.example.com");
    assertEquals("http://www.example.com", url);
    when(session.isValid()).thenReturn(Boolean.TRUE);
    when(session.getId()).thenReturn("1234");
    url = cookieSessionTracking.encodeUrl(request, "http://www.example.com");
    assertEquals("Session is valid, but URL stames the same", "http://www.example.com", url);
  }

  @Test
  public void testRetrieveId() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    sc.setAttribute("com.amadeus.session.id", "uuid");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    HttpServletRequest hsr = (HttpServletRequest)request;
    UUID uuid = UUID.randomUUID();
    UUID uuidUrl = UUID.randomUUID();
    when(hsr.getRequestURI()).thenReturn("/url;somesession=" + uuidUrl);
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", uuid.toString()) });
    assertEquals(uuid.toString(), cookieSessionTracking.retrieveId(request));
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("notused", uuid.toString()) });
    assertNull(cookieSessionTracking.retrieveId(request));
    when(hsr.getCookies()).thenReturn(new Cookie[] {
        new Cookie("othercookie", "ABC"),
        new Cookie("somesession", uuid.toString())
        });
    assertEquals(uuid.toString(), cookieSessionTracking.retrieveId(request));

    String sessionIdWithTimestamp = uuid.toString() + BaseSessionTracking.SESSION_ID_TIMESTAMP_SEPARATOR + System.currentTimeMillis();
    String invalidSessionIdWithTimestamp = uuid.toString() + "-abcdefgh" + BaseSessionTracking.SESSION_ID_TIMESTAMP_SEPARATOR + System.currentTimeMillis();
    String sessionIdWithoutTimestamp = uuid.toString();

    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", sessionIdWithTimestamp)});
    assertNull(cookieSessionTracking.retrieveId(request));
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", sessionIdWithoutTimestamp)});
    assertEquals(sessionIdWithoutTimestamp, cookieSessionTracking.retrieveId(request));

    sc.setTimestampSufix(true);
    cookieSessionTracking.configure(sc);

    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", sessionIdWithTimestamp)});
    assertEquals(sessionIdWithTimestamp, cookieSessionTracking.retrieveId(request));
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", sessionIdWithoutTimestamp)});
    assertEquals(sessionIdWithoutTimestamp, cookieSessionTracking.retrieveId(request));
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somesession", invalidSessionIdWithTimestamp)});
    assertNull(cookieSessionTracking.retrieveId(request));
  }

  @Test
  public void testBadId() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    HttpServletRequest hsr = (HttpServletRequest)request;
    when(hsr.getCookies()).thenReturn(new Cookie[] { new Cookie("somession", "1234") });
    assertNull(cookieSessionTracking.retrieveId(request));
  }

  @Test
  public void testNoSessionPropagation() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    when(((HttpServletRequest)request).getContextPath()).thenReturn("path");
    HttpServletResponse response = mock(HttpServletResponse.class);
    cookieSessionTracking.propagateSession(request, response);
    ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookie.capture());
    assertEquals("somesession", cookie.getValue().getName());
    assertEquals(0, cookie.getValue().getMaxAge());
    assertEquals("path/", cookie.getValue().getPath());
  }

  @Test
  public void testInvalidSessionPropagation() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    when(((HttpServletRequest)request).getContextPath()).thenReturn("path");
    RepositoryBackedSession session = mock(RepositoryBackedSession .class);
    when(session.getId()).thenReturn("123");
    when(session.isValid()).thenReturn(false);
    when(request.getRepositoryBackedSession(false)).thenReturn(session);
    HttpServletResponse response = mock(HttpServletResponse.class);
    cookieSessionTracking.propagateSession(request, response);
    ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookie.capture());
    assertEquals("somesession", cookie.getValue().getName());
    assertEquals("", cookie.getValue().getValue());
    assertEquals(0, cookie.getValue().getMaxAge());
    assertEquals("path/", cookie.getValue().getPath());
  }

  @Test
  public void testSessionPropagation() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    cookieSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class,
        withSettings().extraInterfaces(HttpServletRequest.class));
    when(((HttpServletRequest)request).getContextPath()).thenReturn("path");
    RepositoryBackedSession session = mock(RepositoryBackedSession .class);
    when(session.getId()).thenReturn("123");
    when(session.isValid()).thenReturn(true);
    when(request.getRepositoryBackedSession(false)).thenReturn(session);
    HttpServletResponse response = mock(HttpServletResponse.class);
    cookieSessionTracking.propagateSession(request, response);
    ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookie.capture());
    assertEquals("somesession", cookie.getValue().getName());
    assertEquals("123", cookie.getValue().getValue());
    assertEquals(-1, cookie.getValue().getMaxAge());
    assertEquals("path/", cookie.getValue().getPath());
  }
}
