package com.amadeus.session.repository.inmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionRepository;

/**
 * Session Repository implementation that stores session in memory. This class
 * can be used for testing and development purposes. This repository will also
 * be used for the web apps marked as non-distributable when distribution of 
 * sessions is not enforced.
 * <p>
 * We are storing session in memory in {@link ConcurrentHashMap}. For each
 * session we store {@link SessionData} and session attributes. Multiple threads
 * can access both separate session and same session id, and while repository is
 * thread safe, functional concurrency when using same session id from different
 * threads must be assured in application code (as is general case for
 * {@link HttpSession}.
 * </p>
 * <p>
 * Sessions are cleaned-up by a special task running in separate thread every 60
 * seconds. Only one task is running at the given time and this is assured by
 * {@link SessionManager#schedule(String, Runnable, long)} method.
 * </p>
 */
public class InMemoryRepository implements SessionRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryRepository.class);

  ConcurrentHashMap<String, SessionData> sessionDataCache = new ConcurrentHashMap<>();
  ConcurrentHashMap<String, Map<String, Object>> sessionAttributeCache = new ConcurrentHashMap<>();
  private SessionManager sessionManager;
  private final String namespace;

  private ScheduledFuture<?> cleanupFuture;

  /**
   * Constructor for in-memory repository.
   *
   * @param namespace
   *          the namespace of sessions stored in this repository
   */
  public InMemoryRepository(String namespace) {
    this.namespace = namespace;
  }

  private void remove(String sessionId) {
    String id = id(sessionId);
    sessionDataCache.remove(id);
    sessionAttributeCache.remove(id);
  }

  private String id(String id) {
    return new StringBuilder(namespace.length() + 1 + id.length()).append(namespace).append(':').append(id).toString();
  }

  @Override
  public SessionData getSessionData(String id) {
    return sessionDataCache.get(id(id));
  }

  @Override
  public void storeSessionData(SessionData sessionData) {
    String id = id(sessionData.getId());
    sessionDataCache.put(id, sessionData);
    sessionAttributeCache.putIfAbsent(id, new ConcurrentHashMap<String, Object>());
  }

  @Override
  public Set<String> getAllKeys(SessionData session) {
    Map<String, Object> attributes = sessionAttributeCache.get(id(session.getId()));
    if (attributes != null) {
      return attributes.keySet();
    }
    return Collections.emptySet();
  }

  @Override
  public Object getSessionAttribute(SessionData session, String attribute) {
    Map<String, Object> attributes = sessionAttributeCache.get(id(session.getId()));
    if (attributes != null) {
      return attributes.get(attribute);
    }
    return null;
  }

  @Override
  public void remove(SessionData session) {
    remove(session.getId());
  }

  @Override
  public boolean prepareRemove(SessionData session) {
    sessionDataCache.remove(id(session.getId()));
    return true;
  }

  @Override
  public SessionRepository.CommitTransaction startCommit(SessionData session) {
    return new InMemoryRepository.Transaction(session);
  }

  /**
   * Cleanup task removes expired sessions from memory store.
   */
  final class CleanupTask implements Runnable {

    @Override
    public void run() {
      long instant = System.currentTimeMillis();

      try {
        HashSet<String> toRemove = new HashSet<>();
        logger.debug("Sessions in cache {} for {}", sessionDataCache, sessionManager);
        for (SessionData sd : sessionDataCache.values()) {
          if ((instant - sd.getLastAccessedTime()) > TimeUnit.SECONDS.toMillis(sd.getMaxInactiveInterval())) {
            toRemove.add(sd.getId());
          }
        }
        for (String id : toRemove) {
          sessionManager.delete(id, true);
          logger.debug("Expiring session with key {}", id);
          remove(id);
        }
        if (!toRemove.isEmpty()) {
          logger.info("At {} for {} expired sessions {}", instant, sessionManager, toRemove);
        }
      } catch (Exception e) { // NOSONAR - recover from any exception
        logger.error("An error occured while trying to exipre sessions.", e);
      }
    }
  }

  /**
   * The {@link SessionRepository.CommitTransaction} implementation that stores
   * all changed and removes all removed attributes into the
   * {@link InMemoryRepository} story.
   */
  private class Transaction implements SessionRepository.CommitTransaction {
    ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    Map<String, Object> toRemove = new ConcurrentHashMap<>();
    private SessionData session;

    Transaction(SessionData session) {
      this.session = session;
    }
    
    @Override
    public void addAttribute(String key, Object value) {
      if (value == null) {
        toRemove.put(key, key);
      } else {
        attributes.put(key, value);
      }
    }

    @Override
    public void removeAttribute(String key) {
      toRemove.put(key, key);
    }

    @Override
    public void commit() {
      String id = id(session.getId());
      SessionData sessionData = sessionDataCache.get(id);
      if (sessionData == null) {
        sessionData = new SessionData(session.getId(), session.getLastAccessedTime(),
            session.getMaxInactiveInterval(),
            session.getCreationTime(), null);
      }
      sessionData.setLastAccessedTime(session.getLastAccessedTime());
      sessionData.setMaxInactiveInterval(session.getMaxInactiveInterval());
      sessionDataCache.put(id, sessionData);
      Map<String, Object> attrs = getAttributeMap(session.getId());
      attrs.putAll(attributes);
      for (String key : toRemove.keySet()) {
        attrs.remove(key);
      }
    }

    @Override
    public boolean isSetAllAttributes() {
      return false;
    }

    @Override
    public boolean isDistributing() {
      return false;
    }
  }

  @Override
  public void setSessionManager(final SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    // We are scheduling task here to cleanup expired sessions
    // Note that this will go through all the sessions, so the performance may
    // suffer when there is a large number of sessions to go through.
    cleanupFuture = sessionManager.schedule("in-memory-cleanup", new CleanupTask(), TimeUnit.MINUTES.toSeconds(1));
  }

  @Override
  public void requestFinished() {
    // No cleanup necessary
  }

  @Override
  public void setSessionAttribute(SessionData session, String name, Object value) {
    getAttributeMap(session.getId()).put(name, value);
  }

  private Map<String, Object> getAttributeMap(String sessionId) {
    String id = id(sessionId);
    Map<String, Object> attrs = sessionAttributeCache.get(id);
    if (attrs == null) {
      attrs = new ConcurrentHashMap<>();
      Map<String, Object> attrPrev = sessionAttributeCache.putIfAbsent(id, attrs);
      if (attrPrev != null) {
        attrs = attrPrev;
      }
    }
    return attrs;
  }

  @Override
  public void removeSessionAttribute(SessionData session, String name) {
    getAttributeMap(session.getId()).remove(name);
  }

  @Override
  public boolean cleanSessionsOnShutdown() {
    return true;
  }

  @Override
  public Collection<String> getOwnedSessionIds() {
    ArrayList<String> list = new ArrayList<>(sessionDataCache.size());
    for (SessionData sd : sessionDataCache.values()) {
      list.add(sd.getId());
    }
    return Collections.unmodifiableCollection(list);
  }

  @Override
  public void close() {
    if (cleanupFuture != null) {
      cleanupFuture.cancel(true);
      cleanupFuture = null;
    }
  }

  @Override
  public void reset() {
	  close();
  }

  @Override
  public void sessionIdChange(SessionData sessionData) {
    String id = id(sessionData.getId());
    String oldId = id(sessionData.getOriginalId());
    SessionData originalSessionData = sessionDataCache.get(oldId);
    if (originalSessionData != null) {
      sessionDataCache.put(id, originalSessionData);
      sessionDataCache.remove(oldId);
      originalSessionData.setNewSessionId(sessionData.getId());
    }
    Map<String, Object> attributes = sessionAttributeCache.remove(oldId);
    if (attributes != null) {
      sessionAttributeCache.put(id, attributes);
      sessionAttributeCache.remove(oldId);
    }
  }

  @Override
  public boolean isConnected() {
    return true;
  }
}
