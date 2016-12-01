package com.amadeus.session.repository.redis;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionRepositoryFactory;

public abstract class AbstractRedisSessionRepositoryFactory implements SessionRepositoryFactory {

  @Override
  public RedisSessionRepository repository(SessionConfiguration sessionConfiguration) {
    String namespace = sessionConfiguration.getNamespace();
    RedisConfiguration config = new RedisConfiguration(sessionConfiguration);
    RedisFacade redis = getRedisFacade(config);
    return new RedisSessionRepository(redis, namespace, sessionConfiguration.getNode(), config.strategy,
        sessionConfiguration.isSticky());
  }

  /**
   * Override this method to implement abstract factory for {@link RedisFacade}.
   *
   * @param config
   *          redis configuration
   * @return an instance of redis facade
   */
  protected abstract RedisFacade getRedisFacade(RedisConfiguration config);

  @Override
  public boolean isDistributed() {
    return true;
  }
}
