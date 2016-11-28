package com.amadeus.session.repository.redis;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionManager;

import redis.clients.util.SafeEncoder;

@SuppressWarnings("javadoc")
public class TestExpirationListener {

  private SessionManager sessionManager;
  private ExpirationListener expirationListener;

  @Before
  public void setUp() throws Exception {
    sessionManager = mock(SessionManager.class);
    expirationListener = new ExpirationListener(sessionManager , "myprefix");
  }

  @Test
  public void testOnPMessage() {
    String sessionId = "test-id";
    byte[] pattern = SafeEncoder.encode("");
    byte[] goodChannel = SafeEncoder.encode("_keyspace:test:expired");
    byte[] notExpireChannel = SafeEncoder.encode("_keyspace:test:expared");
    byte[] shortChannel = SafeEncoder.encode("expir");
    byte[] goodKey = SafeEncoder.encode("myprefix:key:test-id");
    byte[] shortKey = SafeEncoder.encode("myprefi");
    expirationListener.onPMessage(pattern, null, goodKey);
    verify(sessionManager, never()).deleteAsync(sessionId, true);
    expirationListener.onPMessage(pattern, goodChannel, null);
    verify(sessionManager, never()).deleteAsync(sessionId, true);
    expirationListener.onPMessage(pattern, notExpireChannel, goodKey);
    verify(sessionManager, never()).deleteAsync(sessionId, true);
    expirationListener.onPMessage(pattern, shortChannel, goodKey);
    verify(sessionManager, never()).deleteAsync(sessionId, true);
    expirationListener.onPMessage(pattern, goodChannel, shortKey);
    verify(sessionManager, never()).deleteAsync(sessionId, true);
    expirationListener.onPMessage(pattern, goodChannel, goodKey);
    verify(sessionManager).deleteAsync(sessionId, true);
  }
}
