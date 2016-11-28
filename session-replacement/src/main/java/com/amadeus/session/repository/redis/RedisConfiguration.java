package com.amadeus.session.repository.redis;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionConfiguration;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

/**
 * This class encapsulates configuration of Redis servers. It provides helper
 * methods to read configuratin, resolve server/sentinel/cluster member names,
 * and configure JedisPool.
 */
class RedisConfiguration {

  /**
   * System or configuration property that specifies that redis server(s) are
   * identified using IPv4 addresses. It is recommended that only one of the
   * {@link #REDIS_USE_IPV4} and {@link #REDIS_USE_IPV6} properties are set to
   * <code>true</code>. Default behavior is to use IPv4 addresses.
   */
  public static final String REDIS_USE_IPV4 = "com.amadeus.session.redis.ipv4";
  /**
   * System or configuration property that specifies that redis server(s) are
   * identified using IPv6 addresses. It is recommended that only one of the
   * {@link #REDIS_USE_IPV4} and {@link #REDIS_USE_IPV6} properties are set to
   * <code>true</code>. Default behavior is to use IPv4 addresses.
   */
  public static final String REDIS_USE_IPV6 = "com.amadeus.session.redis.ipv6";
  /**
   * System or configuration property that specifies expiration strategy used by redis.
   */
  public static final String REDIS_EXPIRATION_STRATEGY = "com.amadeus.session.redis.expiration";
  /**
   * System or configuration property that specifies connection and socket timeout used by redis.
   */
  public static final String REDIS_TIMEOUT = "com.amadeus.session.redis.timeout";
  /**
   * Default redis timeout.
   */
  public static final String DEFAULT_REDIS_TIMEOUT = "2000";
  /**
   * System or configuration property that specifies port of redis server(s) or
   * sentinel(s).
   */
  public static final String REDIS_PORT = "com.amadeus.session.redis.port";
  /**
   * System or configuration property that specifies the address(es) and
   * optionally port(s) of redis servers or sentinels.
   */
  public static final String REDIS_HOST = "com.amadeus.session.redis.host";
  /**
   * System or configuration property that specifies the size of the pool of
   * redis connections.
   */
  public static final String REDIS_POOL_SIZE = "com.amadeus.session.redis.pool";
  /**
   * Default size of the redis pool.
   */
  public static final String DEFAULT_REDIS_POOL_SIZE = "100";
  /**
   * System or configuration property that specifies the redis clustering mode.
   * Can be SINGLE, SENTINEL or CLUSTER.
   */
  public static final String REDIS_CLUSTER_MODE = "com.amadeus.session.redis.mode";
  /**
   * System or configuration property that specifies the name of redis master
   * when using sentinel mode.
   */
  public static final String REDIS_MASTER_NAME = "com.amadeus.session.redis.master";
  /**
   * Default name for redis master when using sentinel mode.
   */
  public static final String DEFAULT_REDIS_MASTER_NAME = "com.amadeus.session";

  static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

  static final String POOL_SIZE_PROPERTY = "pool=";
  static final String CLUSTER_MODE_PROPERTY = "mode=";
  static final String MASTER_NAME_PROPERTY = "master=";
  static final String HOST_PROPERTY = "host=";
  static final String REDIS_PORT_PROPERTY = "port=";
  static final String EXPIRATION_PROPERTY = "expiration=";
  static final String TIMEOUT_PROPERTY = "timeout=";

  String clusterMode;
  String masterName;
  String server;
  String port;
  String poolSize;
  ExpirationStrategy strategy;
  Boolean supportIpV6;
  Boolean supportIpV4;
  Integer timeout = null;

