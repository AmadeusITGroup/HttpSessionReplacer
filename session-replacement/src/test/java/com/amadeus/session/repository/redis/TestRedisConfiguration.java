package com.amadeus.session.repository.redis;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionConfiguration;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Test redis {@link RedisConfiguration} object
 *
 */
@SuppressWarnings("javadoc")
public class TestRedisConfiguration {
  private SessionConfiguration sc;

  @Before
  public void setup() {
    sc = new SessionConfiguration();
    sc.setProviderConfiguration("");
  }

  @Test
  public void testDefaultConfiguration() {
    RedisConfiguration configuration = new RedisConfiguration(sc);
    assertEquals("100", configuration.poolSize);
    assertEquals("SINGLE", configuration.clusterMode);
    assertEquals("localhost", configuration.server);
    assertEquals("6379", configuration.port);
    assertEquals(ExpirationStrategy.NOTIF, configuration.strategy);
    assertEquals(new Integer(2000), configuration.timeout);
  }

  @Test
  public void testParseConfiguration() {
	  sc.setProviderConfiguration("pool=400,timeout=5000,host=www.example.com,expiration=ZRANGE");
    RedisConfiguration configuration = new RedisConfiguration(sc);
    assertEquals("400", configuration.poolSize);
    assertEquals("www.example.com", configuration.server);
    assertEquals(ExpirationStrategy.ZRANGE, configuration.strategy);
    assertEquals(new Integer(5000), configuration.timeout);
  }

  @Test
  public void testExtractHostsAndPorts() {
    RedisConfiguration configuration = new RedisConfiguration(sc);
    Set<HostAndPort> hostsAndPorts = configuration.hostsAndPorts();
    assertEquals(1, hostsAndPorts.size());
    assertEquals("127.0.0.1", hostsAndPorts.iterator().next().getHost());
  }

  @Test
  public void testExtractManyHostsAndPorts() {
    sc.setProviderConfiguration("host=1.2.3.4:2/5.6.7.8");
    RedisConfiguration configuration = new RedisConfiguration(sc);
    Set<HostAndPort> hostsAndPorts = configuration.hostsAndPorts();
    assertEquals(2, hostsAndPorts.size());
    ArrayList<HostAndPort> asList = new ArrayList<>(hostsAndPorts);
    assertEquals("1.2.3.4", asList.get(0).getHost());
    assertEquals(2, asList.get(0).getPort());
    assertEquals("5.6.7.8", asList.get(1).getHost());
    assertEquals(6379, asList.get(1).getPort());
  }

  @Test
  public void testExtractSentinels() {
    RedisConfiguration configuration = new RedisConfiguration(sc);
    Set<String> sentinels = configuration.sentinels();
    assertEquals(1, sentinels.size());
    assertEquals("localhost", sentinels.iterator().next());
  }

  @Test
  public void testConfigurePool() {
    RedisConfiguration configuration = new RedisConfiguration(sc);
    configuration.poolSize = "500";
    JedisPoolConfig pool = configuration.configurePool();
    assertEquals(500, pool.getMaxTotal());
  }
}
