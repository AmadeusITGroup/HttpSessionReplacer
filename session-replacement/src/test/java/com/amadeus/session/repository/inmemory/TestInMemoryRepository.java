package com.amadeus.session.repository.inmemory;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionRepository;
import com.amadeus.session.SessionRepository.CommitTransaction;
import com.amadeus.session.repository.inmemory.InMemoryRepository.CleanupTask;

@SuppressWarnings("javadoc")
public class TestInMemoryRepository {

  private InMemoryRepository repository;

  @Before
  public void setup() {
    repository = new InMemoryRepository("something");
  }

  @Test
  public void testSessionData() {
    assertNull(repository.getSessionData("test"));
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    assertSame(sessionData, repository.getSessionData("test"));
    SessionData anotherSessionData = new SessionData("test", 1001, 10);
    repository.storeSessionData(anotherSessionData);
    assertNotSame(sessionData, repository.getSessionData("test"));
    assertSame(anotherSessionData, repository.getSessionData("test"));
    repository.remove(anotherSessionData);
    assertNull(repository.getSessionData("test"));
  }

  @Test
  public void testAllKeys() {
    SessionData sessionData = new SessionData("test", 1000, 10);
    assertTrue(repository.getAllKeys(sessionData).isEmpty());
    repository.storeSessionData(sessionData);
    assertTrue(repository.getAllKeys(sessionData).isEmpty());
    repository.setSessionAttribute(sessionData, "name", "value");
    assertTrue(repository.getAllKeys(new SessionData("test2", 100, 10)).isEmpty());
    assertFalse(repository.getAllKeys(sessionData).isEmpty());
    assertThat(repository.getAllKeys(sessionData), hasItem("name"));
    assertThat(repository.getAllKeys(sessionData), not(hasItem("name2")));
    repository.setSessionAttribute(sessionData, "name2", "value");
    assertThat(repository.getAllKeys(sessionData), hasItem("name2"));
    repository.removeSessionAttribute(sessionData, "name2");
    assertThat(repository.getAllKeys(sessionData), hasItem("name"));
    assertThat(repository.getAllKeys(sessionData), not(hasItem("name2")));
  }

  @Test
  public void testSessionAttributes() {
    SessionData sessionData = new SessionData("test", 1000, 10);
    assertNull(repository.getSessionAttribute(sessionData, "name"));
    repository.storeSessionData(sessionData);
    assertNull(repository.getSessionAttribute(sessionData, "name"));
    repository.setSessionAttribute(sessionData, "name", "value");
    assertEquals("value", repository.getSessionAttribute(sessionData, "name"));
  }

  @Test
  public void testPrepareRemove() {
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    repository.prepareRemove(sessionData);
    assertThat(repository.getOwnedSessionIds(), not(hasItem("test")));
  }

  @Test
  public void testTransaction() {
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    repository.setSessionAttribute(sessionData, "name3", "value");
    assertNotNull(repository.getSessionAttribute(sessionData, "name3"));
    CommitTransaction trans = repository.startCommit(sessionData);
    trans.addAttribute("name", "value");
    trans.addAttribute("name2", "value2");
    trans.removeAttribute("name3");
    trans.commit();
    assertEquals("value", repository.getSessionAttribute(sessionData, "name"));
    assertEquals("value2", repository.getSessionAttribute(sessionData, "name2"));
    assertNull(repository.getSessionAttribute(sessionData, "name3"));
  }

  @Test
  public void testCleanSessionsOnShutdown() {
    assertTrue(repository.cleanSessionsOnShutdown());
  }

  @Test
  public void testGetOwnedSessionIds() {
    assertThat(repository.getOwnedSessionIds(), not(hasItem("test")));
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    assertThat(repository.getOwnedSessionIds(), hasItem("test"));
    assertThat(repository.getOwnedSessionIds(), not(hasItem("test2")));
    repository.remove(sessionData);
    assertThat(repository.getOwnedSessionIds(), not(hasItem("test")));
  }

  @Test
  public void testSessionIdChange() {
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    repository.setSessionAttribute(sessionData, "name", "value");
    SessionData sessionData2 = new SessionData("test", 1000, 10);
    sessionData2.setNewSessionId("test2");
    repository.sessionIdChange(sessionData2);
    assertNotNull(repository.getSessionAttribute(sessionData2, "name"));
    SessionData sessionDataOrig = new SessionData("test", 1000, 10);
    assertNull(repository.getSessionAttribute(sessionDataOrig, "name"));
  }

  @Test
  public void testCleanUp() {
    SessionManager sm = mock(SessionManager.class);
    repository.setSessionManager(sm);
    verify(sm).schedule(anyString(), any(CleanupTask.class), anyLong());
    SessionData sessionData = new SessionData("test", 1000, 10);
    repository.storeSessionData(sessionData);
    SessionData sessionData2 = new SessionData("test2", 1000, 10);
    repository.storeSessionData(sessionData2);
    CleanupTask cleanUp = repository.new CleanupTask();
    cleanUp.run();
    assertTrue(repository.sessionDataCache.isEmpty());
  }

  @Test
  public void testInMemoryIsNotDistributable() {
    assertFalse(new InMemoryRepositoryFactory().isDistributed());
  }

  @Test
  public void testInMemoryFactory() {
    SessionRepository myRepository = new InMemoryRepositoryFactory().repository(new SessionConfiguration());
    assertThat(myRepository, instanceOf(InMemoryRepository.class));
  }
}
