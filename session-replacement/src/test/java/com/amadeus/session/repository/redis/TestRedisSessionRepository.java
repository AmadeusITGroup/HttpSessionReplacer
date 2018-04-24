package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.amadeus.session.JdkSerializerDeserializer;
import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

@SuppressWarnings("javadoc")
public class TestRedisSessionRepository {
  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void testRemove() {
    RedisFacade facade = mock(RedisFacade.class);
    SessionManager sm = mock(SessionManager.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      RepositoryBackedSession sess = mock(RepositoryBackedSession.class);
      SessionData sd = mock(SessionData.class);
      when(sess.getSessionData()).thenReturn(sd );
      when(sd.getId()).thenReturn("400");
      rsr.remove(sess.getSessionData());
      verify(facade).del(rsr.sessionKey("400"));
    }
  }

  @Test
  public void testSortedSetExpiration() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.ZRANGE, false)) {
      SessionConfiguration conf = new SessionConfiguration();
      SessionManager sm = mock(SessionManager.class);
      when(sm.getConfiguration()).thenReturn(conf);
      rsr.setSessionManager(sm);
      verify(sm).schedule(any(String.class), any(Runnable.class), eq(10L));
      conf.setMaxInactiveInterval(10);
      sm = mock(SessionManager.class);
      when(sm.getConfiguration()).thenReturn(conf);
      rsr.setSessionManager(sm);
      verify(sm).schedule(any(String.class), any(Runnable.class), eq(2L));
      sm = mock(SessionManager.class);
      when(sm.getConfiguration()).thenReturn(conf);
      conf.setMaxInactiveInterval(-100);
      rsr.setSessionManager(sm);
      verify(sm).schedule(any(String.class), any(Runnable.class), eq(10L));
    }
  }

  @Test
  public void testSessionData() {
    DummyRedisForHMGet facade = mock(DummyRedisForHMGet.class, CALLS_REAL_METHODS);
    SessionManager sm = mock(SessionManager.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      facade.hmget = Arrays.asList(null, null, null, null);
      assertNull("Session should be inconsistent", rsr.getSessionData("200"));
      facade.hmget = Arrays.asList(byteArray(8, 100), null, null, null);
      assertNull("Session should be inconsistent", rsr.getSessionData("200"));
      facade.hmget = Arrays.asList(null, byteArray(4, 4), null, null);
      assertNull("Session should be inconsistent", rsr.getSessionData("200"));
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 4), null, new byte[]{ 1 });
      assertNull("Session should be invalid", rsr.getSessionData("200"));
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 4), byteArray(8, 10), null);
      assertNotNull("Session should be valid", rsr.getSessionData("200"));
      assertEquals(100, rsr.getSessionData("200").getLastAccessedTime());
      assertEquals(4, rsr.getSessionData("200").getMaxInactiveInterval());
      assertEquals(10, rsr.getSessionData("200").getCreationTime());
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 5), byteArray(8, 1, 1), new byte[]{ 0 });
      assertNotNull("Session should be valid", rsr.getSessionData("200"));
      assertEquals(100, rsr.getSessionData("200").getLastAccessedTime());
      assertEquals(5, rsr.getSessionData("200").getMaxInactiveInterval());
      assertEquals(257, rsr.getSessionData("200").getCreationTime());
      assertNull(rsr.getSessionData("200").getPreviousOwner());
    }
  }


  @Test
  public void testSessionDataSticky() {
    DummyRedisForHMGet facade = mock(DummyRedisForHMGet.class, CALLS_REAL_METHODS);
    SessionManager sm = mock(SessionManager.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, true)) {
      rsr.setSessionManager(sm);
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 5), byteArray(8, 1, 1), new byte[]{ 0 }, encode("old"));
      assertNotNull(rsr.getSessionData("200"));
      assertEquals("old", rsr.getSessionData("200").getPreviousOwner());
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 5), byteArray(8, 1, 1), new byte[]{ 0 }, null);
      assertNotNull(rsr.getSessionData("200"));
      assertNull(rsr.getSessionData("200").getPreviousOwner());
      facade.hmget = Arrays.asList(byteArray(8, 100), byteArray(4, 5), byteArray(8, 1, 1), new byte[]{ 0 });
      thrown.expect(ArrayIndexOutOfBoundsException.class);
      rsr.getSessionData("200");
    }
  }

  @Test
  public void testSessionDataInvalidData() {
    DummyRedisForHMGet facade = mock(DummyRedisForHMGet.class, CALLS_REAL_METHODS);
    SessionManager sm = mock(SessionManager.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      facade.hmget = Arrays.asList(byteArray(5, 100), byteArray(4, 5), byteArray(8, 1, 1), new byte[]{ 0 });
      thrown.expect(BufferUnderflowException.class);
      rsr.getSessionData("200");
    }
  }

  /**
   * Fills array with values from the end
   * @param length
   * @param values
   * @return
   */
  static byte[] byteArray(int length, int...values) {
    byte[] res = new byte[length];
    for (int i = Math.min(values.length, length); i > 0; i--) {
      res[res.length - i] = (byte)values[values.length - i ];
    }
    return res;
  }

  private static abstract class DummyRedisForHMGet implements RedisFacade {

    List<byte[]> hmget;

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
      return hmget;
    }
  }

  @Test
  public void testGetAllKeys() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      Set<byte[]> result = Collections.singleton(new byte[] { 65 });
      when(facade.hkeys(rsr.sessionKey("400"))).thenReturn(result);
      Set<String> s = rsr.getAllKeys(new SessionData("400", 100, 10));
      assertEquals(1, s.size());
      assertEquals("A", s.toArray()[0]);
      verify(facade, times(1)).hkeys(rsr.sessionKey("400"));
    }
  }

  @Test
  public void testPrepareRemove() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      byte[] key = new byte[] { 35, 58, 105, 110, 118, 97, 108, 105, 100, 83, 101, 115, 115, 105, 111, 110 };
      byte[] value = RedisSessionRepository.BYTES_TRUE;
      Map<byte[], byte[]> result = new HashMap<>();
      result.put(key, value);
      rsr.prepareRemove(new SessionData("401", 100, 10));
      verify(facade, times(1)).hsetnx(argThat(matchesArray(rsr.sessionKey("401"))), argThat(matchesArray(key)), argThat(matchesArray(value)));
    }
  }

  @Test
  public void testGetAttribute() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      SessionManager sessionManager = mock(SessionManager.class);
      when(sessionManager.getMetrics()).thenReturn(new MetricRegistry());
      JdkSerializerDeserializer sd = new JdkSerializerDeserializer();
      sd.setSessionManager(sessionManager);
      Mockito.when(sessionManager.getSerializerDeserializer()).thenReturn(sd);
      rsr.setSessionManager(sessionManager);
      Mockito.when(facade.hmget(rsr.sessionKey("402"), SafeEncoder.encode("ATTR")))
          .thenReturn(Collections.singletonList(sd.serialize("Result")));

      Object res = rsr.getSessionAttribute(new SessionData("402", 100, 10), "ATTR");
      verify(facade, times(1)).hmget(eq(rsr.sessionKey("402")), eq(SafeEncoder.encode("ATTR")));
      assertEquals("Result", res);
    }
  }

  @Test
  public void testSwitchId() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("new-id");
      when(sessionData.getOldSessionId()).thenReturn("old-id");
      rsr.sessionIdChange(sessionData);
      ArgumentCaptor<byte[]> oldkey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newkey = ArgumentCaptor.forClass(byte[].class);
      verify(facade, times(2)).rename(oldkey.capture(), newkey.capture());
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldkey.getAllValues().get(0)));
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newkey.getAllValues().get(0)));
      NotificationExpirationManagement em = (NotificationExpirationManagement)rsr.expirationManager;
      assertEquals(encode(em.getSessionExpireKey("old-id")), encode(oldkey.getAllValues().get(1)));
      assertEquals(encode(em.getSessionExpireKey("new-id")), encode(newkey.getAllValues().get(1)));
      ArgumentCaptor<byte[]> expirekey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newSessionKey = ArgumentCaptor.forClass(byte[].class);
      verify(facade).sadd(expirekey.capture(), newSessionKey.capture());
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newSessionKey.getValue()));
      ArgumentCaptor<byte[]> oldSessionKey = ArgumentCaptor.forClass(byte[].class);
      verify(facade).srem(expirekey.capture(), oldSessionKey.capture());
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldSessionKey.getValue()));
    }
  }

  @Test
  public void testSwitchIdSticky() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, true)) {
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("new-id");
      when(sessionData.getOldSessionId()).thenReturn("old-id");
      rsr.sessionIdChange(sessionData);
      ArgumentCaptor<byte[]> oldkey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newkey = ArgumentCaptor.forClass(byte[].class);
      verify(facade, times(2)).rename(oldkey.capture(), newkey.capture());
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldkey.getAllValues().get(0)));
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newkey.getAllValues().get(0)));
      NotificationExpirationManagement em = (NotificationExpirationManagement)rsr.expirationManager;
      assertEquals(encode(em.getSessionExpireKey("old-id")), encode(oldkey.getAllValues().get(1)));
      assertEquals(encode(em.getSessionExpireKey("new-id")), encode(newkey.getAllValues().get(1)));
      ArgumentCaptor<byte[]> expirekey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newSessionKey = ArgumentCaptor.forClass(byte[].class);
      verify(facade, times(2)).sadd(expirekey.capture(), newSessionKey.capture());
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newSessionKey.getAllValues().get(0)));
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newSessionKey.getAllValues().get(1)));
      ArgumentCaptor<byte[]> oldSessionKey = ArgumentCaptor.forClass(byte[].class);
      verify(facade, times(2)).srem(expirekey.capture(), oldSessionKey.capture());
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldSessionKey.getAllValues().get(0)));
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldSessionKey.getAllValues().get(1)));
    }
  }

  public static ArgumentMatcher<byte[]> matchesArray(final byte[] arr) {
    return new ArgumentMatcher<byte[]>() {
      @Override
      public boolean matches(byte[] arg) {
        if (arg instanceof byte[]) {
          return Arrays.equals(arr, (byte[]) arg);
        }
        return false;
      }
    };
  }

  @Test
  public void testSwitchIdSortedSet() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.ZRANGE, false)) {
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("new-id");
      when(sessionData.getOldSessionId()).thenReturn("old-id");
      rsr.sessionIdChange(sessionData);
      ArgumentCaptor<byte[]> oldkey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newkey = ArgumentCaptor.forClass(byte[].class);
      verify(facade).rename(oldkey.capture(), newkey.capture());
      assertEquals(encode(rsr.sessionKey("old-id")), encode(oldkey.getValue()));
      assertEquals(encode(rsr.sessionKey("new-id")), encode(newkey.getValue()));
      ArgumentCaptor<byte[]> expirekey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> newSessionKey = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<byte[]> oldSessionKey = ArgumentCaptor.forClass(byte[].class);
      verify(facade).zrem(expirekey.capture(), oldSessionKey.capture());
      assertEquals("old-id", encode(oldSessionKey.getValue()));
      ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
      verify(facade).zadd(expirekey.capture(), score.capture(), newSessionKey.capture());
      assertEquals("new-id", encode(newSessionKey.getValue()));
    }
  }

  @Test
  public void testExtractSessionId() {
    assertEquals("", RedisSessionRepository.extractSessionId(""));
    assertEquals("123123", RedisSessionRepository.extractSessionId("123123"));
    assertEquals("123123", RedisSessionRepository.extractSessionId("abc:123123"));
    assertEquals("123123", RedisSessionRepository.extractSessionId("abc:{123123}"));
    assertEquals("0123123", RedisSessionRepository.extractSessionId("abc:0{123123}"));
    assertEquals("0123123", RedisSessionRepository.extractSessionId("abc:def:0{123123}"));
    assertEquals("{123123", RedisSessionRepository.extractSessionId("abc:{123123"));
  }

  @Test
  public void testRemoveSessionAttribute() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("id");
      rsr.removeSessionAttribute(sessionData, "attr");
      verify(facade).hdel(rsr.sessionKey("id"), encode("attr"));
    }
  }

  @Test
  public void testSetSessionAttribute() {
    RedisFacade facade = mock(RedisFacade.class);
    SessionManager sm = mock(SessionManager.class);
    when(sm.getMetrics()).thenReturn(new MetricRegistry());
    JdkSerializerDeserializer serializer = new JdkSerializerDeserializer();
    when(sm.getSerializerDeserializer()).thenReturn(serializer);
    serializer.setSessionManager(sm);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("id");
      rsr.setSessionAttribute(sessionData, "attr", "value");
      verify(facade).hset(rsr.sessionKey("id"), encode("attr"), serializer.serialize("value"));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testStoreSessionData() {
    RedisFacade facade = mock(RedisFacade.class);
    SessionManager sm = mock(SessionManager.class);
    JdkSerializerDeserializer serializer = new JdkSerializerDeserializer();
    when(sm.getSerializerDeserializer()).thenReturn(serializer);
    SessionConfiguration conf = new SessionConfiguration();
    when(sm.getConfiguration()).thenReturn(conf);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      SessionData sessionData = mock(SessionData.class);
      when(sessionData.getId()).thenReturn("id");
      rsr.storeSessionData(sessionData);
      @SuppressWarnings("rawtypes")
      ArgumentCaptor<Map> map = ArgumentCaptor.forClass(Map.class);
      verify(facade).hmset(eq(rsr.sessionKey("id")), map.capture());
    }
  }

  @Test
  public void testGetSessionKey() {
    RedisFacade facade = mock(RedisFacade.class);
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      assertEquals("com.amadeus.session::myapp:{test}", encode(rsr.getSessionKey(encode("test"))));
    }
  }

  @Test
  public void testInternalPrefix() {
    assertFalse(RedisSessionRepository.hasInternalPrefix(encode("test")));
    assertTrue(RedisSessionRepository.hasInternalPrefix(encode("#:test")));
    assertFalse(RedisSessionRepository.hasInternalPrefix(encode("#:")));
    assertFalse(RedisSessionRepository.hasInternalPrefix(encode("#test")));
    assertTrue(RedisSessionRepository.hasInternalPrefix(encode("#:t")));
  }


  @Test
  public void testSetMetrics() {
    RedisFacade facade = mock(RedisFacade.class);
    SessionManager sm = mock(SessionManager.class);
    MetricRegistry metrics = spy(new MetricRegistry());
    when(sm.getMetrics()).thenReturn(metrics);
    metrics.meter("com.amadeus.session.myapp.redis.sample");
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, false)) {
      rsr.setSessionManager(sm);
      verify(metrics).removeMatching(any(MetricFilter.class));
      verify(metrics, never()).meter("com.amadeus.session.myapp.redis.failover");
    }
  }
  @Test
  public void testSetMetricsSticky() {
    RedisFacade facade = mock(RedisFacade.class);
    SessionManager sm = mock(SessionManager.class);
    MetricRegistry metrics = spy(new MetricRegistry());
    when(sm.getMetrics()).thenReturn(metrics );
    try (RedisSessionRepository rsr = new RedisSessionRepository(facade, "myapp", "localhost", ExpirationStrategy.NOTIF, true)) {
      rsr.setSessionManager(sm);
      verify(metrics).meter("com.amadeus.session.myapp.redis.failover");
    }
  }
}
