package com.amadeus.session.repository.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.DefaultSessionFactory;
import com.amadeus.session.ExecutorFacade;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionNotifier;
import com.amadeus.session.SessionRepository.CommitTransaction;
import com.amadeus.session.SessionTracking;

@SuppressWarnings("javadoc")
public abstract class AbstractITRedis {

  protected RedisSessionRepositoryFactory rsrf;
  protected SessionConfiguration sessionConfiguration;
  protected SessionTracking tracking;
  protected SessionNotifier notifier;
  protected RedisSessionRepository repo;
  protected SessionManager sessionManager;
  protected String expirationStrategy;

  public AbstractITRedis(String expirationStrategy) {
    this.expirationStrategy = expirationStrategy;
  }

  @Before
  public void setup() {
    assumeTrue(Boolean.valueOf(System.getProperty("redis.integration.tests")));
    if (Boolean.valueOf(System.getProperty("redis.integration.tests.use.docker.host"))) {
      String dockerHost = System.getenv("DOCKER_HOST");
      Matcher matcher = Pattern.compile(".+//([^:]+):.+").matcher(dockerHost);
      assertTrue("DOCKER_HOST environment variable should match pattern .+//([^:]+):.+, value was "
          + dockerHost, matcher.matches());
      String redisHost = matcher.group(1);
      String conf = System.getProperty("com.amadeus.session.repository.conf");
      if (conf != null) {
        System.setProperty("com.amadeus.session.repository.conf", conf.replace("${redis_host}", redisHost));
      }
    }

    rsrf = new RedisSessionRepositoryFactory();
    sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setRepositoryFactory("redis");
    sessionConfiguration.setMaxInactiveInterval(5);
    SessionConfiguration.AttributeProvider attributes = mock(SessionConfiguration.AttributeProvider.class);
    when(attributes.getAttribute("com.amadeus.session.redis.expiration")).thenReturn(expirationStrategy);
    sessionConfiguration.initializeFrom(attributes);
    tracking = mock(SessionTracking.class);
    notifier = mock(SessionNotifier.class);
    repo = rsrf.repository(sessionConfiguration);
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    sessionManager = new SessionManager(new ExecutorFacade(sessionConfiguration),
        new DefaultSessionFactory(), repo, tracking, notifier, sessionConfiguration, classLoader);
  }

  @After
  public void shutdown() {
    if (sessionManager != null) {
      sessionManager.close();
    }
    if (repo != null) {
      repo.close();
    }
  }

  @Test
  public void testSessionDataStorage() {
    assertNull(repo.getSessionData("2"));
    SessionData sessionData = new SessionData("1", System.currentTimeMillis(), 100);
    repo.storeSessionData(sessionData);
    SessionData storedSessionData = repo.getSessionData(sessionData.getId());
    assertNotSame(sessionData, storedSessionData);
    assertEquals(sessionData.getId(), storedSessionData.getId());
    assertEquals(sessionData.getLastAccessedTime(), storedSessionData.getLastAccessedTime());
    assertEquals(sessionData.getCreationTime(), storedSessionData.getCreationTime());
    repo.remove(storedSessionData);
    assertNull(repo.getSessionData("1"));
    repo.getRedis().del(repo.sessionKey("1"));
  }

