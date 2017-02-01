package com.amadeus.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session that can be stored in repository. The class provides services for
 * retrieving, removing and adding attributes.
 */
public class RepositoryBackedSession {
  private static final Logger logger = LoggerFactory.getLogger(RepositoryBackedSession.class);

  // True if session is no longer valid
  private boolean invalid;
  // True if session should be invalidated at commit
  private boolean invalidateOnCommit;
  // SessionData describing this ession
  private final SessionData sessionData;
  // Task responsible for committing the session
  private final Committer committer;
  // Counter of number of concurrent requests accessing this session
  private final AtomicInteger concurrentUses;
  // Set to true if this session concurrent counter has been increased
  private final AtomicBoolean lockedForUse;
  // Set to true if this session attributes should be committed even
  // when there are still running concurrent requests
  private final boolean forceCommit;
  // Associated session manager
  protected final SessionManager manager;
  // True if session is replicated on non primitive get
  private final boolean replicateOnGet;

  /**
   * Each attribute that was get from repository, removed or set during request
   * is stored in this map. Keys of the map are names of attributes, and values
   * are descriptors of the attributes.
   */
  protected final ConcurrentHashMap<String, Attribute> attrs;

  /**
   * Set to <code>true</code> if session has been changed during request since
   * last commit.
   */
  private boolean dirty;

  private boolean committed;

  private SessionFactory factory;


  /**
   * The attribute descriptor contains value of the attribute as well as flags
   * that indicate if attribute was deleted or changed. Changed attribute would
   * be sent to repository.
   */
  private static class Attribute {
    private Object value;
    /**
     * <code>true</code> if attribute has been deleted.
     */
    private boolean deleted;
    /**
     * <code>true</code> if attribute has been changed.
     */
    private boolean changed;

    /**
     * Default constructor.
     *
     * @param value
     *          the value of the attribute
     */
    public Attribute(Object value) {
      this.value = value;
    }
  }

  /**
   * Creates session based on given {@link SessionData} using
   * {@link SessionManager} to manage lifecycle of session.
   *
   * @param sessionData
   *          the session descriptor
   * @param manager
   *          the associated session manager
   * @param factory
   *          the session factory
   */
  public RepositoryBackedSession(SessionData sessionData, SessionManager manager, SessionFactory factory) {
    this.sessionData = sessionData;
    this.manager = manager;
    this.factory = factory;
    committed = false;
    concurrentUses = new AtomicInteger();
    lockedForUse = new AtomicBoolean();
    committer = new Committer();
    attrs = new ConcurrentHashMap<>();
    replicateOnGet = manager.getConfiguration().getReplicationTrigger().isReplicateOnGet();
    forceCommit = manager.getConfiguration().isCommitOnAllConcurrent();
  }

  protected RepositoryBackedSession(RepositoryBackedSession linked) {
    attrs = linked.attrs;
    concurrentUses = linked.concurrentUses;
    lockedForUse = new AtomicBoolean();
    manager = linked.getSessionManager();
    sessionData = linked.sessionData;
    factory = linked.factory;
    committed = false;
    committer = new Committer();
    replicateOnGet = manager.getConfiguration().getReplicationTrigger().isReplicateOnGet();
    forceCommit = manager.getConfiguration().isCommitOnAllConcurrent();
  }

  /**
   * Sets inactivity interval before session is invalidated. Value is in seconds
   *
   * @param maxInactiveInterval
   *          inactivity interval in seconds before session is invalidated
   */
  public void setMaxInactiveInterval(int maxInactiveInterval) {
    sessionData.setMaxInactiveInterval(maxInactiveInterval);
  }

  /**
   * Returns value of the attribute
   *
   * @param key
   *          name of the attribute
   * @return value of the attribute
   */
  public Object getAttribute(String key) {
    assertValid();
    Attribute attr = retrieveAttribute(key, getAttributeFromCache(key));
    if (attr == null || attr.deleted) {
      return null;
    }

    // If we do get on non simple type, and we have replicate on get, we should
    // replicate the attribute.
    if (replicateOnGet(attr.value)) {
      attr.changed = true;
      dirty = true;
      checkUsedAndLock();
    }
    return attr.value;
  }

  /**
   * Returns <code>true</code> if attribute should be replicated on getAttribute
   * operation.
   *
   * @param obj
   *          object to test
   * @return <code>true</code> if attribute should be replicated
   */
  boolean replicateOnGet(Object obj) {
    return replicateOnGet && !isImmutableType(obj);
  }

