package com.amadeus.session.repository.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionRepositoryFactory;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

/**
 * Redis configuration is provided as a comma separated list with following
 * parameters:
 *
 * <ul>
 * <li><code>pool</code> maximum size of pool.
 * <li><code>mode</code> CLUSTER|SENTINEL|SINGLE. If single, we are using single
 * redis server, otherwise we use cluster mode.
 * <li><code>host</code> slash separated list of DNS name or IP address of
 * cluster or sentinel nodes or server address for single node.
 * <li><code>port</code> default port for Redis servers
 * <li><code>master</code> master name when sentinel mode is used
 * <li><code>expiration</code> expiration strategy used. Possible values are
 * NOTIF and ZRANGE.
 * </ul>
 */
public class RedisSessionRepositoryFactory implements SessionRepositoryFactory {
  static final Logger logger = LoggerFactory.getLogger(RedisSessionRepositoryFactory.class);

  @Override
  public RedisSessionRepository repository(SessionConfiguration sessionConfiguration) {
    String namespace = sessionConfiguration.getNamespace();
    RedisConfiguration config = new RedisConfiguration(sessionConfiguration);
    RedisFacade redis = getRedisFacade(config);
    return new RedisSessionRepository(redis, namespace, sessionConfiguration.getNode(), config.strategy,
        sessionConfiguration.isSticky());
  }

  RedisFacade getRedisFacade(RedisConfiguration config) {
    JedisPoolConfig poolConfig = config.configurePool();
    switch (config.clusterMode) {
    case "SINGLE":
      return singleInstance(poolConfig, config);
    case "SENTINEL":
      return sentinelFacade(poolConfig, config);
    case "CLUSTER":
      return clusterFacade(poolConfig, config);
    default:
      throw new IllegalArgumentException("Unsupported redis mode: " + config);
    }
  }

  private RedisFacade singleInstance(JedisPoolConfig poolConfig, RedisConfiguration config) {
    int port = Integer.parseInt(config.port);
    String[] serverAndPort = config.server.split(":");
    if (serverAndPort.length > 1) {
      port = Integer.parseInt(serverAndPort[1]);
    }
    return new RedisPoolFacade(new JedisPool(poolConfig, config.server, port, config.timeout));
  }

  private RedisFacade sentinelFacade(JedisPoolConfig poolConfig, RedisConfiguration config) {
    return new RedisPoolFacade(new JedisSentinelPool(config.masterName, config.sentinels(), poolConfig,
        config.timeout));
  }

  /**
   * This method builds RedisFacade that connects to Redis cluster. We retrieve
   * all IP addresses corresponding to passed DNS name and use them as cluster
   * nodes.
   *
   * @param poolConfig
   * @param config
   * @return
   */
  private RedisFacade clusterFacade(JedisPoolConfig poolConfig, RedisConfiguration config) {
    return new RedisClusterFacade(new TransactionalJedisCluster(config.hostsAndPorts(), config.timeout, poolConfig));
  }

  @Override
  public boolean isDistributed() {
    return true;
  }
}
