package com.amadeus.session.repository.redis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import org.junit.Test;

import com.amadeus.session.SessionConfiguration;

import redis.clients.jedis.JedisPoolConfig;

@SuppressWarnings("javadoc")
public class TestRedisSessionRepositoryFactory {

  @Test
  public void testSingleRedisFacade() {
    JedisSessionRepositoryFactory factory = spy(new JedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.clusterMode = "SINGLE";
    RedisFacade facade = factory.getRedisFacade(config);
    assertThat(facade, instanceOf(JedisPoolFacade.class));
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
