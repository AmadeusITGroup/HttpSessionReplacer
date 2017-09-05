package com.amadeus.session;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionConfiguration.ReplicationTrigger;
import com.amadeus.session.SessionRepository.CommitTransaction;
import com.amadeus.session.repository.inmemory.InMemoryRepository;

@SuppressWarnings("javadoc")
public class TestRepositoryBackedSession {
  private SessionManager manager;
  private SessionRepository repository;
  private SessionNotifier notifier;
  private SessionConfiguration sessionConfiguration;
  private SessionData sessionData;
  private SessionFactory factory;
  private CommitTransaction transaction;

  @Before
  public void setUpMocks() {
    manager = mock(SessionManager.class);
    repository = mock(SessionRepository.class);
    transaction = mock(SessionRepository.CommitTransaction.class);
    when(repository.startCommit(any(SessionData.class))).thenReturn(transaction);
    notifier = mock(SessionNotifier.class);
    sessionConfiguration = new SessionConfiguration();
    factory = mock(SessionFactory.class);
    when(manager.getConfiguration()).thenReturn(sessionConfiguration);
    when(manager.getRepository()).thenReturn(repository);
    when(manager.getNotifier()).thenReturn(notifier);
    sessionData = sessionData("1");
  }

  @Test
  public void testSetAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    verify(repository).getSessionAttribute(sessionData("1"), "Test");
    verify(repository, never()).setSessionAttribute(any(SessionData.class), any(String.class), any());
    verify(notifier, never()).attributeHasBeenRestored(refEq(rbs), eq("Test"), any());
    verify(notifier, never()).attributeReplaced(refEq(rbs), eq("Test"), any());
    verify(notifier).attributeAdded(refEq(rbs), eq("Test"), eq("value"));
    assertEquals("value", rbs.getAttribute("Test"));
  }

  @Test
  public void testSetExistingAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    verify(notifier, never()).attributeReplaced(refEq(rbs), eq("Test"), any());

    rbs.setAttribute("Test", "value2");
    verify(repository).getSessionAttribute(sessionData("1"), "Test");
    verify(repository, never()).setSessionAttribute(any(SessionData.class), any(String.class), any());
    verify(notifier, never()).attributeHasBeenRestored(refEq(rbs), eq("Test"), eq("value2"));
    verify(notifier).attributeReplaced(refEq(rbs), eq("Test"), eq("value"));
    verify(notifier).attributeAdded(refEq(rbs), eq("Test"), eq("value"));
    verify(notifier).attributeAdded(refEq(rbs), eq("Test"), eq("value2"));
    assertEquals("value2", rbs.getAttribute("Test"));
  }

  @Test
  public void testSetExistingAttributeInRepository() {
    SessionRepository inMemoryRepository = new InMemoryRepository("default");
    when(manager.getRepository()).thenReturn(inMemoryRepository);
    inMemoryRepository.storeSessionData(sessionData);
    inMemoryRepository.setSessionAttribute(sessionData, "Test", "old-value");
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    verify(notifier).attributeHasBeenRestored(refEq(rbs), eq("Test"), eq("old-value"));
    verify(notifier).attributeReplaced(refEq(rbs), eq("Test"), eq("old-value"));
    verify(notifier).attributeAdded(refEq(rbs), eq("Test"), eq("value"));
    assertEquals("value", rbs.getAttribute("Test"));
  }

  @Test
  public void testSetRemovedExistingAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "old-value");
    verify(notifier, never()).attributeReplaced(refEq(rbs), eq("Test"), any());
    rbs.removeAttribute("Test");
    verify(notifier).attributeRemoved(refEq(rbs), eq("Test"), eq("old-value"));
    assertNull(rbs.getAttribute("Test"));
    rbs.setAttribute("Test", "value");
    verify(notifier, never()).attributeReplaced(refEq(rbs), eq("Test"), eq("old-value"));
    verify(notifier).attributeAdded(refEq(rbs), eq("Test"), eq("value"));
  }

  @Test
  public void testSetNonCachedAttribute() {
    sessionData.setMandatoryRemoteKeys(Collections.singleton("RemoteAttr"));
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("RemoteAttr", "value");
    verify(repository).setSessionAttribute(eq(sessionData("1")), eq("RemoteAttr"), eq("value"));
    verify(notifier).attributeAdded(refEq(rbs), eq("RemoteAttr"), eq("value"));
  }

  @Test
  public void testSetNullAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    assertEquals("value", rbs.getAttribute("Test"));
    rbs.setAttribute("Test", null);
    verify(notifier).attributeRemoved(refEq(rbs), eq("Test"), eq("value"));
    assertNull(rbs.getAttribute("Test"));
  }

  @Test
  public void testRemoveAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.removeAttribute("Test");
    verify(notifier).attributeRemoved(refEq(rbs), eq("Test"), eq("value"));
    assertNull(rbs.getAttribute("Test"));
  }

  @Test
  public void testRemoveAttributeFromRepository() {
    SessionRepository inMemoryRepository = new InMemoryRepository("default");
    when(manager.getRepository()).thenReturn(inMemoryRepository);
    inMemoryRepository.storeSessionData(sessionData);
    inMemoryRepository.setSessionAttribute(sessionData, "Test", "value");
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.removeAttribute("Test");
    verify(notifier).attributeRemoved(any(RepositoryBackedSession.class), eq("Test"), eq("value"));
    assertNull(rbs.getAttribute("Test"));
  }

  @Test
  public void testRemoveNonExistingAttribute() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertNull(rbs.getAttribute("Test"));
    rbs.removeAttribute("Test");
    assertNull(rbs.getAttribute("Test"));
  }

  @Test
  public void testIsExpired() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertTrue(rbs.isExpired());
  }

  @Test
  public void testIsNotExpired() {
    sessionData = new SessionData("1", System.currentTimeMillis(), 10);
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertFalse(rbs.isExpired());
  }

  @Test(expected = IllegalStateException.class)
  public void testInvalidate() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("Test2", "value2");
    when(repository.prepareRemove(sessionData("1"))).thenReturn(Boolean.TRUE);
    rbs.invalidate();
    verify(repository).prepareRemove(sessionData("1"));
    verify(manager).remove(sessionData);
    verify(notifier).sessionDestroyed(refEq(rbs), eq(false));
    assertFalse(rbs.isValid());
    rbs.getAttribute("Test");
  }

  @Test
  public void testInvalidateConflict() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    assertEquals("value", rbs.getAttribute("Test"));
    when(repository.prepareRemove(sessionData("1"))).thenReturn(Boolean.FALSE);
    rbs.invalidate();
    verify(manager).invalidationConflict(any(RepositoryBackedSession.class), eq(false));
    verify(repository).prepareRemove(sessionData("1"));
    verify(repository, never()).remove(sessionData);
    verify(notifier, never()).sessionDestroyed(refEq(rbs), eq(false));
    assertFalse(rbs.isValid());
  }

  @Test
  public void testDoInvalidateExpiry() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    assertEquals("value", rbs.getAttribute("Test"));
    when(repository.prepareRemove(sessionData("1"))).thenReturn(Boolean.TRUE);
    rbs.doInvalidate(true);
    verify(manager, never()).invalidationConflict(any(RepositoryBackedSession.class), eq(true));
    // Session is invalidated only after commit
    assertTrue(rbs.isValid());
    rbs.new Committer().run();
    assertFalse(rbs.isValid());
  }

  @Test
  public void testDoInvalidateExpiryNotUsed() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    when(repository.prepareRemove(sessionData("1"))).thenReturn(Boolean.TRUE);
    rbs.doInvalidate(true);
    verify(manager, never()).invalidationConflict(any(RepositoryBackedSession.class), eq(true));
    // Session is invalidated only after commit
    assertFalse(rbs.isValid());
  }

  @Test
  public void testDoInvalidateExpiryConflict() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    assertEquals("value", rbs.getAttribute("Test"));
    when(repository.prepareRemove(sessionData("1"))).thenReturn(Boolean.FALSE);
    rbs.doInvalidate(true);
    verify(manager).invalidationConflict(any(RepositoryBackedSession.class), eq(true));
    assertFalse(rbs.isValid());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetAttributeNames() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("Test2", "value");
    ArrayList<String> names = Collections.list(rbs.getAttributeNames());
    assertThat(names, hasItems("Test", "Test2"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetAttributeNamesWithRemoteKeys() {
    when(repository.getAllKeys(any(SessionData.class))).thenReturn(new HashSet<>(Arrays.asList("Test2", "Test3")));
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("Test2", "value");
    ArrayList<String> names = Collections.list(rbs.getAttributeNames());
    assertThat(names, hasItems("Test", "Test2", "Test3"));
  }

  @Test
  public void testNeverExpires() {
    SessionData sessionExpired = new SessionData("1", 200, 10);
    RepositoryBackedSession rbsExpired = new RepositoryBackedSession(sessionExpired, manager, factory);
    assertTrue(rbsExpired.isExpired());
    SessionData sessionNotExpired = new SessionData("1", System.currentTimeMillis(), 10000);
    RepositoryBackedSession rbsNotExpired = new RepositoryBackedSession(sessionNotExpired, manager, factory);
    assertFalse(rbsNotExpired.isExpired());
    SessionData sessionNeverExpires = new SessionData("2", 200, 0);
    RepositoryBackedSession rbsNeverExpires = new RepositoryBackedSession(sessionNeverExpires, manager, factory);
    assertFalse(rbsNeverExpires.isExpired());
  }

  @Test
  public void testGetTimestamps() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertEquals(100, rbs.getCreationTime());
    assertEquals(100, rbs.getLastAccessedTime());
    assertEquals(10, rbs.getMaxInactiveInterval());
    assertTrue(rbs.isNew());
    SessionData sessionData2 = new SessionData("2", 200, 10, 150, null);
    RepositoryBackedSession rbs2 = new RepositoryBackedSession(sessionData2, manager, factory);
    assertEquals(150, rbs2.getCreationTime());
    assertEquals(200, rbs2.getLastAccessedTime());
    assertEquals(10, rbs2.getMaxInactiveInterval());
    assertFalse(rbs2.isNew());
  }

  @Test
  public void testReplication() {
    sessionConfiguration.setReplicationTrigger(ReplicationTrigger.SET);
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertFalse(rbs.replicateOnGet("String"));
    assertFalse(rbs.replicateOnGet(this));
    sessionConfiguration.setReplicationTrigger(ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET);
    rbs = new RepositoryBackedSession(sessionData, manager, factory);
    assertFalse(rbs.replicateOnGet("String"));
    assertTrue(rbs.replicateOnGet(this));
  }

  @Test
  public void testIsImmutable() {
    assertTrue(RepositoryBackedSession.isImmutableType(""));
    assertTrue(RepositoryBackedSession.isImmutableType(Boolean.FALSE));
    assertTrue(RepositoryBackedSession.isImmutableType(Character.valueOf(' ')));
    assertTrue(RepositoryBackedSession.isImmutableType(Integer.valueOf(1)));
    assertTrue(RepositoryBackedSession.isImmutableType(Long.valueOf(2)));
    assertTrue(RepositoryBackedSession.isImmutableType(Float.valueOf(3)));
    assertTrue(RepositoryBackedSession.isImmutableType(ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET));
    assertFalse(RepositoryBackedSession.isImmutableType(new HashSet<>()));
    assertFalse(RepositoryBackedSession.isImmutableType(mock(Object.class)));
  }

  @Test
  public void testCommit() {
    sessionData.setMandatoryRemoteKeys(Collections.singleton("NonCacheable"));
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("TestChanged", "valueOriginal");
    rbs.setAttribute("TestChanged", "valueFinal");
    rbs.setAttribute("TestToDelete", "value");
    rbs.removeAttribute("TestToDelete");
    rbs.setAttribute("NonCacheable", "valueNonCacheable");
    rbs.getCommitter().run();
    verify(repository).startCommit(sessionData("1"));
    verify(transaction).removeAttribute("TestToDelete");
    verify(transaction).addAttribute("Test", "value");
    verify(transaction).addAttribute("TestChanged", "valueFinal");
    verify(transaction, never()).addAttribute("TestChanged", "valueOriginal");
    verify(transaction, never()).addAttribute(eq("NonCacehable"), anyString());
  }

  @Test
  public void testGetAttributeNamesWithValues() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("TestChanged", "valueOriginal");
    rbs.setAttribute("TestChanged", "valueFinal");
    rbs.setAttribute("TestToDelete", "value");
    rbs.removeAttribute("TestToDelete");
    List<String> list = rbs.getAttributeNamesWithValues();
    assertThat(list, hasItems("Test", "TestChanged"));
    assertThat(list, not(hasItems("TestToDelete")));
  }

  @Test
  public void testCommitWrapped() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    RepositoryBackedSession rbsWrapped = new RepositoryBackedSession(rbs);
    rbsWrapped.checkUsedAndLock();
    rbsWrapped.getCommitter().run();
    verify(repository).startCommit(sessionData("1"));
    verify(transaction, never()).addAttribute("Test", "value");
    rbs.getCommitter().run();
    verify(repository, times(2)).startCommit(sessionData("1"));
    verify(transaction).addAttribute("Test", "value");
  }

  @Test
  public void testCommitWrappedWithAlwaysCommit() {
    sessionConfiguration.setCommitOnAllConcurrent(true);
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    RepositoryBackedSession rbsWrapped = new RepositoryBackedSession(rbs);
    rbsWrapped.checkUsedAndLock();
    rbsWrapped.getCommitter().run();
    verify(repository).startCommit(sessionData("1"));
    verify(transaction).addAttribute("Test", "value");
    rbs.getCommitter().run();
    verify(repository, times(2)).startCommit(sessionData("1"));
    verify(transaction, times(2)).addAttribute("Test", "value");
  }

  @Test
  public void testCommitFirstOriginalThenWrapped() {
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    RepositoryBackedSession rbsWrapped = new RepositoryBackedSession(rbs);
    rbsWrapped.checkUsedAndLock();
    rbs.getCommitter().run();
    verify(repository).startCommit(sessionData("1"));
    verify(transaction, never()).addAttribute("Test", "value");
    rbsWrapped.getCommitter().run();
    verify(repository, times(2)).startCommit(sessionData("1"));
    verify(transaction).addAttribute("Test", "value");
  }

  @Test
  public void testCommitDistributing() {
    sessionData.setMandatoryRemoteKeys(Collections.singleton("NonCacheable"));
    when(transaction.isDistributing()).thenReturn(true);
    RepositoryBackedSession rbs = new RepositoryBackedSession(sessionData, manager, factory);
    rbs.setAttribute("Test", "value");
    rbs.setAttribute("TestChanged", "valueOriginal");
    rbs.setAttribute("TestChanged", "valueFinal");
    rbs.setAttribute("TestToDelete", "value");
    rbs.removeAttribute("TestToDelete");
    rbs.setAttribute("NonCacheable", "valueNonCacheable");
    rbs.getCommitter().run();
    verify(notifier).attributeBeingStored(refEq(rbs), eq("Test"), any());
    verify(notifier).attributeBeingStored(refEq(rbs), eq("TestChanged"), any());
    verify(notifier, never()).attributeBeingStored(refEq(rbs), eq("NonCacheable"), any());
    verify(notifier, never()).attributeBeingStored(refEq(rbs), eq("TestToDelete"), any());
  }

  private SessionData sessionData(String id) {
    return new SessionData(id, 100, 10);
  }
}
