package com.amadeus.session.repository.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amadeus.session.repository.redis.RedisFacade;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.JedisPoolConfig;

public class CustomRedisFacade implements RedisFacade{
  
  public CustomRedisFacade(JedisPoolConfig poolConfig, RedisConfiguration config){
   
  }

  @Override
  public void psubscribe(RedisPubSub listener, String pattern) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void punsubscribe(RedisPubSub expirationListener, byte[] pattern) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Long hdel(byte[] key, byte[]... fields) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long hsetnx(byte[] key, byte[] field, byte[] value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long hset(byte[] key, byte[] field, byte[] value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String set(byte[] key, byte[] value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String setex(byte[] key, int expiry, byte[] value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long expire(byte[] key, int value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void srem(byte[] key, byte[]... member) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Long sadd(byte[] key, byte[]... member) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long del(byte[]... keys) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Boolean exists(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long expireAt(byte[] key, long unixTime) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long zadd(byte[] key, double score, byte[] elem) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long zrem(byte[] key, byte[]... fields) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double start, double end) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<byte[]> zrange(byte[] key, long start, long end) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long persist(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String info(String section) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supportsMultiSpop() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void requestFinished() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public byte[] get(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long publish(byte[] channel, byte[] message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void startMonitoring(MetricRegistry metrics) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isRedisException(Exception e) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <T> ResponseFacade<T> transaction(byte[] key, TransactionRunner<T> transaction) {
    // TODO Auto-generated method stub
    return null;
  }

}
