package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.repository.redis.NotificationExpirationManagement.SubscriptionRunner;
import com.amadeus.session.repository.redis.RedisFacade.TransactionRunner;

@SuppressWarnings("javadoc")
public class TestNotificationExpirationManagement {

  private RedisSessionRepository redisSession;
  private RedisFacade redis;
  private SessionData session;
  private NotificationExpirationManagement expiration;

  @Before
  public void setup() {
    redisSession = mock(RedisSessionRepository.class);
    redis = mock(RedisFacade.class);
    when(redisSession.sessionKey("1")).thenReturn(SafeEncoder.encode("key:{1}"));
    session = new SessionData("1", 100, 20);
    expiration = new NotificationExpirationManagement(redis, redisSession, "test", "this", "prefix", false);
  }

  @Test
  public void testSessionDeleted() {
    expiration.sessionDeleted(session);
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).srem(captureExpireKey.capture(), captureSessionKey.capture());
    assertEquals("prefixexpirations:60000", SafeEncoder.encode(captureExpireKey.getValue()));
    assertEquals("key:{1}", SafeEncoder.encode(captureSessionKey.getValue()));
    ArgumentCaptor<byte[]> captureSessionExpireKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).del(captureSessionExpireKey.capture());
    assertEquals("com.amadeus.session:expire::test:{1}", SafeEncoder.encode(captureSessionExpireKey.getValue()));
  }

  @Test
  public void testSessionDeletedIn2Minutes() {
    session = new SessionData("1", 100, 70);
    expiration.sessionDeleted(session);
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).srem(captureExpireKey.capture(), captureSessionKey.capture());
    assertEquals("prefixexpirations:120000", SafeEncoder.encode(captureExpireKey.getValue()));
  }

  @Test
  public void testSessionTouched() {
    expiration.sessionTouched(session);
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).sadd(captureExpireKey.capture(), captureSessionKey.capture());
    assertEquals("prefixexpirations:60000", SafeEncoder.encode(captureExpireKey.getValue()));
    assertEquals("key:{1}", SafeEncoder.encode(captureSessionKey.getValue()));
    ArgumentCaptor<Long> captureInstant = ArgumentCaptor.forClass(Long.class);
    verify(redis).expireAt(captureExpireKey.capture(), captureInstant.capture());
    assertEquals("prefixexpirations:60000", SafeEncoder.encode(captureExpireKey.getValue()));
    assertEquals(Long.valueOf(360), captureInstant.getValue());
    ArgumentCaptor<byte[]> captureValue = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<Integer> captureInt = ArgumentCaptor.forClass(Integer.class);
    verify(redis).setex(captureExpireKey.capture(), captureInt.capture(), captureValue.capture());
    assertEquals("com.amadeus.session:expire::test:{1}", SafeEncoder.encode(captureExpireKey.getValue()));
    assertEquals("", SafeEncoder.encode(captureValue.getValue()));
    assertEquals(Integer.valueOf(20), captureInt.getValue());
    verify(redis).expire(captureSessionKey.capture(), captureInt.capture());
    assertEquals("key:{1}", SafeEncoder.encode(captureSessionKey.getValue()));
    assertEquals(Integer.valueOf(320), captureInt.getValue());
  }

  @Test
  public void testSessionTouchedNotNewSessionChangeExpiryMinute() {
    session = new SessionData("1", 100, 70);
    session.setNew(false);
    expiration.sessionTouched(session);
    ArgumentCaptor<byte[]> captureExpireKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).srem(captureExpireKey.capture(), captureSessionKey.capture());
    assertEquals("prefixexpirations:60000", SafeEncoder.encode(captureExpireKey.getValue()));
    assertEquals("key:{1}", SafeEncoder.encode(captureSessionKey.getValue()));
  }

  @Test
  public void testSessionTouchedNotNewSession() {
    session = new SessionData("1", 100, 10);
    session.setNew(false);
    expiration.sessionTouched(session);
    verify(redis, never()).srem(any(byte[].class), any(byte[].class));
  }

  NotificationExpirationManagement setUp() {
    return expiration;
  }

  @Test
  public void testSessionTouchedNotNewSessionNeverExpires() {
    NotificationExpirationManagement neverExpires
      = new NotificationExpirationManagement(redis, redisSession, "test", "this", "prefix:", false);
    session = new SessionData("1", 100, 0);
    session.setNew(false);
    neverExpires.sessionTouched(session);
    ArgumentCaptor<byte[]> captureKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).del(captureKey.capture());
    assertEquals("com.amadeus.session:expire::test:{1}", SafeEncoder.encode(captureKey.getValue()));
    ArgumentCaptor<byte[]> captureKey2 = ArgumentCaptor.forClass(byte[].class);
    verify(redis).persist(captureKey2.capture());
    assertArrayEquals(SafeEncoder.encode("key:{1}"), captureKey2.getValue());
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis).srem(captureKey.capture(), captureSessionKey.capture());
    assertEquals("prefix:expirations:60000", SafeEncoder.encode(captureKey.getValue()));
    assertEquals("key:{1}", SafeEncoder.encode(captureSessionKey.getValue()));
  }

  @Test
  public void testSessionTouchedSticky() {
    NotificationExpirationManagement sticky = new NotificationExpirationManagement(redis, redisSession, "test", "this",
        "prefix:", true);
    sticky.sessionTouched(session);
    ArgumentCaptor<byte[]> captureKey = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<byte[]> captureSessionKey = ArgumentCaptor.forClass(byte[].class);
    verify(redis, times(2)).sadd(captureKey.capture(), captureSessionKey.capture());
    ArrayList<String> captured = new ArrayList<>();
    for (byte[] key : captureKey.getAllValues()) {
      captured.add(encode(key));
    }
    assertThat(captured, hasItem("prefix:forced-expirations:120000"));
    verify(redis, times(2)).expireAt(captureKey.capture(), any(long.class));
    captured.clear();
    for (byte[] key : captureSessionKey.getAllValues()) {
      captured.add(encode(key));
    }
    assertThat(captured, hasItem("key:{1}"));
  }

  @Test
  public void testSubscriptionRunner() {
    SessionManager sessionManager = mock(SessionManager.class);
    expiration.startExpiredSessionsTask(sessionManager );
    ArgumentCaptor<Runnable> cleanupCapture = ArgumentCaptor.forClass(Runnable.class);
    verify(sessionManager).submit(anyString(), cleanupCapture.capture());
    cleanupCapture.getValue().run();
    verify(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
  }

  @Test
  public void testSubscriptionRunnerWithJedisException() {
    SessionManager sessionManager = mock(SessionManager.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        throw new DummyException("Test");
      }
    }).when(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
    doAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return true;
      }
    }).when(redis).isRedisException(any(DummyException.class));
    final ArrayList<Long> delays = new ArrayList<>();
    SubscriptionRunner runner = expiration.new SubscriptionRunner(sessionManager) {
      @Override
      void doWait() {
        delays.add(getDelay());
      }
    };
    try {
      runner.run();
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(delays, hasItems(1000L, 1000L, 2000L, 3000L, 5000L, 8000L, 13000L, 21000L, 34000L, 55000L, 89000L, 144000L, 233000L));
    }
  }

  @Test
  public void testSubscriptionRunnerWithInterruptedThreadRedisException() {
    SessionManager sessionManager = mock(SessionManager.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Thread.currentThread().interrupt();
        throw new DummyException("Test");
      }
    }).when(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
    doAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return true;
      }
    }).when(redis).isRedisException(any(DummyException.class));
    SubscriptionRunner runner = spy(expiration.new SubscriptionRunner(sessionManager));
    runner.run();
    verify(runner, never()).doWait();
  }

  @Test
  public void testSubscriptionRunnerWithInterruptedJedisException() {
    SessionManager sessionManager = mock(SessionManager.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        throw new DummyException("Test", new InterruptedException());
      }
    }).when(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
    doAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return true;
      }
    }).when(redis).isRedisException(any(DummyException.class));
    SubscriptionRunner runner = spy(expiration.new SubscriptionRunner(sessionManager));
    runner.run();
    verify(runner, never()).doWait();
  }

  @Test
  public void testSubscriptionRunnerWithInterruptedOtherException() {
    SessionManager sessionManager = mock(SessionManager.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Thread.currentThread().interrupt();
        throw new Exception("Test");
      }
    }).when(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
    SubscriptionRunner runner = spy(expiration.new SubscriptionRunner(sessionManager));
    runner.run();
    verify(runner, never()).doWait();
  }

  @Test
  public void testSubscriptionRunnerWithOtherException() {
    SessionManager sessionManager = mock(SessionManager.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        throw new Exception("Test");
      }
    }).when(redis).psubscribe(any(RedisFacade.RedisPubSub.class), anyString());
    final ArrayList<Long> delays = new ArrayList<>();
    SubscriptionRunner runner = expiration.new SubscriptionRunner(sessionManager) {
      @Override
      void doWait() {
        delays.add(getDelay());
      }
    };
    try {
      runner.run();
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(delays, hasItems(1000L, 1000L, 2000L, 3000L, 5000L, 8000L, 13000L, 21000L, 34000L, 55000L, 89000L, 144000L, 233000L));
    }
  }

  @Test
  public void testRoundDownMinute() {
    assertEquals(0L, NotificationExpirationManagement.roundDownMinute(1234L));
    assertEquals(0L, NotificationExpirationManagement.roundDownMinute(12345L));
    assertEquals(60000L, NotificationExpirationManagement.roundDownMinute(62000L));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetKeysToExpire() {
    Set<byte[]> expected = Collections.singleton(new byte[] { 1 });
    RedisFacade.ResponseFacade<Object> value = mock(RedisFacade.ResponseFacade.class);
    when(value.get()).thenReturn(expected);
    when(redis.transaction(eq(encode("Test")), any(TransactionRunner.class))).thenReturn(value);
    Set<byte[]> result = expiration.getKeysToExpire(encode("Test"));
    assertSame(expected, result);
    verify(redis).transaction(eq(encode("Test")), any(TransactionRunner.class));
  }

  @Test
  public void testSessionExpireKeyBuilder() {
    NotificationExpirationManagement sticky = new NotificationExpirationManagement(redis, redisSession, "test", "this",
                                                                                   "prefix:", true);
    assertArrayEquals(sticky.getSessionExpireKey("10"), sticky.getSessionExpireKey("this", "10"));
  }
}
