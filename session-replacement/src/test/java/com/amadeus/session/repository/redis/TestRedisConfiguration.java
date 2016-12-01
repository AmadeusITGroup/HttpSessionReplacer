package com.amadeus.session.repository.redis;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.repository.redis.RedisConfiguration.HostAndPort;

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
    List<HostAndPort> hostsAndPorts = configuration.hostsAndPorts();
    assertEquals(1, hostsAndPorts.size());
    assertEquals("127.0.0.1", hostsAndPorts.iterator().next().host);
  }

  @Test
  public void testExtractManyHostsAndPorts() {
    sc.setProviderConfiguration("host=1.2.3.4:2/5.6.7.8");
    RedisConfiguration configuration = new RedisConfiguration(sc);
    List<HostAndPort> hostsAndPorts = configuration.hostsAndPorts();
    assertEquals(2, hostsAndPorts.size());
    ArrayList<HostAndPort> asList = new ArrayList<>(hostsAndPorts);
    assertEquals("1.2.3.4", asList.get(0).host);
    assertEquals(2, asList.get(0).port);
    assertEquals("5.6.7.8", asList.get(1).host);
    assertEquals(6379, asList.get(1).port);
  }

  @Test
  public void testExtractSentinels() {
    RedisConfiguration configuration = new RedisConfiguration(sc);
    Set<String> sentinels = configuration.sentinels();
    assertEquals(1, sentinels.size());
    assertEquals("localhost", sentinels.iterator().next());
  }
}
