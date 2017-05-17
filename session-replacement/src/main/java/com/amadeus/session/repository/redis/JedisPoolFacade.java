package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

/**
 * This class hides difference of APIs between {@link Jedis} and
 * {@link JedisCluster}. The implementation offers subset of
 * {@link BinaryJedisCommands}.
 */
class JedisPoolFacade extends AbstractJedisFacade {
  private final Pool<Jedis> jedisPool;
  private final ThreadLocal<Jedis> currentJedis = new ThreadLocal<>();

  /**
   * Creates RedisFacade from {@link Pool} of {@link Jedis} connections.
   *
   * @param jedisPool
   *          pool of jedis connections
   */
  JedisPoolFacade(Pool<Jedis> jedisPool) {
    this.jedisPool = jedisPool;
  }

  /**
   * Associates jedis connection with current thread.
   *
   * @return jedis connection associated with current thread.
   */
  Jedis jedis() {
    Jedis jedis = currentJedis.get();
    if (jedis == null) {
      jedis = jedisPool.getResource();
      currentJedis.set(jedis);
    }
    return jedis;
  }

  @Override
  public void requestFinished() {
    Jedis jedis = currentJedis.get();
    if (jedis != null) {
      currentJedis.set(null);
      jedis.close();
    }
  }

  @Override
  public void psubscribe(final RedisPubSub listener, String pattern) {
    BinaryJedisPubSub bps = new BinaryJedisPubSub() {
      @Override
      public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        listener.onPMessage(pattern, channel, message);
      }
    };
    listener.link(bps);
    jedis().psubscribe(bps, encode(pattern));
  }

  @Override
  public void punsubscribe(final RedisPubSub listener, byte[] pattern) {
    ((BinaryJedisPubSub) listener.getLinked()).punsubscribe(pattern);
  }

  @Override
  public Long hdel(byte[] key, byte[]... fields) {
    return jedis().hdel(key, fields);
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return jedis().hmget(key, fields);
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return jedis().hmset(key, hash);
  }

  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    return jedis().hsetnx(key, field, value);
  }

  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    return jedis().hset(key, field, value);
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return jedis().hkeys(key);
  }

  @Override
  public String set(byte[] key, byte[] value) {
    return jedis().set(key, value);
  }

  @Override
  public String setex(byte[] key, int expiry, byte[] value) {
    return jedis().setex(key, expiry, value);
  }

  @Override
  public Long expire(byte[] key, int value) {
    return jedis().expire(key, value);
  }

  @Override
  public void srem(byte[] key, byte[]... member) {
    jedis().srem(key, member);
  }

  @Override
  public Long sadd(byte[] key, byte[]... member) {
    return jedis().sadd(key, member);
  }

  @Override
  public Long del(byte[]... keys) {
    return jedis().del(keys);
  }

  @Override
  public Boolean exists(byte[] key) {
    return jedis().exists(key);
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    return jedis().smembers(key);
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    return jedis().spop(key, count);
  }

  @Override
  public Long expireAt(byte[] key, long unixTime) {
    return jedis().expireAt(key, unixTime);
  }

  @Override
  public Long zadd(byte[] key, double score, byte[] elem) {
    return jedis().zadd(key, score, elem);
  }

  @Override
  public Long zrem(byte[] key, byte[]... fields) {
    return jedis().zrem(key, fields);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double start, double end) {
    return jedis().zrangeByScore(key, start, end);
  }

  @Override
  public Set<byte[]> zrange(byte[] key, long start, long end) {
    return jedis().zrange(key, start, end);
  }

  @Override
  public Long persist(byte[] key) {
    return jedis().persist(key);
  }

  @Override
  public String info(String section) {
    return jedis().info(section);
  }

  @Override
  public <T> RedisFacade.ResponseFacade<T> transaction(final byte[] key, final TransactionRunner<T> transaction) {
    final Transaction t = jedis().multi();
    RedisFacade.ResponseFacade<T> response = transaction.run(wrapJedisTransaction(t));
    t.exec();
    return response;
  }

  @Override
  public void close() {
    jedisPool.close();
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    return jedis().rename(oldkey, newkey);
  }

  @Override
  public byte[] get(byte[] key) {
    return jedis().get(key);
  }

  @Override
  public Long publish(byte[] channel, byte[] message) {
    return jedis().publish(channel, message);
  }

  @Override
  public void startMonitoring(MetricRegistry metrics) {
    AbstractJedisFacade.addMetrics(jedisPool, metrics);
  }
}
