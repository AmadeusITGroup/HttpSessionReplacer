package com.amadeus.session.repository.redis;

import static com.amadeus.session.repository.redis.SafeEncoder.encode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amadeus.session.WrappedException;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.util.JedisClusterCRC16;

/**
 * This class acts as facade to {@link JedisCluster}. The implementation offers subset of {@link BinaryJedisCommands},
 * and, for methods that are not supported in cluster (e.g. rename), it also provides semantically similar
 * implementations.
 */
class JedisClusterFacade extends AbstractJedisFacade {
  private final TransactionalJedisCluster jedisCluster;
  private boolean transactionOnKey;

  /**
   * Creates RedisFacade from jedis cluster
   *
   * @param jedisCluster
   */
  JedisClusterFacade(TransactionalJedisCluster jedisCluster) {
    this.jedisCluster = jedisCluster;
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
    jedisCluster.psubscribe(bps, encode(pattern));
  }

  @Override
  public void punsubscribe(final RedisPubSub listener, byte[] pattern) {
    ((BinaryJedisPubSub) listener.getLinked()).punsubscribe(pattern);
  }

  @Override
  public Long hdel(byte[] key, byte[]... fields) {
    return jedisCluster.hdel(key, fields);
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return jedisCluster.hmget(key, fields);
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return jedisCluster.hmset(key, hash);
  }

  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    return jedisCluster.hsetnx(key, field, value);
  }

  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    return jedisCluster.hset(key, field, value);
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return jedisCluster.hkeys(key);
  }

  @Override
  public String set(byte[] key, byte[] value) {
    return jedisCluster.set(key, value);
  }

  @Override
  public String setex(byte[] key, int expiry, byte[] value) {
    return jedisCluster.setex(key, expiry, value);
  }

  @Override
  public Long expire(byte[] key, int value) {
    return jedisCluster.expire(key, value);
  }

  @Override
  public void srem(byte[] key, byte[]... member) {
    jedisCluster.srem(key, member);
    return;
  }

  @Override
  public Long sadd(byte[] key, byte[]... member) {
    return jedisCluster.sadd(key, member);
  }

  @Override
  public Long del(byte[]... keys) {
    return jedisCluster.del(keys);
  }

  @Override
  public Boolean exists(byte[] key) {
    return jedisCluster.exists(key);
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    return jedisCluster.smembers(key);
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {

    return jedisCluster.spop(key, count);
  }

  @Override
  public Long expireAt(byte[] key, long unixTime) {
    return jedisCluster.expireAt(key, unixTime);
  }

  @Override
  public Long zadd(byte[] key, double score, byte[] elem) {
    return jedisCluster.zadd(key, score, elem);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.repository.redis.IRedisFacade#zrem(byte[], byte)
   */
  @Override
  public Long zrem(byte[] key, byte[]... fields) {
    return jedisCluster.zrem(key, fields);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double start, double end) {
    return jedisCluster.zrangeByScore(key, start, end);
  }

  @Override
  public Set<byte[]> zrange(byte[] key, long start, long end) {
    return jedisCluster.zrange(key, start, end);
  }

  @Override
  public Long persist(byte[] key) {
    return jedisCluster.persist(key);
  }

  @Override
  public String info(String section) {
    return jedisCluster.info(section);
  }

  @Override
  public void close() {
    try {
      jedisCluster.close();
    } catch (IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void requestFinished() {
    // Do nothing
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    int slot = JedisClusterCRC16.getSlot(oldkey);
    int newSlot = JedisClusterCRC16.getSlot(newkey);
    // If slots are not same we need to apply move logic (get, set, delete).
    if (slot != newSlot) {
      return renameToDifferentSlots(oldkey, newkey);
    }
    return jedisCluster.rename(oldkey, newkey);
  }

  /**
   * Implements move of Redis elements where oldkey and newkey don't fall in same slot. The concrete implementation
   * depends on the type of data at oldkey.
   *
   * @param oldkey
   * @param newkey
   * @return
   */
  private String renameToDifferentSlots(byte[] oldkey, byte[] newkey) {
    String type = jedisCluster.type(oldkey);
    String result;
    switch (type) {
      case "string":
        result = renameString(oldkey, newkey);
        break;
      case "hash":
        result = renameHash(oldkey, newkey);
        break;
      case "set":
        result = renameSet(oldkey, newkey);
        break;
      case "list":
        result = renameList(oldkey, newkey);
        break;
      case "zrange":
        result = renameZRange(oldkey, newkey);
        break;
      default:
        throw new JedisClusterException("Unknown element type " + type + " for key " + encode(oldkey));
    }
    return result;
  }

  /**
   * Renames Redis set. As this operation may span multiple servers, we add set elements to new key, and then remove the
   * old key.
   *
   * @param oldkey
   *          the old set key
   * @param newkey
   *          the new key of the set
   * @return OK if succeeded, ERR otherwise - e.g. when oldkey doesn't exist
   */
  String renameSet(byte[] oldkey, byte[] newkey) {
    Set<byte[]> value = jedisCluster.smembers(oldkey);
    if (value == null) {
      return "ERR";
    }
    jedisCluster.sadd(newkey, new ArrayList<>(value).toArray(new byte[][] {}));
    jedisCluster.del(oldkey);
    return "OK";
  }

  /**
   * Renames Redis hash. As this operation may span multiple servers, we add hash elements to new key, and then remove
   * the old key.
   *
   * @param oldkey
   *          the old hash key
   * @param newkey
   *          the new key of the hash
   * @return OK if succeeded, ERR otherwise - e.g. when oldkey doesn't exist
   */
  String renameHash(byte[] oldkey, byte[] newkey) {
    Map<byte[], byte[]> value = jedisCluster.hgetAll(oldkey);
    if (value == null) {
      return "ERR";
    }
    jedisCluster.hmset(newkey, value);
    jedisCluster.del(oldkey);
    return "OK";
  }

  /**
   * Renames simple Redis key. As this operation may span multiple servers, we set newkey element to value of oldkey
   * element, then remove the old key.
   *
   * @param oldkey
   *          the old key
   * @param newkey
   *          the new key
   * @return OK if succeeded, ERR otherwise - e.g. when oldkey doesn't exist
   */
  String renameString(byte[] oldkey, byte[] newkey) {
    byte[] value = jedisCluster.get(oldkey);
    if (value == null) {
      return "ERR";
    }
    jedisCluster.set(newkey, value);
    jedisCluster.del(oldkey);
    return "OK";
  }

  /**
   * Renames Redis list. As this operation may span multiple servers, we RPUSH newkey elements with full LRANGE of
   * oldkey elements, then remove the old key.
   *
   * @param oldkey
   *          the old key
   * @param newkey
   *          the new key
   * @return OK if succeeded, ERR otherwise - e.g. when oldkey doesn't exist
   */
  String renameList(byte[] oldkey, byte[] newkey) {
    List<byte[]> lrange = jedisCluster.lrange(oldkey, 0, -1);
    if (lrange == null) {
      return "ERR";
    }
    jedisCluster.rpush(newkey, lrange.toArray(new byte[][] {}));
    jedisCluster.del(oldkey);
    return "OK";
  }

  /**
   * Renames Redis ZRANGE. As this operation may span multiple servers, we retrieve old ZRANGE, insert them (ZADD) at
   * newkey, then remove the old key. The
   *
   * @param oldkey
   *          the old key
   * @param newkey
   *          the new key
   * @return OK if succeeded, ERR otherwise - e.g. when oldkey doesn't exist
   */
  String renameZRange(byte[] oldkey, byte[] newkey) {
    Set<Tuple> values = jedisCluster.zrangeWithScores(oldkey, 0, -1);
    if (values == null) {
      return "ERR";
    }
    Map<byte[], Double> scoreMembers = new HashMap<>(values.size());
    for (Tuple t : values) {
      scoreMembers.put(t.getBinaryElement(), t.getScore());
    }
    jedisCluster.zadd(newkey, scoreMembers);
    jedisCluster.del(oldkey);
    return "OK";
  }

  @Override
  public byte[] get(byte[] key) {
    return jedisCluster.get(key);
  }

  @Override
  public Long publish(byte[] channel, byte[] message) {
    return jedisCluster.publish(channel, message);
  }

  @Override
  public void startMonitoring(MetricRegistry metrics) {
    for (JedisPool item : jedisCluster.getClusterNodes().values()) {
      AbstractJedisFacade.addMetrics(item, metrics);
    }
  }

  @Override
  public <T> RedisFacade.ResponseFacade<T> transaction(final byte[] key, final TransactionRunner<T> transaction) {
    if (transactionOnKey) {
      return jedisCluster.transaction(key, transaction);
    }
    return jedisCluster.transaction(transaction);
  }

  /**
   * If set to true, cluster transaction will be executed in multi mode on node owns slot for transaction key. If set to
   * false, each transaction step is executed on node depending of step's own key. This is default behavior.
   * 
   * @param transactionOnKey
   *          set true to use MULTI command for transactions
   */
  public void setTransactionOnKey(boolean transactionOnKey) {
    this.transactionOnKey = transactionOnKey;
  }
}
