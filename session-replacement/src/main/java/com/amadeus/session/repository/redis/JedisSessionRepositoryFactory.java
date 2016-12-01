package com.amadeus.session.repository.redis;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
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
public class JedisSessionRepositoryFactory extends AbstractRedisSessionRepositoryFactory {
  static final Logger logger = LoggerFactory.getLogger(JedisSessionRepositoryFactory.class);

  @Override
  protected RedisFacade getRedisFacade(RedisConfiguration config) {
    JedisPoolConfig poolConfig = configurePool(config);
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

  /**
   * Configures Jedis pool of connection.
   * @param config
   *
   * @return configured Jedis pool of connection.
   */
  static JedisPoolConfig configurePool(RedisConfiguration config) {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(Integer.parseInt(config.poolSize));
    poolConfig.setMaxIdle(Math.min(poolConfig.getMaxIdle(), poolConfig.getMaxTotal()));
    poolConfig.setMinIdle(Math.min(poolConfig.getMinIdle(), poolConfig.getMaxIdle()));
    return poolConfig;
  }

  private RedisFacade singleInstance(JedisPoolConfig poolConfig, RedisConfiguration config) {
    int port = Integer.parseInt(config.port);
    String[] serverAndPort = config.server.split(":");
    if (serverAndPort.length > 1) {
      port = Integer.parseInt(serverAndPort[1]);
    }
    return new JedisPoolFacade(new JedisPool(poolConfig, config.server, port, config.timeout));
  }

  private RedisFacade sentinelFacade(JedisPoolConfig poolConfig, RedisConfiguration config) {
    return new JedisPoolFacade(new JedisSentinelPool(config.masterName, config.sentinels(), poolConfig,
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
    return new JedisClusterFacade(new TransactionalJedisCluster(jedisHostsAndPorts(config), config.timeout, poolConfig));
  }

  private Set<HostAndPort> jedisHostsAndPorts(RedisConfiguration config) {
    Set<HostAndPort> hostsAndPorts = new HashSet<>();
    for (RedisConfiguration.HostAndPort hp : config.hostsAndPorts()) {
      hostsAndPorts.add(new HostAndPort(hp.host, hp.port));
    }
    return hostsAndPorts;
  }
}
