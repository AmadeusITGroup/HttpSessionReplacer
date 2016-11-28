package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.codahale.metrics.MetricRegistry;

@SuppressWarnings("javadoc")
public class TestSessionManager {
  private ExecutorFacade executors;
  private SessionFactory factory;
  private SessionRepository repository;
  private SessionTracking tracking;
  private SessionNotifier notifier;
  private SessionConfiguration configuration;
  private SessionManager sessionManager;
  private MetricRegistry metrics;

  @Before
  public void setup() {
    executors = mock(ExecutorFacade.class);
    factory = mock(SessionFactory.class);
    repository = mock(SessionRepository.class);
    tracking = mock(SessionTracking.class);
    notifier = mock(SessionNotifier.class);
    configuration = new SessionConfiguration();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    sessionManager = new SessionManager(executors, factory, repository, tracking, notifier, configuration, classLoader);
    metrics = sessionManager.getMetrics();
  }

  @After
  public void shutdown() {
    sessionManager.close();
  }

  @Test
  public void testGetSessionFromPropagatorId() {
    SessionData sessionData = new SessionData("1", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getId()).thenReturn("1");
    when(repository.getSessionData("1")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    RequestWithSession request = mock(RequestWithSession.class);
    when(tracking.retrieveId(request)).thenReturn("1");
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(request).setRequestedSessionId("1");
    assertSame(session, retrievedSession);
  }

  @Test
  public void testGetSessionNoSessionId() {
    RequestWithSession request = mock(RequestWithSession.class);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(request).setRequestedSessionId(null);
    assertNull(retrievedSession);
  }

  @Test
  public void testGetSessionNoSessionIdCreate() {
    RequestWithSession request = mock(RequestWithSession.class);
    when(tracking.newId()).thenReturn("new-id");
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getId()).thenReturn("new-id");
    when(factory.build(any(SessionData.class))).thenReturn(session);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, true, null);
    verify(request).setRequestedSessionId(null);
    verify(tracking).newId();
    assertSame(session, retrievedSession);
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "created")).getCount());
  }

  @Test
  public void testGetSessionFromRequestedId() {
    SessionData sessionData = new SessionData("2", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getId()).thenReturn("2");
    when(repository.getSessionData("2")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    RequestWithSession request = mock(RequestWithSession.class);
    when(request.getRequestedSessionId()).thenReturn("2");
    when(request.isIdRetrieved()).thenReturn(true);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(tracking, never()).retrieveId(request);
    assertSame(session, retrievedSession);
  }

  @Test
  public void testGetSessionExpiredValid() {
    SessionData sessionData = new SessionData("2", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getId()).thenReturn("2");
    when(session.isExpired()).thenReturn(true);
    when(session.isValid()).thenReturn(true);
    when(repository.getSessionData("2")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    RequestWithSession request = mock(RequestWithSession.class);
    when(request.getRequestedSessionId()).thenReturn("2");
    when(request.isIdRetrieved()).thenReturn(true);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(session).doInvalidate(true);
    assertNull(retrievedSession);
  }

  @Test
  public void testGetSessionExpiredInvalid() {
    SessionData sessionData = new SessionData("2", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getId()).thenReturn("2");
    when(session.isExpired()).thenReturn(true);
    when(session.isValid()).thenReturn(false);
    when(repository.getSessionData("2")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    RequestWithSession request = mock(RequestWithSession.class);
    when(request.getRequestedSessionId()).thenReturn("2");
    when(request.isIdRetrieved()).thenReturn(true);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(session, never()).doInvalidate(true);
    assertNull(retrievedSession);

  }
  @Test
  public void testGetSessionIdNotExists() {
    RequestWithSession request = mock(RequestWithSession.class);
    when(request.getRequestedSessionId()).thenReturn("2");
    when(request.isIdRetrieved()).thenReturn(true);
    RepositoryBackedSession retrievedSession = sessionManager.getSession(request, false, null);
    verify(tracking, never()).retrieveId(request);
    verify(repository).getSessionData("2");
    assertNull(retrievedSession);
  }

  @Test
  public void testPropagateSession() {
    RequestWithSession request = mock(RequestWithSession.class);
    ResponseWithSessionId response = mock(ResponseWithSessionId.class);
    sessionManager.propagateSession(request, response);
    verify(tracking).propagateSession(request, response);
    verify(request).setAttribute(SessionManager.SESSION_PROPAGATED, Boolean.TRUE);
  }

  @Test
  public void testPropagateSessionAlreadyPropagated() {
    RequestWithSession request = mock(RequestWithSession.class);
    ResponseWithSessionId response = mock(ResponseWithSessionId.class);
    when(request.getAttribute(SessionManager.SESSION_PROPAGATED)).thenReturn(Boolean.TRUE);
    sessionManager.propagateSession(request, response);
    verify(tracking, never()).propagateSession(request, response);
    verify(request, never()).setAttribute(SessionManager.SESSION_PROPAGATED, Boolean.TRUE);
  }

  @Test
  public void testDelete() {
    SessionData sessionData = new SessionData("1", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(repository.getSessionData("1")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    sessionManager.delete("1", true);
    verify(repository).getSessionData("1");
    verify(factory).build(sessionData);
    verify(session).doInvalidate(true);
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "deleted")).getCount());
  }

  @Test
  public void testDeleteExpired() {
    when(repository.getSessionData("1")).thenReturn(null);
    sessionManager.delete("1", true);
    verify(repository).getSessionData("1");
    verify(factory, never()).build(any(SessionData.class));
    assertEquals(0, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "deleted")).getCount());
  }

  private long now() {
    return System.currentTimeMillis();
  }

  @Test
  public void testRequestFinished() {
    sessionManager.requestFinished();
    verify(repository).requestFinished();
  }

  @Test
  public void testSubmit() {
    Runnable runnable = mock(Runnable.class);
    sessionManager.submit(null, runnable);
    verify(executors).submit(runnable);
  }

  @Test
  public void testSubmitWithTimer() {
    Runnable runnable = mock(Runnable.class);
    sessionManager.submit("test", runnable);
    ArgumentCaptor<SessionManager.RunnableWithTimer> arg= ArgumentCaptor.forClass(SessionManager.RunnableWithTimer.class);
    verify(executors).submit(arg.capture());
    assertNotNull(arg.getValue().timer);
    assertSame(runnable, arg.getValue().task);
  }

  @Test
  public void testSchedule() {
    Runnable runnable = mock(Runnable.class);
    sessionManager.schedule(null, runnable , 10);
    verify(executors).scheduleAtFixedRate(runnable, 10L, 10L, TimeUnit.SECONDS);
  }

  @Test
  public void testScheduleWithTimer() {
    Runnable runnable = mock(Runnable.class);
    sessionManager.schedule("test", runnable , 10);
    ArgumentCaptor<SessionManager.RunnableWithTimer> arg= ArgumentCaptor.forClass(SessionManager.RunnableWithTimer.class);
    verify(executors).scheduleAtFixedRate(arg.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
    assertNotNull(arg.getValue().timer);
    assertSame(runnable, arg.getValue().task);
  }

  @Test
  public void testDeleteAsync() {
    sessionManager.deleteAsync("1", true);
    ArgumentCaptor<SessionManager.RunnableWithTimer> captor = ArgumentCaptor.forClass(SessionManager.RunnableWithTimer.class);
    verify(executors).submit(captor.capture());
    assertNotNull(captor.getValue().timer);
    SessionData sessionData = new SessionData("1", now(), 10);
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(repository.getSessionData("1")).thenReturn(sessionData);
    when(factory.build(sessionData)).thenReturn(session);
    captor.getValue().task.run();
    verify(repository).getSessionData("1");
    verify(factory).build(sessionData);
    verify(session).doInvalidate(true);
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "deleted")).getCount());
  }

  @Test
  public void testInvokeCommit() {
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    when(session.getCommitter()).thenReturn(mock(Runnable.class));
    sessionManager.invokeCommit(session);
    verify(session).getCommitter();
  }

  @Test
  public void testInvalidationConflict() {
    assertEquals(0, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors")).getCount());
    assertEquals(0, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors", "expiry")).getCount());
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    sessionManager.invalidationConflict(session, true);
    assertEquals(0, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors")).getCount());
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors", "expiry")).getCount());
    sessionManager.invalidationConflict(session, false);
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors")).getCount());
    assertEquals(1, metrics.meter(MetricRegistry.name(SessionManager.SESSIONS_METRIC_PREFIX, "invalidation", "errors", "expiry")).getCount());
  }

  @Test
  public void testSessionSwitch() {
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    SessionData sessionData = mock(SessionData.class);
    when(session.getSessionData()).thenReturn(sessionData);
    sessionManager.switchSessionId(session);
    verify(sessionData).isIdChanged();
    verify(sessionData).setNewSessionId(any(String.class));
    verify(repository).sessionIdChange(sessionData);
    verify(notifier).sessionIdChanged(session, sessionData.getOldSessionId());
  }


  @Test
  public void testPreventDoubleSessionSwitch() {
    RepositoryBackedSession session = mock(RepositoryBackedSession.class);
    SessionData sessionData = mock(SessionData.class);
    when(session.getSessionData()).thenReturn(sessionData);
    when(sessionData.isIdChanged()).thenReturn(true);
    sessionManager.switchSessionId(session);
    verify(sessionData).isIdChanged();
    verify(sessionData, never()).setNewSessionId(any(String.class));
    verify(repository, never()).sessionIdChange(sessionData);
    verify(notifier, never()).sessionIdChanged(session, sessionData.getOldSessionId());
  }
}
