package com.amadeus.session.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
public class TestUrlSessionTracking {

  private UrlSessionTracking urlSessionTracking;

  @Before
  public void setup() {
    urlSessionTracking = new UrlSessionTracking();
  }

  @Test
  public void testEncodeUrl() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("asession");
    urlSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class, withSettings().extraInterfaces(HttpServletRequest.class));
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(request.getRepositoryBackedSession(false)).thenReturn(session);
    when(session.isValid()).thenReturn(Boolean.FALSE);
    String url = urlSessionTracking.encodeUrl(request, "http://www.example.com");
    assertEquals("Session is not valid, so URL should stay the same", "http://www.example.com", url);
    when(session.isValid()).thenReturn(Boolean.TRUE);
    when(session.getId()).thenReturn("1234");
    url = urlSessionTracking.encodeUrl(request, "http://www.example.com");
    assertEquals("Session is valid, so URL should be encoded", "http://www.example.com;asession=1234", url);
    url = urlSessionTracking.encodeUrl(request, "http://www.example.com/abc");
    assertEquals("Session is valid, so URL should be encoded", "http://www.example.com/abc;asession=1234", url);
    url = urlSessionTracking.encodeUrl(request, "http://www.example.com/def?abc");
    assertEquals("Session is valid, so URL should be encoded", "http://www.example.com/def;asession=1234?abc", url);
    url = urlSessionTracking.encodeUrl(request, "http://www.example.com/def?abc?");
    assertEquals("Session is valid, so URL should be encoded", "http://www.example.com/def;asession=1234?abc?", url);
  }

  @Test
  public void testRetrieveId() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    sc.setAttribute("com.amadeus.session.id", "uuid");
    urlSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class, withSettings().extraInterfaces(HttpServletRequest.class));
    HttpServletRequest hsr = (HttpServletRequest)request;
    UUID uuid = UUID.randomUUID();
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+uuid);
    String id = urlSessionTracking.retrieveId(request);
    assertEquals(uuid.toString(), id);

    String sessionIdWithTimestamp = uuid.toString() + BaseSessionTracking.SESSION_ID_TIMESTAMP_SEPARATOR + System.currentTimeMillis();
    String invalidSessionIdWithTimestamp = uuid.toString() + "-abcdefgh" + BaseSessionTracking.SESSION_ID_TIMESTAMP_SEPARATOR + System.currentTimeMillis();
    String sessionIdWithoutTimestamp = uuid.toString();

    when(hsr.getPathInfo()).thenReturn(";somesession="+sessionIdWithTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+sessionIdWithTimestamp);
    assertNull(urlSessionTracking.retrieveId(request));
    when(hsr.getPathInfo()).thenReturn(";somesession="+sessionIdWithoutTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+sessionIdWithoutTimestamp);
    assertEquals(sessionIdWithoutTimestamp, urlSessionTracking.retrieveId(request));
    when(hsr.getPathInfo()).thenReturn(";somesession="+invalidSessionIdWithTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+invalidSessionIdWithTimestamp);
    assertNull(urlSessionTracking.retrieveId(request));

    sc.setTimestampSufix(true);
    urlSessionTracking.configure(sc);
    when(hsr.getPathInfo()).thenReturn(";somesession="+sessionIdWithTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+sessionIdWithTimestamp);
    assertEquals(sessionIdWithTimestamp, urlSessionTracking.retrieveId(request));
    when(hsr.getPathInfo()).thenReturn(";somesession="+sessionIdWithoutTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+sessionIdWithoutTimestamp);
    assertEquals(sessionIdWithoutTimestamp, urlSessionTracking.retrieveId(request));
    when(hsr.getPathInfo()).thenReturn(";somesession="+invalidSessionIdWithTimestamp);
    when(hsr.getRequestURI()).thenReturn("/url;somesession="+invalidSessionIdWithTimestamp);
    assertNull(urlSessionTracking.retrieveId(request));
  }

  @Test
  public void testBadId() {
    SessionConfiguration sc = new SessionConfiguration();
    sc.setSessionIdName("somesession");
    urlSessionTracking.configure(sc);
    RequestWithSession request = mock(RequestWithSession.class, withSettings().extraInterfaces(HttpServletRequest.class));
    HttpServletRequest hsr = (HttpServletRequest)request;
    when(hsr.getRequestURI()).thenReturn("/url;somesession=");
    String id = urlSessionTracking.retrieveId(request);
    assertNull(id);
  }

}

