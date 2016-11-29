package com.amadeus.session.repository.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 * This interface offers subset of {@link BinaryJedisCommands}. The subset
 * offers commands used by redis session repository implementation.
 */
interface RedisFacade {

  /**
   * See {@link MultiKeyBinaryCommands#psubscribe(BinaryJedisPubSub, byte[]...)}
   *
   * @param listener
   * @param pattern
   */
  void psubscribe(BinaryJedisPubSub listener, String pattern);

  /**
   * See {@link BinaryJedisCommands#hdel(byte[], byte[]...)}
   *
   * @param key
   * @param fields
   * @return
   */
  Long hdel(byte[] key, byte[]... fields);

  /**
   * See {@link BinaryJedisCommands#hmget(byte[], byte[]...)}
   *
   * @param key
   * @param fields
   *
   * @return
   */
  List<byte[]> hmget(byte[] key, byte[]... fields);

  /**
   * See {@link BinaryJedisCommands#hmset(byte[], Map)}
   *
   * @param key
   * @param hash
   * @return
   */
  String hmset(byte[] key, Map<byte[], byte[]> hash);

  /**
   * See {@link BinaryJedisCommands#hsetnx(byte[], byte[], byte[])}
   *
   * @param key
   * @param field
   * @param value
   * @return
   */
  Long hsetnx(byte[] key, byte[] field, byte[] value);

  /**
   * See {@link BinaryJedisCommands#hset(byte[], byte[], byte[])}
   *
   * @param key
   * @param field
   * @param value
   * @return
   */
  Long hset(byte[] key, byte[] field, byte[] value);

  /**
   * See {@link BinaryJedisCommands#hkeys(byte[])}
   *
   * @param key
   * @return
   */
  Set<byte[]> hkeys(byte[] key);

  /**
   * See {@link BinaryJedisCommands#set(byte[], byte[])}
   *
   * @param key
   * @param value
   * @return
   */
  String set(byte[] key, byte[] value);

  /**
   * See {@link BinaryJedisCommands#setx(byte[], int, byte[])}
   *
   * @param key
   * @param expiry
   * @param value
   * @return
   */
  String setex(byte[] key, int expiry, byte[] value);

  /**
   * See {@link BinaryJedisCommands#expire(byte[], int)}
   *
   * @param key
   * @param value
   * @return
   */
  Long expire(byte[] key, int value);

  /**
   * See {@link BinaryJedisCommands#srem(byte[], byte[]...)}
   *
   * @param key
   * @param member
   */
  void srem(byte[] key, byte[]... member);

  /**
   * See {@link BinaryJedisCommands#sadd(byte[], byte[]...)}
   *
   * @param key
   * @param member
   * @return
   */
  Long sadd(byte[] key, byte[]... member);

  /**
   * See {@link BinaryJedisCommands#del(byte[]...)}
   *
   * @param keys
   * @return
   */
  Long del(byte[]... keys);

  /**
   * See {@link BinaryJedisCommands#exists(byte[])}
   *
   * @param key
   * @return
   */
  Boolean exists(byte[] key);

  /**
   * See {@link BinaryJedisCommands#smembers(byte[])}
   *
   * @param key
   * @return
   */
  Set<byte[]> smembers(byte[] key);

  /**
   * See {@link BinaryJedisCommands#spop(byte[], long)}
   *
   * @param key
   * @param count
   * @return
   */
  Set<byte[]> spop(byte[] key, long count);

  /**
   * See {@link BinaryJedisCommands#expireAt(byte[], long)}
   *
   * @param key
   * @param unixTime
   * @return
   */
  Long expireAt(byte[] key, long unixTime);

  /**
   * See {@link BinaryJedisCommands#zadd(byte[], byte[]...)}
   *
   * @param key
   * @param score
   * @param elem
   * @return
   */
  Long zadd(byte[] key, double score, byte[] elem);

  /**
   * See {@link BinaryJedisCommands#zrem(byte[], byte[]...)}
   *
   * @param key
   * @param fields
   * @return
   */
  Long zrem(byte[] key, byte[]... fields);

  /**
   * See {@link BinaryJedisCommands#zrangeByScore(byte[], double, double)}
   *
   * @param key
   * @param start
   * @param end
   * @return
   */
  Set<byte[]> zrangeByScore(byte[] key, double start, double end);

  /**
   * See {@link BinaryJedisCommands#zrange(byte[], long, long)}
   *
   * @param key
   * @param start
   * @param end
   * @return
   */
  Set<byte[]> zrange(byte[] key, long start, long end);

  /**
   * See {@link BinaryJedisCommands#persist(byte[])}
   *
   * @param key
   * @return
   */
  Long persist(byte[] key);

  /**
   * See {@link BinaryJedisCommands#rename(byte[], byte[])}
   *
   * @param oldkey
   * @param newkey
   * @return
   */
  String rename(byte[] oldkey, byte[] newkey);

  /**
   * Returns information about server
   *
   * @param section
   * @return
   */
  String info(String section);

  /**
   * Executes redis transaction associated to a key. When using cluster, key
   * must be provided as the transaction is linked to a single slot.
   *
   * @param key
   *          used when performing transaction on cluster to fix the node where
   *          transaction occurs.
   * @param transaction
   *          the implementation of transaction
   * @return result of transaction
   */
  <T> Response<T> transaction(byte[] key, RedisTransaction<T> transaction);

  /**
   * Returns true if redis implementation supports SPOP that returns multiple
   * elements (http://redis.io/commands/spop). This is command is supported in
   * redis v3.2 and later.
   *
   * @return <code>true</code> if SPOP with multiple results is supported
   */
  boolean supportsMultiSpop();

  /**
   * Implementation of this interface can be run within a redis transaction. The
   * transaction is sequence of redis commands executed atomically.
   * <p>
   * See <a href="http://redis.io/topics/transactions">http://redis.io/topics/
   * transactions</a> for details about transactions.
   *
   * @param <T>
   *          result type of the transaction
   */
  interface RedisTransaction<T> {
    /**
     * Runs transaction and returns its result.
     *
     * @param transaction
     *          redis transaction to run
     * @return result of transaction
     */
    Response<T> run(Transaction transaction);
  }

  /**
   * Dissociates jedis connection from the current thread and returns it to the
   * pool.
   */
  void requestFinished();

  /**
   * Closes connections to redis.
   */
  void close();

  /**
   * See {@link BinaryJedisCommands#get(byte[])}
   *
   * @param key
   * @return
   */
  byte[] get(byte[] key);

  /**
   * See {@link BinaryJedisCommands#publish(byte[], byte[])}
   *
   * @param channel
   * @param message
   * @return
   */
  Long publish(byte[] channel, byte[] message);

  /**
   * Starts monitoring Redis interaction. This method
   * can be used to add metrics to metric registry.
   * @param metrics
   */
  void startMonitoring(MetricRegistry metrics);
}