package com.amadeus.session.servlet;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionNotifier;
import com.amadeus.session.SessionRepository;

@SuppressWarnings("javadoc")
public class TestHttpSessionFactory {

  private ServletContext servletContext;
  private SessionManager sessionManager;
  private HttpSessionFactory factory;
  private SessionData sessionData;
  private SessionConfiguration sessionConfiguration;

  @Before
  public void setup() {
    servletContext = mock(ServletContext.class);
    sessionManager = mock(SessionManager.class);
    sessionConfiguration = new SessionConfiguration();
    when(sessionManager.getConfiguration()).thenReturn(sessionConfiguration);
    when(sessionManager.getRepository()).thenReturn(mock(SessionRepository.class));
    when(sessionManager.getNotifier()).thenReturn(mock(SessionNotifier.class));
    factory = new HttpSessionFactory(servletContext);
    sessionData = new SessionData("1", 1000, 500);
  }

  @Test
  public void testBuildNoCache() {
    factory.setSessionManager(sessionManager);
    RepositoryBackedSession session = factory.build(sessionData);
    assertNotNull(session);
    assertEquals("1", session.getId());
  }

  @Test
  public void testBuildWithCache() {
    sessionConfiguration.setAllowedCachedSessionReuse(true);
    factory.setSessionManager(sessionManager);
    RepositoryBackedSession session = factory.build(sessionData);
    assertThat(session, instanceOf(RepositoryBackedHttpSession.class));
    assertNotNull(session);
    assertEquals("1", session.getId());
    session.setAttribute("A", "B");
    SessionData sessionDataConcurrent = new SessionData("1", 1000, 500);
    RepositoryBackedSession sessionConcurrent = factory.build(sessionDataConcurrent);
    assertNotNull(sessionConcurrent);
    assertEquals("1", sessionConcurrent.getId());
    assertEquals("B", sessionConcurrent.getAttribute("A"));
  }

  @Test
  public void testCommitted() {
    sessionConfiguration.setAllowedCachedSessionReuse(true);
    factory.setSessionManager(sessionManager);
    RepositoryBackedHttpSession session = (RepositoryBackedHttpSession)factory.build(sessionData);
    session.commit();
    factory.committed(session);
  }

  public void testSessionIdChange() {
    fail("Not yet implemented");
  }

}
