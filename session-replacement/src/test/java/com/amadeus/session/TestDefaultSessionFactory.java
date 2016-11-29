package com.amadeus.session;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class TestDefaultSessionFactory {

  @Test
  public void testSessionIdChange() {
    DefaultSessionFactory sf = new DefaultSessionFactory();
    SessionConfiguration sc = new SessionConfiguration();
    sc.setAllowedCachedSessionReuse(true);
    SessionManager sessionManager = mock(SessionManager.class);
    when(sessionManager.getConfiguration()).thenReturn(sc);
    sf.setSessionManager(sessionManager);

    SessionData sessionData = new SessionData("1", 100L, 200);
    RepositoryBackedSession session = sf.build(sessionData);
    assertNotNull(session);
    sessionData.setNewSessionId("2");
    sf.sessionIdChange(sessionData);
  }

}
