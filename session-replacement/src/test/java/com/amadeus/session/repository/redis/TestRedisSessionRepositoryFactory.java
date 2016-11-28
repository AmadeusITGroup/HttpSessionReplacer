package com.amadeus.session.repository.redis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import org.junit.Test;

import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
public class TestRedisSessionRepositoryFactory {

  @Test
  public void testSingleRedisFacade() {
    RedisSessionRepositoryFactory factory = spy(new RedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.clusterMode = "SINGLE";
    RedisFacade facade = factory.getRedisFacade(config);
    assertThat(facade, instanceOf(RedisPoolFacade.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknwonRedisFacade() {
    RedisSessionRepositoryFactory factory = spy(new RedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisConfiguration config = spy(new RedisConfiguration(sessionConfig));
    config.clusterMode = "SOMETHING";
    factory.getRedisFacade(config);
  }

  @Test
  public void testSingleRedisRepository() {
    RedisSessionRepositoryFactory factory = spy(new RedisSessionRepositoryFactory());
    SessionConfiguration sessionConfig = spy(new SessionConfiguration());
    RedisSessionRepository repo = factory.repository(sessionConfig);
    assertNotNull(repo);
  }

  @Test
  public void testRedisIsDistributable() {
    assertTrue(new RedisSessionRepositoryFactory().isDistributed());
  }
}
