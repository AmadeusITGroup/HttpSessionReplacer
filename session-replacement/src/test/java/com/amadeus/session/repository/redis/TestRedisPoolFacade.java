package com.amadeus.session.repository.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amadeus.session.repository.redis.RedisFacade.RedisTransaction;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;

@SuppressWarnings("javadoc")
public class TestRedisPoolFacade {
  private Pool<Jedis> pool;
  private RedisPoolFacade rf;
  private Jedis jedis;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    jedis = mock(Jedis.class);
    pool = mock(Pool.class);
    when(pool.getResource()).thenReturn(jedis);
    rf = new RedisPoolFacade(pool);
  }

  @Test
  public void testGet() {
    byte[] key = new byte[]{};
    rf.get(key);
    verify(jedis).get(key);
  }

  @Test
  public void testHdel() {
    byte[] key = new byte[]{};
    byte[] fields = new byte[]{};
    rf.hdel(key, fields);
    verify(jedis).hdel(key, fields);
  }

  @Test
  public void testPublish() {
    byte[] channel = new byte[]{};
    byte[] message = new byte[]{};
    rf.publish(channel, message);
    verify(jedis).publish(channel, message);
  }

  @Test
  public void testHmget() {
    byte[] key = new byte[]{};
    byte[] fields = new byte[]{};
    rf.hmget(key, fields);
    verify(jedis).hmget(key, fields);
  }

  @Test
  public void testHmset() {
    byte[] key = new byte[]{};
    @SuppressWarnings("unchecked")
    Map<byte[], byte[]> fields = mock(Map.class);
    rf.hmset(key, fields);
    verify(jedis).hmset(key, fields);
  }

  @Test
  public void testHsetnx() {
    byte[] key = new byte[]{};
    byte[] fields = new byte[]{};
    byte[] values = new byte[]{};
    rf.hsetnx(key, fields, values);
    verify(jedis).hsetnx(key, fields, values);
  }

  @Test
  public void testHkeys() {
    byte[] key = new byte[]{};
    rf.hkeys(key);
    verify(jedis).hkeys(key);
  }

  @Test
  public void testSet() {
    byte[] key = new byte[]{};
    byte[] value = new byte[]{};
    rf.set(key, value);
    verify(jedis).set(key, value);
  }

  @Test
  public void testHset() {
    byte[] key = new byte[]{};
    byte[] field = new byte[]{};
    byte[] value = new byte[]{};
    rf.hset(key, field, value);
    verify(jedis).hset(key, field, value);
  }

  @Test
  public void testSetex() {
    byte[] key = new byte[]{};
    byte[] value = new byte[]{};
    int expiry = 10;
    rf.setex(key, expiry, value);
    verify(jedis).setex(key, expiry, value);
  }

  @Test
  public void testExpire() {
    byte[] key = new byte[]{};
    int value = 1;
    rf.expire(key, value);
    verify(jedis).expire(key, 1);
  }

  @Test
  public void testSrem() {
    byte[] key = new byte[]{};
    byte[] value = new byte[]{};
    rf.srem(key, value);
    verify(jedis).srem(key, value);
  }

  @Test
  public void testSadd() {
    byte[] key = new byte[]{};
    byte[] value = new byte[]{};
    rf.sadd(key, value);
    verify(jedis).sadd(key, value);
  }

  @Test
  public void testDel() {
    byte[] key = new byte[]{};
    rf.del(key, key);
    verify(jedis).del(key, key);
  }

  @Test
  public void testExists() {
    byte[] key = new byte[]{};
    rf.exists(key);
    verify(jedis).exists(key);
  }

  @Test
  public void testSmembers() {
    byte[] key = new byte[]{};
    rf.smembers(key);
    verify(jedis).smembers(key);
  }

  @Test
  public void testSpop() {
    byte[] key = new byte[]{};
    long count = 1;
    rf.spop(key, count);
    verify(jedis).spop(key, count);
  }

  @Test
  public void testExpireat() {
    byte[] key = new byte[]{};
    long time = 1;
    rf.expireAt(key, time);
    verify(jedis).expireAt(key, time);
  }

  @Test
  public void testZadd() {
    byte[] key = new byte[]{};
    byte[] value = new byte[]{};
    double score = 10;
    rf.zadd(key, score, value);
    verify(jedis).zadd(key, score, value);
  }

  @Test
  public void testZrem() {
    byte[] key = new byte[]{};
    byte[] field= new byte[]{};
    rf.zrem(key, field, field);
    verify(jedis).zrem(key, field, field);
  }

  @Test
  public void testZrangeByScore() {
    byte[] key = new byte[]{};
    double start = 1;
    double end = 2;
    rf.zrangeByScore(key, start, end);
    verify(jedis).zrangeByScore(key, start, end);
  }

  @Test
  public void testZrange() {
    byte[] key = new byte[]{};
    long start = 1;
    long end = 2;
    rf.zrange(key, start, end);
    verify(jedis).zrange(key, start, end);
  }

  @Test
  public void testPersist() {
    byte[] key = new byte[]{};
    rf.persist(key);
    verify(jedis).persist(key);
  }

  @Test
  public void testTransaction() {
    byte[] key = new byte[]{};
    Transaction jedisTransaction = mock(Transaction.class);
    when(jedis.multi()).thenReturn(jedisTransaction);
    RedisTransaction<?> transaction = mock(RedisTransaction.class);
    rf.transaction(key, transaction);
    verify(jedis).multi();
    verify(transaction).run(jedisTransaction);
    verify(jedisTransaction).exec();
  }

  @Test
  public void testSupportsMultiPopV999_999_999() {
    when(jedis.info("server")).thenReturn("# Server\r\nredis_version:999.999.999\r\nredis_git_sha1:ceaf58df\r\nredis_git_dirty:1");
    assertTrue(rf.supportsMultiSpop());
  }

  @Test
  public void testSupportsMultiPopV3_0_0() {
    when(jedis.info("server")).thenReturn("# Server\r\nredis_version:3.0.0\r\nredis_git_sha1:ceaf58df\r\nredis_git_dirty:1");
    assertFalse(rf.supportsMultiSpop());
  }

  @Test
  public void testSupportsMultiPopV3_2_0() {
    when(jedis.info("server")).thenReturn("# Server\r\nredis_version:3.2.0\r\nredis_git_sha1:ceaf58df\r\nredis_git_dirty:1");
    assertTrue(rf.supportsMultiSpop());
  }

  @Test
  public void testSupportsMultiPopVAbsent() {
    when(jedis.info("server")).thenReturn("");
    assertFalse(rf.supportsMultiSpop());
  }

  @Test
  public void testRename() {
    byte[] oldkey = new byte[]{};
    byte[] newkey = new byte[]{};
    rf.rename(oldkey, newkey);
    verify(jedis).rename(oldkey, newkey);
  }

  @Test
  public void testPsubscribe() {
    BinaryJedisPubSub listener = mock(BinaryJedisPubSub.class);
    String pattern = "test";
    rf.psubscribe(listener, pattern);
    verify(jedis).psubscribe(listener, SafeEncoder.encode(pattern));
  }

  @Test
  public void testClose() {
    rf.close();
    verify(pool).close();
  }

  @Test
  public void testJedisRetrieval() {
    rf.jedis();
    verify(pool).getResource();
    rf.jedis();
    verify(pool).getResource();
    // Now check that if we re-use jedis after requestFinish, new one
    // is retrieved.
    rf.requestFinished();
    rf.jedis();
    verify(pool, times(2)).getResource();
  }

  @Test
  public void testRequestFinished() {
    rf.requestFinished();
    verify(jedis, never()).close();
    rf.jedis();
    rf.requestFinished();
    verify(jedis).close();
  }

  @Test
  public void testMetrics() {
    MetricRegistry metrics = mock(MetricRegistry.class);
    Client client = mock(Client.class);
    when(client.getHost()).thenReturn("myhost");
    when(jedis.getClient()).thenReturn(client);
    when(pool.getNumActive()).thenReturn(1);
    when(pool.getNumIdle()).thenReturn(2);
    when(pool.getNumWaiters()).thenReturn(3);
    rf.startMonitoring(metrics);
    @SuppressWarnings("rawtypes")
    ArgumentCaptor<Gauge> gauge = ArgumentCaptor.forClass(Gauge.class);
    verify(metrics).register(eq("com.amadeus.session.redis.myhost.active"), gauge.capture());
    verify(metrics).register(eq("com.amadeus.session.redis.myhost.idle"), gauge.capture());
    verify(metrics).register(eq("com.amadeus.session.redis.myhost.waiting"), gauge.capture());
    assertEquals(1, gauge.getAllValues().get(0).getValue());
    assertEquals(2, gauge.getAllValues().get(1).getValue());
    assertEquals(3, gauge.getAllValues().get(2).getValue());
  }
}