  /**
   * Returns <code>true</code> if object is known immutable type. The standard
   * immutable types are {@link String}, {@link Boolean}, {@link Character} and
   * the types derived from {@link Enum} and {@link Number}.
   *
   * @param obj
   *          object to test
   * @return <code>true</code> if object is known immutable type
   */
  static boolean isImmutableType(Object obj) {
    return obj instanceof Number || obj instanceof Character || obj instanceof String || obj instanceof Boolean
        || obj instanceof Enum;
  }

  /**
   * Retrieves list of all known attributes. It includes both those known
   * locally and the ones from repostiory.
   *
   * @return enumeration of all known attributes
   */
  @SuppressWarnings({ "rawtypes" })
  public Enumeration getAttributeNames() {
    assertValid();
    List<String> keys = getAttributeNamesWithValues();
    for (String key : getAllRepositoryKeys()) {
      // If key isn't already in local cache, add it to enumeration.
      if (!attrs.containsKey(key)) {
        keys.add(key);
      }
    }
    return Collections.enumeration(keys);
  }

  /**
   * Used internally to get attribute names that have values
   *
   * @return list containing all loaded attributes with values
   */
  public List<String> getAttributeNamesWithValues() {
    ArrayList<String> keys = new ArrayList<>(attrs.size());
    for (Map.Entry<String, Attribute> entry : attrs.entrySet()) {
      if (entry.getValue().value != null) {
        keys.add(entry.getKey());
      }
    }
    return keys;
  }

  /**
   * Returns the time when this session was created, measured in milliseconds
   * since midnight January 1, 1970 GMT.
   *
   * @return a <code>long</code> specifying when this session was created,
   *         expressed in milliseconds since 1/1/1970 GMT
   *
   * @exception IllegalStateException
   *              if this method is called on an invalidated session
   */
  public long getCreationTime() {
    assertValid();
    return sessionData.getCreationTime();
  }

  /**
   * Returns a string containing the unique identifier assigned to this session.
   * The identifier can be customized but usually it is a {@link UUID}.
   *
   * @return a string specifying the identifier assigned to this session
   *
   * @exception IllegalStateException
   *              if this method is called on an invalidated session
   */
  public String getId() {
    assertValid();
    return sessionData.getId();
  }

  /**
   * Returns the last time the client sent a request associated with this
   * session, as the number of milliseconds since midnight January 1, 1970 GMT,
   * and marked by the time the request was received.
   *
   * <p>
   * Actions that your application takes, such as getting or setting a value
   * associated with the session, do not affect the access time.
   *
   * @return a <code>long</code> representing the last time the client sent a
   *         request associated with this session, expressed in milliseconds
   *         since 1/1/1970 GMT
   *
   * @exception IllegalStateException
   *              if this method is called on an invalidated session
   */
  public long getLastAccessedTime() {
    assertValid();
    return sessionData.getLastAccessedTime();
  }

  /**
   * Returns maximum inactive interval for the session in seconds.
   *
   * @return maximum inactive interval for the session in seconds.
   */
  public int getMaxInactiveInterval() {
    assertValid();
    return sessionData.getMaxInactiveInterval();
  }

  /**
   * Invalidates this session then unbinds any objects bound to it.
   *
   * @exception IllegalStateException
   *              if this method is called on an already invalidated session
   */
  public void invalidate() {
    assertValid();
    doInvalidate(false);
  }

  /**
   * This method performs invalidation of the session. If it was called on
   * session expire event, invalidation will happen at the session commit time.
   * If it was called from {@link #invalidate()} method, invalidation is
   * effective immediately.
   *
   * @param expired
   *          set to <code>true</code> if invalidate is due to expiring
   */
  public void doInvalidate(boolean expired) {
    boolean canRemove = false;
    try {
      if (!invalid) {
        canRemove = invalidateOrNotify(expired);
      }
    } finally {
      if (!invalidateOnCommit) {
        finishInvalidation(canRemove);
      }
    }
  }

  /**
   * Prepares session for removal, invalidate it or notifies 
   * manager if there was conflict (e.g. concurrent access)
   * during invalidation.
   * 
   * @param expired 
   *          <code>true</code> if session has expired
   * @return <code>true</code> if session can be removed as there was no conflict
   */
  private boolean invalidateOrNotify(boolean expired) {
    boolean canRemove;
    canRemove = manager.getRepository().prepareRemove(getSessionData());
    if (canRemove) {
      if (expired && isUsed()) {
        invalidateOnCommit = true;
      } else {
        invalidateOnCommit = false;
        wipeInvalidSession();
      }
    } else {
      manager.invalidationConflict(this, expired);
    }
    return canRemove;
  }

