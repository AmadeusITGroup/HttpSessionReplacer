package com.amadeus.session.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;

import org.junit.Test;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionConfiguration.ReplicationTrigger;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;

@SuppressWarnings("javadoc")
public class TestRepositoryBackendHttpSessionWrapper {

  @Test
  public void testWrapper() {
    SessionManager manager = mock(SessionManager.class);
    SessionConfiguration conf = new SessionConfiguration();
    conf.setReplicationTrigger(ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET);
    when(manager.getConfiguration()).thenReturn(conf);
    SessionData session = new SessionData("1", 1000, 10);
    ServletContext context = mock(ServletContext.class);
    HttpSessionFactory factory = mock(HttpSessionFactory.class);
    RepositoryBackedHttpSession originalSession = new RepositoryBackedHttpSession(context, session, manager, factory);
    RepositoryBackedHttpSession wrapped = new RepositoryBackedHttpSession(originalSession);
    assertFalse(wrapped.isCommitted());
    assertFalse(originalSession.isCommitted());
    wrapped.setCommitted(true);
    assertTrue(wrapped.isCommitted());
    assertFalse(originalSession.isCommitted());
    wrapped.setCommitted(false);
    assertFalse(wrapped.isCommitted());
    assertFalse(originalSession.isCommitted());
    originalSession.setCommitted(true);
    assertFalse(wrapped.isCommitted());
  }

}