  RedisConfiguration(SessionConfiguration conf) {
    readConfigurationString(conf.getProviderConfiguration());
    serverAddress(conf);
    if (masterName == null) {
      masterName = conf.getAttribute(REDIS_MASTER_NAME, DEFAULT_REDIS_MASTER_NAME);
    }
    if (clusterMode == null) {
      clusterMode = conf.getAttribute(REDIS_CLUSTER_MODE, "SINGLE");
    }
    ipSupport(conf);
    if (poolSize == null) {
      poolSize = conf.getAttribute(REDIS_POOL_SIZE, DEFAULT_REDIS_POOL_SIZE);
    }
    if (strategy == null) {
      String expirationStrategy = conf.getAttribute(REDIS_EXPIRATION_STRATEGY, "NOTIF");
      try {
        strategy = ExpirationStrategy.valueOf(expirationStrategy);
      } catch (IllegalArgumentException e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Unknown expiration strategy. Got: `{}`, supported: {}, Excepion: {}", expirationStrategy,
              Arrays.asList(ExpirationStrategy.values()), e);
        }
        strategy = ExpirationStrategy.NOTIF;
      }
    }
    if (timeout == null) {
      timeout = Integer.parseInt(conf.getAttribute(REDIS_TIMEOUT, DEFAULT_REDIS_TIMEOUT));
    }
    logger.info("Redis configuration: {}", this);
  }

  private void serverAddress(SessionConfiguration conf) {
    if (server == null) {
      server = conf.getAttribute(REDIS_HOST, "localhost");
    }
    if (port == null) {
      port = conf.getAttribute(REDIS_PORT, "6379");
    }
  }

  /**
   * Reads IP address support configuration. Implementation may support IPv4 and
   * IPv6.
   */
  private void ipSupport(SessionConfiguration conf) {
    if (supportIpV4 == null) {
      supportIpV4 = Boolean.valueOf(conf.getAttribute(REDIS_USE_IPV4, "true"));
    }
    if (supportIpV6 == null) {
      supportIpV6 = Boolean.valueOf(conf.getAttribute(REDIS_USE_IPV6, "false"));
    }
  }

  private void readConfigurationString(String conf) {
    if (conf != null) {
      String[] args = conf.split(",");
      for (String arg : args) {
        parseArgFromConfiguration(arg.trim());
      }
    }
  }
  
  private void parseArgFromConfiguration(String arg) {
    if (arg.startsWith(POOL_SIZE_PROPERTY)) {
      poolSize = arg.substring(POOL_SIZE_PROPERTY.length());
    } else if (arg.startsWith(REDIS_PORT_PROPERTY)) {
      port = arg.substring(REDIS_PORT_PROPERTY.length());
    } else if (arg.startsWith(CLUSTER_MODE_PROPERTY)) {
      clusterMode = arg.substring(CLUSTER_MODE_PROPERTY.length());
    } else if (arg.startsWith(HOST_PROPERTY)) {
      server = arg.substring(HOST_PROPERTY.length());
    } else if (arg.startsWith(MASTER_NAME_PROPERTY)) {
      masterName = arg.substring(MASTER_NAME_PROPERTY.length());
    } else if (arg.startsWith(EXPIRATION_PROPERTY)) {
      try {
        strategy = ExpirationStrategy.valueOf(arg.substring(EXPIRATION_PROPERTY.length()));
      } catch (IllegalArgumentException e) {
        logger.warn("Unknown expiration strategy. Got: `" + arg + "`, supported: "
            + Arrays.asList(ExpirationStrategy.values()), e);
      }
    } else if (arg.startsWith(TIMEOUT_PROPERTY)) {
      timeout = Integer.parseInt(arg.substring(TIMEOUT_PROPERTY.length()));
    }
  }

  /**
   * Utility method to extract host and port from configuration. Used for Redis
   * cluster name resolution.
   *
   * @return set containing host ip addresses and ports.
   */
  Set<HostAndPort> hostsAndPorts() {
    Set<HostAndPort> hostAndPort = new HashSet<>();
    int defaultPort = Integer.parseInt(this.port);
    try {
      String[] servers = server.split("[/;]");
      for (String aServer : servers) {
        String[] serverAndPort = aServer.split(":");
        int portToUse = portToUse(serverAndPort, defaultPort);
        collectHosts(hostAndPort, serverAndPort, portToUse);
      }
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Unable to resolve cluster host for configuration " + this, e);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Port paramter was in server configuration. Expecting numeric values, but it was not: " + this);
    }
    logger.debug("Resolved hosts from '{}':{} are {}", server, port, hostAndPort);
    return hostAndPort;
  }

  private void collectHosts(Set<HostAndPort> hostAndPort, String[] serverAndPort, int portToUse)
      throws UnknownHostException {
    InetAddress[] hosts = resolveServers(serverAndPort[0]);
    for (InetAddress host : hosts) {
      if (isIpSupported(host)) {
        hostAndPort.add(new HostAndPort(host.getHostAddress(), portToUse));
      }
    }
  }

  /**
   * Returns port to use either from server:port pair, or default port.
   *
   * @param serverAndPort
   *          server and optional port pair
   * @param defaultPort
   *          default port o use
   * @return port to use
   */
  private int portToUse(String[] serverAndPort, int defaultPort) {
    int currentPort = defaultPort;
    if (serverAndPort.length > 1) {
      currentPort = Integer.parseInt(serverAndPort[1]);
    }
    return currentPort;
  }

  /**
   * Resolves server DNS name if needed. Retrieves all IP addresses associated
   * with DNS name.
   *
   * @param serverName
   *          DNS name or IP address
   * @return list of IP addresses associated with DNS name
   * @throws UnknownHostException
   *           if server name is not recognized by DNS
   */
  private InetAddress[] resolveServers(String serverName) throws UnknownHostException {
    InetAddress[] hosts = InetAddress.getAllByName(serverName);
    if (logger.isInfoEnabled()) {
      logger.info("Resolved hosts from '{}', parsed={} resolved={}", server, serverName, Arrays.asList(hosts));
    }
    return hosts;
  }

  /**
   * Check if IP address is allowed: e.g. is address IPv6 or IPv4 and is that
   * type of IP addresses allowed).
   *
   * @param host
   *          IP address of the host
   * @return if IP address is supported
   */
  private boolean isIpSupported(InetAddress host) {
    if (host instanceof Inet6Address) {
      return supportIpV6;
    }
    return supportIpV4;
  }

  /**
   * Returns set of sentinel servers
   *
   * @return
   */
  Set<String> sentinels() {
    return new HashSet<>(Arrays.asList(server.split("[/;]")));
  }

  /**
   * Configures Jedis pool of connection.
   *
   * @return configured Jedis pool of connection.
   */
  JedisPoolConfig configurePool() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(Integer.parseInt(this.poolSize));
    poolConfig.setMaxIdle(Math.min(poolConfig.getMaxIdle(), poolConfig.getMaxTotal()));
    poolConfig.setMinIdle(Math.min(poolConfig.getMinIdle(), poolConfig.getMaxIdle()));
    return poolConfig;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RedisConfiguration [clusterMode=").append(clusterMode).append(", masterName=").append(masterName)
        .append(", server=").append(server).append(", port=").append(port).append(", poolSize=").append(poolSize)
        .append(", strategy=").append(strategy).append(", supportIpV6=").append(supportIpV6).append(", supportIpV4=")
        .append(supportIpV4).append(", timeout=")
        .append(timeout).append("]");
    return builder.toString();
  }
}