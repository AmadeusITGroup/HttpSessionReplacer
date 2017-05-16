package com.amadeus.session.repository.redis;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amadeus.session.repository.redis.RedisFacade.TransactionRunner;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Tuple;

@SuppressWarnings("javadoc")
public class TestJedisClusterFacade {
  private TransactionalJedisCluster jedisCluster;
  private JedisClusterFacade rf;

  @Before
  public void setup() {
    jedisCluster = mock(TransactionalJedisCluster.class);
    rf = new JedisClusterFacade(jedisCluster);

  }

  @Test
  public void testHdel() {
    byte[] key = new byte[] {};
    byte[] fields = new byte[] {};
    rf.hdel(key, fields);
    verify(jedisCluster).hdel(key, fields);
  }

  @Test
  public void testHmget() {
    byte[] key = new byte[] {};
    byte[] fields = new byte[] {};
    rf.hmget(key, fields);
    verify(jedisCluster).hmget(key, fields);
  }

  @Test
  public void testHmset() {
    byte[] key = new byte[] {};
    @SuppressWarnings("unchecked")
    Map<byte[], byte[]> fields = mock(Map.class);
    rf.hmset(key, fields);
    verify(jedisCluster).hmset(key, fields);
  }

  @Test
  public void testHsetnx() {
    byte[] key = new byte[] {};
    byte[] fields = new byte[] {};
    byte[] values = new byte[] {};
    rf.hsetnx(key, fields, values);
    verify(jedisCluster).hsetnx(key, fields, values);
  }

  @Test
  public void testHkeys() {
    byte[] key = new byte[] {};
    rf.hkeys(key);
    verify(jedisCluster).hkeys(key);
  }

  @Test
  public void testSet() {
    byte[] key = new byte[] {};
    byte[] value = new byte[] {};
    rf.set(key, value);
    verify(jedisCluster).set(key, value);
  }

  @Test
  public void testSetex() {
    byte[] key = new byte[] {};
    byte[] value = new byte[] {};
    int expiry = 10;
    rf.setex(key, expiry, value);
    verify(jedisCluster).setex(key, expiry, value);
  }

  @Test
  public void testExpire() {
    byte[] key = new byte[] {};
    int value = 1;
    rf.expire(key, value);
    verify(jedisCluster).expire(key, 1);
  }

  @Test
  public void testSrem() {
    byte[] key = new byte[] {};
    byte[] value = new byte[] {};
    rf.srem(key, value);
    verify(jedisCluster).srem(key, value);
  }

  @Test
  public void testSadd() {
    byte[] key = new byte[] {};
    byte[] value = new byte[] {};
    rf.sadd(key, value);
    verify(jedisCluster).sadd(key, value);
  }

  @Test
  public void testDel() {
    byte[] key = new byte[] {};
    rf.del(key, key);
    verify(jedisCluster).del(key, key);
  }

  @Test
  public void testExists() {
    byte[] key = new byte[] {};
    rf.exists(key);
    verify(jedisCluster).exists(key);
  }

  @Test
  public void testSmembers() {
    byte[] key = new byte[] {};
    rf.smembers(key);
    verify(jedisCluster).smembers(key);
  }

  @Test
  public void testSpop() {
    byte[] key = new byte[] {};
    long count = 1;
    rf.spop(key, count);
    verify(jedisCluster).spop(key, count);
  }

  @Test
  public void testExpireat() {
    byte[] key = new byte[] {};
    long time = 1;
    rf.expireAt(key, time);
    verify(jedisCluster).expireAt(key, time);
  }

  @Test
  public void testZadd() {
    byte[] key = new byte[] {};
    byte[] value = new byte[] {};
    double score = 10;
    rf.zadd(key, score, value);
    verify(jedisCluster).zadd(key, score, value);
  }

  @Test
  public void testZrem() {
    byte[] key = new byte[] {};
    byte[] field = new byte[] {};
    rf.zrem(key, field, field);
    verify(jedisCluster).zrem(key, field, field);
  }

  @Test
  public void testZrangeByScore() {
    byte[] key = new byte[] {};
    double start = 1;
    double end = 2;
    rf.zrangeByScore(key, start, end);
    verify(jedisCluster).zrangeByScore(key, start, end);
  }

  @Test
  public void testZrange() {
    byte[] key = new byte[] {};
    long start = 1;
    long end = 2;
    rf.zrange(key, start, end);
    verify(jedisCluster).zrange(key, start, end);
  }

  @Test
  public void testPersist() {
    byte[] key = new byte[] {};
    rf.persist(key);
    verify(jedisCluster).persist(key);
  }

  @Test
  public void testTransaction() {
    byte[] key = new byte[] {};
    TransactionRunner<?> transaction = mock(TransactionRunner.class);
    rf.setTransactionOnKey(true);
    rf.transaction(key, transaction);
    verify(jedisCluster).transaction(key, transaction);
  }


