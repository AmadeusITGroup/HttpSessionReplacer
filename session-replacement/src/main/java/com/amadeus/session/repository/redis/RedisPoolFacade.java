package com.amadeus.session.repository.redis;

import static com.codahale.metrics.MetricRegistry.name;
import static redis.clients.util.SafeEncoder.encode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

/**
 * This class hides difference of APIs between {@link Jedis} and
 * {@link JedisCluster}. The implementation offers subset of
 * {@link BinaryJedisCommands}.
 */
class RedisPoolFacade extends AbstractRedisFacade implements RedisFacade {
  private final Pool<Jedis> jedisPool;
  private final ThreadLocal<Jedis> currentJedis = new ThreadLocal<>();

  /**
   * Creates RedisFacade from {@link Pool} of {@link Jedis} connections.
   *
   * @param jedisPool
   *          pool of jedis connections
   */
  RedisPoolFacade(Pool<Jedis> jedisPool) {
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

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.repository.redis.IRedisFacade#psubscribe(redis.clients.
   * jedis.BinaryJedisPubSub, java.lang.String)
   */
  @Override
  public void psubscribe(BinaryJedisPubSub listener, String pattern) {
    jedis().psubscribe(listener, encode(pattern));
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hdel(byte[], byte)
   */
  @Override
  public Long hdel(byte[] key, byte[]... fields) {
    return jedis().hdel(key, fields);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hmget(byte[], byte)
   */
  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return jedis().hmget(key, fields);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hmset(byte[],
   * java.util.Map)
   */
  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return jedis().hmset(key, hash);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hsetnx(byte[],
   * byte[], byte[])
   */
  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    return jedis().hsetnx(key, field, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hset(byte[], byte[],
   * byte[])
   */
  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    return jedis().hset(key, field, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#hkeys(byte[])
   */
  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return jedis().hkeys(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#set(byte[], byte[])
   */
  @Override
  public String set(byte[] key, byte[] value) {
    return jedis().set(key, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#setex(byte[], int,
   * byte[])
   */
  @Override
  public String setex(byte[] key, int expiry, byte[] value) {
    return jedis().setex(key, expiry, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#expire(byte[], int)
   */
  @Override
  public Long expire(byte[] key, int value) {
    return jedis().expire(key, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#srem(byte[], byte)
   */
  @Override
  public void srem(byte[] key, byte[]... member) {
    jedis().srem(key, member);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#sadd(byte[], byte)
   */
  @Override
  public Long sadd(byte[] key, byte[]... member) {
    return jedis().sadd(key, member);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#del(byte)
   */
  @Override
  public Long del(byte[]... keys) {
    return jedis().del(keys);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#exists(byte[])
   */
  @Override
  public Boolean exists(byte[] key) {
    return jedis().exists(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#smembers(byte[])
   */
  @Override
  public Set<byte[]> smembers(byte[] key) {
    return jedis().smembers(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#spop(byte[], long)
   */
  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    return jedis().spop(key, count);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#expireAt(byte[],
   * long)
   */
  @Override
  public Long expireAt(byte[] key, long unixTime) {
    return jedis().expireAt(key, unixTime);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#zadd(byte[], double,
   * byte[])
   */
  @Override
  public Long zadd(byte[] key, double score, byte[] elem) {
    return jedis().zadd(key, score, elem);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#zrem(byte[], byte)
   */
  @Override
  public Long zrem(byte[] key, byte[]... fields) {
    return jedis().zrem(key, fields);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.repository.redis.IRedisFacade#zrangeByScore(byte[],
   * double, double)
   */
  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double start, double end) {
    return jedis().zrangeByScore(key, start, end);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#zrange(byte[], long,
   * long)
   */
  @Override
  public Set<byte[]> zrange(byte[] key, long start, long end) {
    return jedis().zrange(key, start, end);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#persist(byte[])
   */
  @Override
  public Long persist(byte[] key) {
    return jedis().persist(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.repository.redis.IRedisFacade#info(java.lang.String)
   */
  @Override
  public String info(String section) {
    return jedis().info(section);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#transaction(byte[],
   * com.amadeus.session.repository.redis.RedisFacade.RedisTransaction)
   */
  @Override
  public <T> Response<T> transaction(final byte[] key, final RedisTransaction<T> transaction) {
    Transaction t = jedis().multi();
    Response<T> response = transaction.run(t);
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
    addMetrics(jedisPool, metrics);
  }

  /**
   * Helper method that registers metrics for a jedis pool.
   *
   * @param jedisPool
   *          the pool which is monitored
   * @param metrics
   *          the registry to use for metrics
   */
  static void addMetrics(final Pool<Jedis> jedisPool, MetricRegistry metrics) {
    final String host = jedisPool.getResource().getClient().getHost();
    String prefix = name(METRIC_PREFIX, "redis", host);
    metrics.register(name(prefix, "active"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return jedisPool.getNumActive();
      }
    });
    metrics.register(name(prefix, "idle"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return jedisPool.getNumIdle();
      }
    });
    metrics.register(name(prefix, "waiting"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return jedisPool.getNumWaiters();
      }
    });
  }
}
