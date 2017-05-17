package com.amadeus.session.repository.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;

/**
 * This interface offers subset of redis.clients.jedis.BinaryJedisCommands. The subset offers commands used by redis
 * session repository implementation.
 */
public interface RedisFacade {

  /**
   * See BinaryJedisCommands#psubscribe(BinaryJedisPubSub, byte[]...)
   *
   * @param listener
   * @param pattern
   */
  void psubscribe(RedisPubSub listener, String pattern);

  /**
   * See BinaryJedisPubSub#punsubscribe(byte[]...)
   *
   * @param expirationListener
   * @param pattern
   */
  void punsubscribe(RedisPubSub expirationListener, byte[] pattern);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hdel(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param fields
   * @return If the field was present in the hash it is deleted and 1 is returned, otherwise 0 is returned and no
   *         operation is performed.
   */
  Long hdel(byte[] key, byte[]... fields);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hmget(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param fields
   *
   * @return list of retrieved values
   */
  List<byte[]> hmget(byte[] key, byte[]... fields);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hmset(byte[], Map)
   *
   * @param key
   *          key as byte array
   * @param hash
   *          map of fields to modify
   * @return status of the operation
   */
  String hmset(byte[] key, Map<byte[], byte[]> hash);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hsetnx(byte[], byte[], byte[])
   *
   * @param key
   *          key as byte array
   * @param field
   *          field to modify
   * @param value
   *          value of the field
   * @return status of the operation
   */
  Long hsetnx(byte[] key, byte[] field, byte[] value);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hset(byte[], byte[], byte[])
   *
   * @param key
   *          key as byte array
   * @param field
   * @param value
   * @return status of the operation
   */
  Long hset(byte[] key, byte[] field, byte[] value);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#hkeys(byte[])
   *
   * @param key
   *          key as byte array
   * @return
   */
  Set<byte[]> hkeys(byte[] key);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#set(byte[], byte[])
   *
   * @param key
   *          key as byte array
   * @param value
   * @return
   */
  String set(byte[] key, byte[] value);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#setx(byte[], int, byte[])
   *
   * @param key
   *          key as byte array
   * @param expiry
   * @param value
   * @return
   */
  String setex(byte[] key, int expiry, byte[] value);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#expire(byte[], int)
   *
   * @param key
   *          key as byte array
   * @param value
   * @return
   */
  Long expire(byte[] key, int value);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#srem(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param member
   */
  void srem(byte[] key, byte[]... member);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#sadd(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param member
   * @return
   */
  Long sadd(byte[] key, byte[]... member);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#del(byte[]...)
   *
   * @param keys
   *          key as byte arrays
   * @return
   */
  Long del(byte[]... keys);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#exists(byte[])
   *
   * @param key
   *          key as byte array
   * @return
   */
  Boolean exists(byte[] key);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#smembers(byte[])
   *
   * @param key
   *          key as byte array
   * @return
   */
  Set<byte[]> smembers(byte[] key);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#spop(byte[], long)
   *
   * @param key
   *          key as byte array
   * @param count
   * @return
   */
  Set<byte[]> spop(byte[] key, long count);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#expireAt(byte[], long)
   *
   * @param key
   *          key as byte array
   * @param unixTime
   * @return
   */
  Long expireAt(byte[] key, long unixTime);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#zadd(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param score
   * @param elem
   * @return
   */
  Long zadd(byte[] key, double score, byte[] elem);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#zrem(byte[], byte[]...)
   *
   * @param key
   *          key as byte array
   * @param fields
   * @return
   */
  Long zrem(byte[] key, byte[]... fields);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#zrangeByScore(byte[], double, double)
   *
   * @param key
   *          key as byte array
   * @param start
   * @param end
   * @return
   */
  Set<byte[]> zrangeByScore(byte[] key, double start, double end);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#zrange(byte[], long, long)
   *
   * @param key
   *          key as byte array
   * @param start
   * @param end
   * @return
   */
  Set<byte[]> zrange(byte[] key, long start, long end);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#persist(byte[])
   *
   * @param key
   *          key as byte array
   * @return
   */
  Long persist(byte[] key);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#rename(byte[], byte[])
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
   * Returns true if redis implementation supports SPOP that returns multiple elements (http://redis.io/commands/spop).
   * This is command is supported in redis v3.2 and later.
   *
   * @return <code>true</code> if SPOP with multiple results is supported
   */
  boolean supportsMultiSpop();

  /**
   * Dissociates jedis connection from the current thread and returns it to the pool.
   */
  void requestFinished();

  /**
   * Closes connections to redis.
   */
  void close();

  /**
   * See redis.clients.jedis.BinaryJedisCommands#get(byte[])
   *
   * @param key
   *          key as byte array
   * @return
   */
  byte[] get(byte[] key);

  /**
   * See redis.clients.jedis.BinaryJedisCommands#publish(byte[], byte[])
   *
   * @param channel
   * @param message
   * @return
   */
  Long publish(byte[] channel, byte[] message);

  /**
   * Starts monitoring Redis interaction. This method can be used to add metrics to metric registry.
   *
   * @param metrics
   */
  void startMonitoring(MetricRegistry metrics);

  /**
   * Returns <code>true</code> if exception was thrown by redis library
   *
   * @param e
   *          the exception to check
   * @return <code>true</code> if exception was thrown by redis library
   */
  boolean isRedisException(Exception e);

  /**
   * Executes redis transaction associated to a key. When using cluster, key must be provided as the transaction is
   * linked to a single slot.
   *
   * @param key
   *          key as byte array used when performing transaction on cluster to fix the node where transaction occurs.
   * @param transaction
   *          the implementation of transaction
   * @return result of transaction
   */
  <T> ResponseFacade<T> transaction(byte[] key, TransactionRunner<T> transaction);

  /**
   * Used to wrap implementation's response for transactions.
   *
   * @param <T>
   *          the result type
   */
  public interface ResponseFacade<T> {

    T get();
  }

  /**
   * Implementation of this interface can be run within a redis transaction. The transaction is sequence of redis
   * commands executed atomically.
   * <p>
   * See <a href="http://redis.io/topics/transactions">http://redis.io/topics/ transactions</a> for details about
   * transactions.
   *
   * @param <T>
   *          result type of the transaction
   */
  interface TransactionRunner<T> {

    /**
     * Runs transaction and returns its result.
     *
     * @param transactionImpl
     *          underlying redis transaction to run
     * @return result of transaction
     */
    ResponseFacade<T> run(TransactionFacade transactionImpl);
  }

  /**
   * Used to encapsulate redis library's transaction (MULTI) processing.
   *
   */
  interface TransactionFacade {

    /**
     * See redis.clients.jedis.Transaction#hdel(byte[], byte[]...)
     *
     * @param key
     *          key as byte array
     * @param fields
     */
    void hdel(byte[] key, byte[]... fields);

    /**
     * See redis.clients.jedis.Transaction#hmset(byte[], Map)
     *
     * @param key
     *          key as byte array
     * @param hash
     */
    void hmset(byte[] key, Map<byte[], byte[]> hash);

    /**
     * See redis.clients.jedis.Transaction#del(byte[]...)
     *
     * @param keys
     *          key as byte arrays
     */
    void del(byte[]... keys);

    /**
     * See redis.clients.jedis.Transaction#smembers(byte[])
     *
     * @param key
     *          key as byte array
     * @return
     */
    RedisFacade.ResponseFacade<Set<byte[]>> smembers(byte[] key);
  }

  /**
   * Used to implement PUBSUB mechanism.
   */
  interface RedisPubSub {

    void onPMessage(byte[] pattern, byte[] channelBuf, byte[] message);

    /**
     * Returns linked underlying PubSub listener implementation.
     *
     * @return underlying PubSub listener implementation
     */
    Object getLinked();

    /**
     * Links with underlying PubSub listener implementation.
     *
     * @param linkedImplementation
     *          underlying PubSub listener implementation
     */
    void link(Object linkedImplementation);
  }
}