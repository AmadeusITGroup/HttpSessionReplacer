package com.amadeus.session.repository.redis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.amadeus.session.SessionConfiguration;

import redis.clients.jedis.JedisPoolConfig;

@SuppressWarnings("javadoc")
public class TestJedisSessionRepositoryFactory {

  @Test
  public void testSingleRedisFacade() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.clusterMode = "SINGLE";
    RedisFacade facade = factory.getRedisFacade(config);
    assertThat(facade, instanceOf(JedisPoolFacade.class));
  }

  @Test
  public void testSingleRedisFacadeWithPort() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.server = "1.2.3.4:1";
    config.clusterMode = "SINGLE";
    RedisFacade facade = factory.getRedisFacade(config);
    assertThat(facade, instanceOf(JedisPoolFacade.class));
  }

  @Test
  public void testSentinelRedisFacade() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    JedisPoolFacade jpf = mock(JedisPoolFacade.class);
    doReturn(jpf).when(factory).sentinelFacade(any(JedisPoolConfig.class), any(RedisConfiguration.class));
    config.server = "1.2.3.4:1;2.3.4.5:2";
    config.clusterMode = "SENTINEL";
    RedisFacade facade = factory.getRedisFacade(config);
    verify(factory).sentinelFacade(any(JedisPoolConfig.class), eq(config));
    assertThat(facade, instanceOf(JedisPoolFacade.class));
  }

  @Test
  public void testClusterRedisFacade() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    JedisClusterFacade clusterFacade = mock(JedisClusterFacade.class);
    doReturn(clusterFacade).when(factory).clusterFacade(any(JedisPoolConfig.class), any(RedisConfiguration.class));
    config.server = "1.2.3.4:1;2.3.4.5:2";
    config.clusterMode = "CLUSTER";
    RedisFacade facade = factory.getRedisFacade(config);
    verify(factory).clusterFacade(any(JedisPoolConfig.class), eq(config));
    assertSame(clusterFacade, facade);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testUnknwonRedisFacade() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.clusterMode = "SOMETHING";
    factory.getRedisFacade(config);
  }

  @Test
  public void testSingleRedisRepository() {
    AbstractRedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisSessionRepository repo = factory.repository(sessionConfig);
    assertNotNull(repo);
  }

  @Test
  public void testRedisIsDistributable() {
    assertTrue(new JedisSessionRepositoryFactory().isDistributed());
  }

  @Test
  public void testConfigurePool() {
    SessionConfiguration sc = new SessionConfiguration();
    RedisConfiguration configuration = new RedisConfiguration(sc);
    configuration.poolSize = "500";
    JedisPoolConfig pool = JedisSessionRepositoryFactory.configurePool(configuration);
    assertEquals(500, pool.getMaxTotal());
  }
}