  @Test
  public void testSessionAttributes() {
    Date date = new Date();
    repo.setSessionAttribute(sessionData("1"), "attrString", "value");
    repo.setSessionAttribute(sessionData("1"), "attrInt", Integer.valueOf(10));
    repo.setSessionAttribute(sessionData("1"), "attrDate", date);
    repo.setSessionAttribute(sessionData("2"), "attrString", "anotherValue");
    assertNull(repo.getSessionAttribute(sessionData("3"), "attrString"));
    assertNull(repo.getSessionAttribute(sessionData("3"), "notThere"));

    assertEquals(Integer.valueOf(10), repo.getSessionAttribute(sessionData("1"), "attrInt"));
    assertEquals("value", repo.getSessionAttribute(sessionData("1"), "attrString"));
    assertEquals("anotherValue", repo.getSessionAttribute(sessionData("2"), "attrString"));
    assertEquals(date, repo.getSessionAttribute(sessionData("1"), "attrDate"));
    repo.setSessionAttribute(sessionData("1"), "attrString", "changedValue");
    assertEquals("changedValue", repo.getSessionAttribute(sessionData("1"), "attrString"));
    assertNull(repo.getSessionAttribute(sessionData("1"), "notThere"));
    assertThat(repo.getAllKeys(sessionData("1")), Matchers.containsInAnyOrder("attrDate", "attrString", "attrInt"));
    assertThat(repo.getAllKeys(sessionData("1")), Matchers.hasSize(3));
    assertThat(repo.getAllKeys(sessionData("2")), Matchers.hasSize(1));
    assertThat(repo.getAllKeys(sessionData("3")), Matchers.empty());

    repo.removeSessionAttribute(sessionData("1"), "attrInt");
    assertNull(repo.getSessionAttribute(sessionData("1"), "attrInt"));

    repo.removeSessionAttribute(sessionData("1"), "notThere");
    assertNull(repo.getSessionAttribute(sessionData("1"), "notThere"));
    repo.getRedis().del(repo.sessionKey("1"));
    repo.getRedis().del(repo.sessionKey("2"));
    repo.getRedis().del(repo.sessionKey("3"));
  }

  private SessionData sessionData(String id) {
    return new SessionData(id, 100, 10);
  }

  @Test
  public void testSessionCommit() {
    SessionData sessionData = new SessionData("1", System.currentTimeMillis(), 200);
    CommitTransaction trans = repo.startCommit(sessionData);
    trans.addAttribute("attrStr", "aValue");
    trans.addAttribute("attrToRemove", "toRemoveValue");
    trans.addAttribute("attrToChange", "initialValue");
    trans.commit();
    assertEquals("aValue", repo.getSessionAttribute(sessionData("1"), "attrStr"));
    assertEquals("toRemoveValue", repo.getSessionAttribute(sessionData("1"), "attrToRemove"));
    assertEquals("initialValue", repo.getSessionAttribute(sessionData("1"), "attrToChange"));
    assertNull(repo.getSessionAttribute(sessionData("1"), "attrAdded"));
    SessionData retrievedSessionData = repo.getSessionData("1");
    CommitTransaction trans2 = repo.startCommit(retrievedSessionData);
    trans2.addAttribute("attrAdded", "addedValue");
    trans2.addAttribute("attrToChange", "newValue");
    trans2.removeAttribute("attrToRemove");
    trans2.commit();
    assertEquals("addedValue", repo.getSessionAttribute(sessionData("1"), "attrAdded"));
    assertEquals("aValue", repo.getSessionAttribute(sessionData("1"), "attrStr"));
    assertEquals("newValue", repo.getSessionAttribute(sessionData("1"), "attrToChange"));
    assertNull(repo.getSessionAttribute(sessionData("1"), "attrToRemove"));
    repo.getRedis().del(repo.sessionKey("1"));
  }

  @Test
  public void testSessionExpiry() throws InterruptedException {
    SessionData sessionData = new SessionData("expire", System.currentTimeMillis(), 2);
    CommitTransaction trans = repo.startCommit(sessionData);
    trans.addAttribute("attrStr", "aValue");
    trans.addAttribute("attrToRemove", "toRemoveValue");
    trans.addAttribute("attrToChange", "initialValue");
    trans.commit();
    assertNotNull(repo.getSessionAttribute(sessionData("expire"), "attrStr"));
    await(1000);
    assertNotNull(repo.getSessionAttribute(sessionData("expire"), "attrStr"));
    await(700);
    // Time sensitive (if two line above and the next take more than 1000ms,
    // this may fail, but normally, operation repo.getSessionAttribute ~ 1ms.
    assertNotNull(repo.getSessionAttribute(sessionData("expire"), "attrStr"));
    await(3000);
    assertNull(repo.getSessionAttribute(sessionData("expire"), "attrStr"));
  }

  // Wait for specified amount of milliseconds
  private static void await(int millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