  @Test
  public void testTransactionAsSequence() {
    byte[] key = new byte[] {};
    TransactionRunner<?> transaction = mock(TransactionRunner.class);
    rf.transaction(key, transaction);
    verify(jedisCluster, never()).transaction(key, transaction);
    verify(jedisCluster).transaction(transaction);
  }
  
  @Test
  public void testRenameString() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 66 };
    byte[] value = new byte[] { 67 };

    when(jedisCluster.type(any(byte[].class))).thenReturn("string");
    when(jedisCluster.get(any(byte[].class))).thenReturn(value);
    rf.rename(oldkey, newkey);
    verify(jedisCluster, never()).rename(oldkey, newkey);
    verify(jedisCluster).type(oldkey);
    verify(jedisCluster).get(oldkey);
    verify(jedisCluster).set(newkey, value);
    verify(jedisCluster).del(oldkey);
  }

  @Test
  public void testRenameHash() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 66 };
    Map<byte[], byte[]> value = new HashMap<>();

    when(jedisCluster.type(any(byte[].class))).thenReturn("hash");
    when(jedisCluster.hgetAll(any(byte[].class))).thenReturn(value);
    rf.rename(oldkey, newkey);
    verify(jedisCluster, never()).rename(oldkey, newkey);
    verify(jedisCluster).type(oldkey);
    verify(jedisCluster).hgetAll(oldkey);
    verify(jedisCluster).hmset(newkey, value);
    verify(jedisCluster).del(oldkey);
  }

  @Test
  public void testRenameList() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 66 };
    List<byte[]> value = new ArrayList<>();

    when(jedisCluster.type(any(byte[].class))).thenReturn("list");
    when(jedisCluster.lrange(oldkey, 0, -1)).thenReturn(value);
    rf.rename(oldkey, newkey);
    verify(jedisCluster, never()).rename(oldkey, newkey);
    verify(jedisCluster).type(oldkey);
    verify(jedisCluster).lrange(oldkey, 0, -1);
    verify(jedisCluster).del(oldkey);
  }

  @Test
  public void testRenameSet() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 66 };
    Set<byte[]> value = new HashSet<>();

    when(jedisCluster.type(any(byte[].class))).thenReturn("set");
    when(jedisCluster.smembers(oldkey)).thenReturn(value);
    rf.rename(oldkey, newkey);
    verify(jedisCluster, never()).rename(oldkey, newkey);
    verify(jedisCluster).type(oldkey);
    verify(jedisCluster).smembers(oldkey);
    verify(jedisCluster).del(oldkey);
  }

  @Test
  public void testRenameZrange() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 66 };
    Set<Tuple> value = new HashSet<>();

    when(jedisCluster.type(any(byte[].class))).thenReturn("zrange");
    when(jedisCluster.zrangeWithScores(oldkey, 0, -1)).thenReturn(value);
    rf.rename(oldkey, newkey);
    verify(jedisCluster, never()).rename(oldkey, newkey);
    verify(jedisCluster).type(oldkey);
    verify(jedisCluster).zrangeWithScores(oldkey, 0, -1);
    verify(jedisCluster).del(oldkey);
  }

  @Test
  public void testRenameSameSlot() {
    byte[] oldkey = new byte[] { 65 };
    byte[] newkey = new byte[] { 65 };

    rf.rename(oldkey, newkey);
    verify(jedisCluster).rename(oldkey, newkey);
  }

  @Test
  public void testHset() {
    byte[] key = new byte[]{};
    byte[] field = new byte[]{};
    byte[] value = new byte[]{};
    rf.hset(key, field, value);
    verify(jedisCluster).hset(key, field, value);
  }

  @Test
  public void testPsubscribe() {
    BinaryJedisPubSub listener = mock(BinaryJedisPubSub.class);
    RedisFacade.RedisPubSub facadeListener = mock(RedisFacade.RedisPubSub.class);
    when(facadeListener.getLinked()).thenReturn(listener);
    String pattern = "test";
    rf.psubscribe(facadeListener, pattern);
    ArgumentCaptor<BinaryJedisPubSub> capture = ArgumentCaptor.forClass(BinaryJedisPubSub.class);
    verify(facadeListener).link(capture.capture());
    verify(jedisCluster).psubscribe(capture.getValue(), SafeEncoder.encode(pattern));
  }

  @Test
  public void testGet() {
    byte[] key = new byte[]{};
    rf.get(key);
    verify(jedisCluster).get(key);
  }

  @Test
  public void testPublish() {
    byte[] channel = new byte[]{};
    byte[] message = new byte[]{};
    rf.publish(channel, message);
    verify(jedisCluster).publish(channel, message);
  }

  @Test
  public void testClose() throws IOException {
    rf.close();
    verify(jedisCluster).close();
  }
}
