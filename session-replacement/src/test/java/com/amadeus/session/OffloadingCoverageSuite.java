package com.amadeus.session;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.amadeus.session.repository.inmemory.TestInMemoryRepository;
import com.amadeus.session.repository.redis.TestExpirationListener;
import com.amadeus.session.repository.redis.TestNotificationExpirationManagement;
import com.amadeus.session.repository.redis.TestJedisClusterFacade;
import com.amadeus.session.repository.redis.TestRedisConfiguration;
import com.amadeus.session.repository.redis.TestJedisPoolFacade;
import com.amadeus.session.repository.redis.TestRedisSessionRepository;
import com.amadeus.session.repository.redis.TestRedisSessionRepositoryFactory;
import com.amadeus.session.repository.redis.TestSortedSetExpiration;
import com.amadeus.session.servlet.TestCookieSessionTracking;
import com.amadeus.session.servlet.TestHttpRequestWrapper;
import com.amadeus.session.servlet.TestHttpResponseWrapper;
import com.amadeus.session.servlet.TestHttpSessionFactory;
import com.amadeus.session.servlet.TestHttpSessionNotifier;
import com.amadeus.session.servlet.TestInitializeSessionManagement;
import com.amadeus.session.servlet.TestRepositoryBackendHttpSessionWrapper;
import com.amadeus.session.servlet.TestSessionHelpers;
import com.amadeus.session.servlet.TestShutdownListener;
import com.amadeus.session.servlet.TestUrlSessionTracking;
import com.amadeus.session.servlet.TestWebXmlParser;

/**
 * Used to calculate coverage in eclipse.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestJdkSerializerDeserializer.class,
  TestRepositoryBackedSession.class,
  TestRandomIdProvider.class,
  TestSessionConfiguration.class,
  TestSessionData.class,
  TestSessionManager.class,
  TestUuidProvider.class,

  TestHttpRequestWrapper.class,
  TestHttpResponseWrapper.class,
  TestHttpSessionNotifier.class,
  TestHttpSessionFactory.class,
  TestSessionHelpers.class,
  TestShutdownListener.class,
  TestJedisClusterFacade.class,
  TestWebXmlParser.class,
  TestCookieSessionTracking.class,
  TestUrlSessionTracking.class,
  TestInitializeSessionManagement.class,
  TestRepositoryBackendHttpSessionWrapper.class,

  TestInMemoryRepository.class,

  TestExpirationListener.class,
  TestJedisClusterFacade.class,
  TestJedisPoolFacade.class,
  TestRedisSessionRepository.class,
  TestSortedSetExpiration.class,
  TestNotificationExpirationManagement.class,
  TestRedisConfiguration.class,
  TestRedisSessionRepositoryFactory.class,
  TestSortedSetExpiration.class
})
public class OffloadingCoverageSuite {

}