  /**
   * Returns <code>true</code> if session is used by at least on request
   *
   * @return <code>true</code> if session is used by at least on request
   */
  private boolean isUsed() {
    return concurrentUses.get() > 0;
  }

  /**
   * Cleans all attributes in session
   */
  void wipeInvalidSession() {
    loadAllAttributes();
    manager.getNotifier().sessionDestroyed(this, false);
    attrs.clear();
  }

  /**
   * Terminates session invalidation.
   *
   * @param canRemove
   *          identifies if session can be removed from repository
   */
  private void finishInvalidation(boolean canRemove) {
    invalid = true;
    if (canRemove) {
      manager.getRepository().remove(sessionData);
    }
  }

  /**
   * Gets keys of all attributes stored in repository
   *
   * @return set containing keys of all attributes stored in repository
   */
  private Set<String> getAllRepositoryKeys() {
    Set<String> set = sessionData.getRepositoryKeys();
    if (set == null) {
      set = manager.getRepository().getAllKeys(sessionData);
      sessionData.setRepositoryKeys(set);
    }
    return set;
  }

  /**
   * Loads all attributes from repository.
   */
  private void loadAllAttributes() {
    for (String key : getAllRepositoryKeys()) {
      if (getAttributeFromCache(key) == null) {
        retrieveAttribute(key, null);
      }
    }
  }

  /**
   * Loads attribute from repository. Request to remote repository is done once
   * per request at most once between initial retrieval of session data from
   * repository and the call to {@link #commit()}.
   *
   * @param key
   *          the name of the attribute
   * @param attribute
   *          the known value of the attribute
   * @return object representing attribute or <code>null</code> if not present
   */
  private Attribute retrieveAttribute(String key, Attribute attribute) {
    // If the attribute is non-cachable or null and possibly in remote
    // repository, then we need to retrieve it
    Attribute attr = attribute;
    if (attr != null && !sessionData.isNonCacheable(key)) {
      return attr;
    } else if (attr == null && !sessionData.isMaybeInRepository(key)) {
      return null;
    }
    Object value = manager.getRepository().getSessionAttribute(sessionData, key);
    if (attr == null) {
      attr = new Attribute(value);
      attrs.put(key, attr);
    } else {
      attr.value = value;
    }
    if (value != null) {
      manager.getNotifier().attributeHasBeenRestored(this, key, value);
    }
    return attr;
  }

  /**
   * Returns <code>true</code> if the client does not yet know about the session
   * or if the client chooses not to join the session. For example, if an HTTP
   * server used only cookie-based sessions, and the client had disabled the use
   * of cookies, then a session would be new on each request.
   *
   * @return <code>true</code> if the server has created a session, but the
   *         client has not yet joined
   *
   * @exception IllegalStateException
   *              if this method is called on an already invalidated session
   */
  public boolean isNew() {
    assertValid();
    return sessionData.isNew();
  }

  /**
   * Removes the object bound with the specified name from this session. If the
   * session does not have an object bound with the specified name, this method
   * does nothing.
   *
   * <p>
   * After this method executes, it calls
   * {@link SessionNotifier#attributeRemoved(RepositoryBackedSession, String, Object)}
   * .
   * </p>
   *
   * @param name
   *          the name of the object to remove from this session
   *
   * @exception IllegalStateException
   *              if this method is called on an invalidated session
   */
  public void removeAttribute(String name) {
    assertValid();
    Attribute attr = getAttributeFromCache(name);
    if (attr == null) {
      attr = retrieveAttribute(name, null);
      if (attr == null) {
        return;
      }
    }
    if (sessionData.isNonCacheable(name)) {
      manager.getRepository().removeSessionAttribute(sessionData, name);
    }
    Object oldValue = attr.value;
    attr.value = null;
    attr.deleted = true;
    attr.changed = true;
    dirty = true;
    checkUsedAndLock();
    // Trigger the removal and binding events
    if (oldValue != null) {
      manager.getNotifier().attributeRemoved(this, name, oldValue);
    }
  }

