package com.amadeus.session.repository.redis;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

/**
 * Base class for jedis facades. Contains methods that are common for both
 * single/sentinel node facade and cluster facade.
 *
 */
abstract class AbstractJedisFacade implements RedisFacade {
  private static final String CRLF = "\r\n";
  private static final String REDIS_VERSION_LABEL = "redis_version:";
  private static final Integer[] MIN_MULTISPOP_VERSION = new Integer[] { 3, 2 };

  private List<Integer> version;

  /**
   * Multi spop is only supported redis 3.2+.
   */
  @Override
  public boolean supportsMultiSpop() {
    readVersion();
    for (int i = 0; i < MIN_MULTISPOP_VERSION.length && i < version.size(); i++) {
      if (version.get(i) < MIN_MULTISPOP_VERSION[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reads and parses version information from redis server. See
   * http://redis.io/commands/INFO for details how to obtain redis version.
   */
  void readVersion() {
    if (version == null) {
      String info = info("server");
      if (info != null) {
        int start = info.indexOf(REDIS_VERSION_LABEL);
        if (start >= 0) {
          start += REDIS_VERSION_LABEL.length();
          // In RESP different parts of the protocol are always terminated with
          // "\r\n" (CRLF).
          int end = info.indexOf(CRLF, start);
          if (end < 0) {
            end = info.length();
          }
          String[] coordiantes = info.substring(start, end).split("\\.");
          version = new ArrayList<>();
          for (String coordinate : coordiantes) {
            version.add(Integer.parseInt(coordinate));
          }
        }
      }
      if (version == null) {
        version = Collections.singletonList(0);
      }
    }
  }

  @Override
  public boolean isRedisException(Exception e) {
    return e instanceof JedisException;
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
    String prefix = name(RedisConfiguration.METRIC_PREFIX, "redis", host);
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

  /**
   * Wraps jedis transaction into TransactionFacade.
   *
   * @param t
   *          jedis transaction
   * @return wrapped instance
   */
  static RedisFacade.TransactionFacade wrapJedisTransaction(final Transaction t) {
    return new RedisFacade.TransactionFacade() {
      @Override
      public void hdel(byte[] key, byte[]... fields) {
        t.hdel(key, fields);
      }

      @Override
      public void hmset(byte[] key, Map<byte[], byte[]> hash) {
        t.hmset(key, hash);
      }

      @Override
      public void del(byte[]... keys) {
        t.del(keys);
      }

      @Override
      public RedisFacade.ResponseFacade<Set<byte[]>> smembers(final byte[] key) {
        return new RedisFacade.ResponseFacade<Set<byte[]>>() {
          @Override
          public Set<byte[]> get() {
            return t.smembers(key).get();
          }

        };
      }
    };
  }
}