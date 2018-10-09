package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.repository.redis.SortedSetSessionExpirationManagement.CleanupTask;

@SuppressWarnings("javadoc")
public class TestSortedSetExpiration {

  private RedisSessionRepository redisSession;
  private RedisFacade redis;
  private SortedSetSessionExpirationManagement expiration;
  private SessionData session;

  @Before
  public void setup() {
    redisSession = mock(RedisSessionRepository.class);
    redis = mock(RedisFacade.class);
    when(redisSession.sessionKey("1")).thenReturn(new byte[]{'1'});
    expiration = new SortedSetSessionExpirationManagement(redis, redisSession, "test", false, null);
    session = new SessionData("1", 100, 10);
  }
  @Test
  public void testSessionDeleted() {
    expiration.sessionDeleted(session);
    ArgumentCaptor<byte[]> captureKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureFields = ArgumentCaptor.forClass(byte[].class);
    verify(redis).zrem(captureKey.capture(), captureFields.capture());
    byte[] expectedKey = SafeEncoder.encode(SortedSetSessionExpirationManagement.ALLSESSIONS_KEY + "test");
    assertArrayEquals(expectedKey, captureKey.getValue());
    assertNotNull(captureFields.getValue());
    assertEquals(1, captureFields.getAllValues().size());
    assertArrayEquals(new byte[]{'1'}, captureFields.getAllValues().get(0));
  }

  @Test
  public void testSessionTouched() {
    expiration.initPollingIntervals(300);
    expiration.sessionTouched(session);
    byte[] expectedKey = SafeEncoder.encode(SortedSetSessionExpirationManagement.ALLSESSIONS_KEY + "test");
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Double> captureScore = ArgumentCaptor.forClass(Double.class);
    verify(redis).zadd(captureExpireKey.capture(), captureScore.capture(), captureSessionKey.capture());
    assertArrayEquals(expectedKey, captureExpireKey.getValue());
    assertArrayEquals(new byte[]{'1'}, captureSessionKey.getValue());
    assertEquals(Double.valueOf(10100), captureScore.getValue());
    ArgumentCaptor<Integer> captureExpireAt = ArgumentCaptor.forClass(Integer.class);
    verify(redis).expire(captureSessionKey.capture(), captureExpireAt.capture());
    assertArrayEquals(new byte[]{'1'}, captureSessionKey.getValue());
    assertEquals(310, captureExpireAt.getValue().intValue());
  }

  @Test
  public void testManageNoExpiration() {
    session = new SessionData("1", 100, 0);
    expiration.sessionTouched(session);
    byte[] expectedKey = SafeEncoder.encode(SortedSetSessionExpirationManagement.ALLSESSIONS_KEY + "test");
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Double> captureScore = ArgumentCaptor.forClass(Double.class);
    verify(redis).zadd(captureExpireKey.capture(), captureScore.capture(), captureSessionKey.capture());
    assertArrayEquals(expectedKey, captureExpireKey.getValue());
    assertArrayEquals(new byte[]{'1'}, captureSessionKey.getValue());
    assertEquals(Double.valueOf(Double.MAX_VALUE), captureScore.getValue());
    verify(redis).persist(captureSessionKey.capture());
    assertArrayEquals(new byte[]{'1'}, captureSessionKey.getValue());
  }

  @Test
  public void testSessionIdChanged() {
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Double> captureInstant = ArgumentCaptor.forClass(Double.class);
    ArgumentCaptor<byte[]> captureValue = ArgumentCaptor.forClass(byte[].class);
    when(redisSession.sessionKey("2")).thenReturn(new byte[]{'2'});
    session.setNewSessionId("2");
    expiration.sessionIdChange(session);
    verify(redis).zrem(captureExpireKey.capture(), captureValue.capture());
    assertEquals("com.amadeus.session:all-sessions-set:test", encode(captureExpireKey.getValue()));
    assertEquals("1", encode(captureValue.getValue()));
    verify(redis).zadd(captureExpireKey.capture(), captureInstant.capture(), captureValue.capture());
    assertEquals("com.amadeus.session:all-sessions-set:test", encode(captureExpireKey.getValue()));
    assertEquals(Double.valueOf(10100), captureInstant.getValue());
    assertEquals("2", encode(captureValue.getValue()));
  }

  @Test
  public void testSessionCleanupEmptyZrange() {
    SessionManager manager = mock(SessionManager.class);
    CleanupTask task = expiration.new CleanupTask(manager);
    long now = System.currentTimeMillis();
    task.run();
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Double> captureStart = ArgumentCaptor.forClass(Double.class);
    ArgumentCaptor<Double> captureEnd= ArgumentCaptor.forClass(Double.class);
    verify(redis).zrangeByScore(captureExpireKey.capture(), captureStart.capture(), captureEnd.capture());
    assertEquals(Double.valueOf(0), captureStart.getAllValues().get(0));
    assertTrue(now <= captureEnd.getAllValues().get(0).longValue());
  }

  @Test
  public void testSessionCleanupZrange() {
    SessionManager manager = mock(SessionManager.class);
    CleanupTask task = expiration.new CleanupTask(manager);
    long now = System.currentTimeMillis();
    // Using LinkedHashSet to guarantee the order
    Set<byte[]> zrange = new LinkedHashSet<>();
    byte[] key1 = new byte[]{'1'};
    zrange.add(key1);
    byte[] key2 = new byte[]{'2'};
    zrange.add(key2);
    when(redis.zrangeByScore(any(byte[].class), any(double.class), any(double.class))).thenReturn(zrange);
    when(redis.zrem(any(byte[].class), eq(key1))).thenReturn(Long.valueOf(1L));
    when(redis.zrem(any(byte[].class), eq(key2))).thenReturn(Long.valueOf(0L));
    task.run();
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Double> captureStart = ArgumentCaptor.forClass(Double.class);
    ArgumentCaptor<Double> captureEnd= ArgumentCaptor.forClass(Double.class);
    verify(redis).zrangeByScore(captureExpireKey.capture(), captureStart.capture(), captureEnd.capture());
    assertEquals(Double.valueOf(0), captureStart.getAllValues().get(0));
    assertTrue(captureEnd.getAllValues().get(0).longValue() >= now);
    ArgumentCaptor<byte[]> captureKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis, times(2)).zrem(captureExpireKey.capture(), captureKey.capture());

    assertEquals("1", encode(captureKey.getAllValues().get(0)));
    assertEquals("2", encode(captureKey.getAllValues().get(1)));
    verify(manager).delete("1", true);
    verify(manager, never()).delete("2", true);
  }

  @Test
  public void testStartCleanup() {
    SessionManager manager = mock(SessionManager.class);
    SessionConfiguration conf = new SessionConfiguration();
    when(manager.getConfiguration()).thenReturn(conf);
    expiration.startExpiredSessionsTask(manager);
    verify(manager).schedule(anyString(), any(Runnable.class), anyLong());
  }
}