  /**
   * Retrieves attribute descriptor from internal cache.
   *
   * @param name
   *          the name of the attribute
   * @return attribute or <code>null</code> if not present
   */
  public Attribute getAttributeFromCache(String name) {
    return attrs.get(name);
  }

  /**
   * Binds an object to this session, using the name specified. If an object of
   * the same name is already bound to the session, the object is replaced.
   *
   * <p>
   * After this method executes, and if the attribute was replaced the method
   * calls
   * {@link SessionNotifier#attributeReplaced(RepositoryBackedSession, String, Object)}
   * , and if it was new, the method calls
   * {@link SessionNotifier#attributeAdded(RepositoryBackedSession, String, Object)}
   * .
   * </p>
   *
   * <p>
   * If the value passed in is null, this has the same effect as calling
   * <code>removeAttribute()</code>.
   * </p>
   *
   * @param key
   *          the name to which the object is bound; cannot be null
   *
   * @param value
   *          the object to be bound
   *
   * @exception IllegalStateException
   *              if this method is called on an invalidated session
   */
  public void setAttribute(String key, Object value) {
    assertValid();
    if (value == null) {
      removeAttribute(key);
      return;
    }
    Object oldValue;
    Attribute attr;
    if (sessionData.isNonCacheable(key)) {
      manager.getRepository().setSessionAttribute(sessionData, key, value);
      attr = getAttributeFromCache(key);
    } else {
      attr = retrieveAttribute(key, getAttributeFromCache(key));
    }
    if (attr == null) {
      attr = new Attribute(value);
      attrs.put(key, attr);
      oldValue = null;
    } else {
      oldValue = attr.value;
      attr.value = value;
      attr.deleted = false;
    }
    attr.changed = true;
    dirty = true;
    checkUsedAndLock();
    if (oldValue != value) { // NOSONAR identity check
      // Trigger the replace events
      if (oldValue != null) {
        manager.getNotifier().attributeReplaced(this, key, oldValue);
      }
      // Trigger the add events
      manager.getNotifier().attributeAdded(this, key, value);
    }
  }

  /**
   * Throws {@link IllegalStateException} if session is not valid.
   */
  private void assertValid() {
    if (invalid) {
      throw new IllegalStateException("Session with id " + sessionData.getId()
          + " is invalid. Operation is not allowed. For information session data is " + sessionData);
    }
  }

  /**
   * Stores session to session repository. Called when request is completed. If
   * session is invalid call has no effect.
   */
  public synchronized void commit() {
    if (!invalid) {
      manager.invokeCommit(this);
    }
  }

  /**
   * If session has been modified, but didn't add lock to concurrentUses, this
   * method will increase it.
   *
   * @return <code>true</code> if session has been changed
   */
  boolean checkUsedAndLock() {
    boolean used = !isCommitted() || dirty;
    if (used && lockedForUse.compareAndSet(false, true)) {
      concurrentUses.incrementAndGet();
    }
    return used;
  }

  /**
   * This class implements logic that commits session to
   * {@link SessionRepository}. The logic allows atomic commit if repository
   * supports it. The algorithm is as follows:
   * <p>
   * Call {@link SessionRepository#startCommit(SessionData)} to initiate commit
   * transaction. Verify if session is in used and is locked. Only such sessions
   * need to be committed. Unlock the session. If the session is not last
   * concurrent session request, then we don't need to reset internal attribute
   * flags indicating that they become same as the ones in repository. If the
   * session is the last concurrent use (i.e. no other requests access session
   * concurrently), or if {@link SessionConfiguration#COMMIT_ON_ALL_CONCURRENT}
   * was set to <code>true</code>, the changed and deleted session attributes
   * are indeed updated in repository. Add changed and/or deleted attributes,
   * notify listeners that the session is being stored in repository.
   * </p>
   */
  class Committer implements Runnable {
    @Override
    public void run() {
      if (checkUsedAndLock()) {
        // Unlock the session and reduce the counter
        boolean lastSession = unlockSession();
        boolean keepChangedFlag = !lastSession;
        boolean commitAttributes = lastSession || forceCommit;
        if (lastSession && invalidateOnCommit) {
          invalidationOnCommit();
        } else {
          storeToRepository(commitAttributes, keepChangedFlag);
        }
        committed();
        dirty = false;
        logger.info("Committed session: {}", sessionData);
      } else {
        logger.debug("Nothing to commit for session: {}", sessionData);
      }
    }

    
    /**
     * Unlocks the session and returns <code>true</code> if it was last active
     * session.
     *
     * @return <code>true</code> if it was last active session
     */
    private boolean unlockSession() {
      if (lockedForUse.compareAndSet(true, false)) {
        return concurrentUses.decrementAndGet() == 0;
      }
      return false;
    }

    /**
     * Called when session become invalid before committing. I.e. if expiration
     * event was received.
     */
    private void invalidationOnCommit() {
      try {
        wipeInvalidSession();
      } finally {
        finishInvalidation(true);
      }
    }

    /**
     * Stores session into the repository.
     *
     * @param commitAttributes
     *          <code>true</code> if attributes should be committed
     * @param keepChangedFlag
     *          <code>true</code> if internal flags indicating attribute was
     *          changed should be kept unchanged
     */
    void storeToRepository(boolean commitAttributes, boolean keepChangedFlag) {
      SessionRepository.CommitTransaction transaction = manager.getRepository().startCommit(sessionData);
      logger.debug("Committing session: {}", sessionData);

      if (commitAttributes) {
        commitAttributes(transaction, keepChangedFlag);
      }
      transaction.commit();
    }

    /*
     * Commit attributes into repository
     */
    private void commitAttributes(SessionRepository.CommitTransaction transaction, boolean keepChangedFlag) {
      for (Map.Entry<String, Attribute> entry : attrs.entrySet()) {
        if (sessionData.isNonCacheable(entry.getKey())) {
          // Skip attributes that are always repository backed
          continue;
        }
        Attribute attr = entry.getValue();
        if (attr.changed || transaction.isSetAllAttributes()) {
          removeOrAddAttribute(transaction, entry.getKey(), attr);
          if (!keepChangedFlag) {
            attr.changed = false;
          }
        }
      }
    }

    /*
     * Removes delete attributes or adds changed/added attribute to session
     * update transaction.
     */
    private void removeOrAddAttribute(SessionRepository.CommitTransaction transaction,
        String key, Attribute attr) {
      if (attr.deleted) {
        transaction.removeAttribute(key);
      } else {
        transaction.addAttribute(key, attr.value);
        if (transaction.isDistributing()) {
          manager.getNotifier().attributeBeingStored(RepositoryBackedSession.this, key, attr.value);
        }
      }
    }
  }

  /**
   * Called when the session was committed.
   */
  protected void committed() {
    setCommitted(true);
    factory.committed(this);
  }

  /**
   * Returns <code>true</code> if session is valid. Session becomes invalid
   * following call to {@link #invalidate()} method.
   *
   * @return <code>true</code> if session is valid
   */
  public boolean isValid() {
    return !invalid;
  }

  /**
   * Returns <code>true</code> if session has expired. Session is expired if
   * current time is less then sum of {@link #getLastAccessedTime()} and max
   *
   * @return <code>true</code> if session has expired
   */
  public boolean isExpired() {
    int maxInactiveInterval = sessionData.getMaxInactiveInterval();
    if (maxInactiveInterval <= 0) {
      return false;
    }
    long instanceOfExpiring = sessionData.getLastAccessedTime() + TimeUnit.SECONDS.toMillis(maxInactiveInterval);
    return instanceOfExpiring < System.currentTimeMillis();
  }

  /**
   * Set to <code>true</code> if session has been changed during request since
   * last commit.
   *
   * @return <code>true</code> if session has been changed
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Returns committer that can be called to commit session. Committer is a
   * {@link Runnable} instance. See {@link Committer}
   *
   * @return a {@link Committer} for this session
   */
  public Runnable getCommitter() {
    return committer;
  }

  /**
   * Returns descriptor of this session (see {@link SessionData}).
   *
   * @return the session descriptor
   */
  public SessionData getSessionData() {
    return sessionData;
  }

  /**
   * Returns <code>true</code> if session was committed during request
   *
   * @return <code>true</code> if session was committed during request
   */
  public boolean isCommitted() {
    return committed;
  }

  /**
   * Sets the flag indicating if session was committed.
   *
   * @param committed
   *          flag indicating if session was committed
   */
  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  /**
   * Returns {@link SessionManager} associated to this http session.
   *
   * @return {@link SessionManager} associated to this http session.
   */
  public SessionManager getSessionManager() {
    return manager;
  }

  /**
   * @return the concurrentUses
   */
  public int getConcurrentUses() {
    return concurrentUses.get();
  }
}
