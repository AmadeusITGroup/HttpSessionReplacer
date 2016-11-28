package com.amadeus.session;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Container for basic session information. It includes the following standard
 * data uses for <code>HttpSession</code> in JEE:
 * <ul>
 * <li>creation time as timestamp</li>
 * <li>last accesses time as timestamp</li>
 * <li>max inactive interval in seconds</li>
 * <li>whether the session is a new one</li>
 * </ul>
 *
 * It also includes additional information:
 * <ul>
 * <li>last accessed time at moment of the retrieval from repository. This can
 * be used to manage expiration strategy - e.g. as we now have new access time,
 * we can clean timers that are based on this old session accessed time.</li>
 * <li>keys that are known to be stored in repository. For attributes that are
 * in repository, we can retrieve them when we need to trigger notification
 * during a request.</li>
 * <li>keys of attributes that must be kept synchronized with repository. Any
 * change to such attribute is immediately propagated to repository.</li>
 * <li>the id of node that previously owned the session (may be the same
 * as current</li>
 * <li>old session id if session id was changed</li>
 * </ul>
 *
 */
public class SessionData {
  private String id;
  private String oldSessionId;
  private long creationTime;
  private long lastAccessedTime;
  private int maxInactiveInterval;
  private Set<String> repositoryKeys;
  private Set<String> mandatoryRemoteKeys;
  private boolean isNew;
  private final long originalLastAccessed;
  private final String previousOwner;

  /**
   * Constructor with session id, last epoch time of access, maximum inactivity
   * interval, creation time (from epoch) and node id of previous owner.
   *
   * @param id
   *          the session id
   * @param lastAccessedTime
   *          timestamp when the session was previously accessed
   * @param maxInactiveInterval
   *          maximum inactivity interval in seconds
   * @param creationTime
   *          timestamp of the session creation
   * @param previousOwner
   *          the node that was previous owner of the session
   */
  public SessionData(String id, long lastAccessedTime, int maxInactiveInterval, long creationTime,
      String previousOwner) {
    this.id = id;
    this.originalLastAccessed = lastAccessedTime;
    this.maxInactiveInterval = maxInactiveInterval;
    this.lastAccessedTime = lastAccessedTime;
    this.creationTime = creationTime;
    this.previousOwner = previousOwner;
  }

  /**
   * Constructor used when session is created. Takes session id, creation
   * instant (which is used as last accessed instant), and maximum inactivity
   * interval as arguments.
   *
   * @param sessionId
   *          the session id
   * @param creationTime
   *          timestamp of the session creation
   * @param maxInactiveInterval
   *          maximum inactivity interval in seconds
   */
  public SessionData(String sessionId, long creationTime, int maxInactiveInterval) {
    this(sessionId, creationTime, maxInactiveInterval, creationTime, null);
    isNew = true;
  }

  /**
   * @return session id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets session id.
   *
   * @param id
   *          the session id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Creation time expressed in seconds since epoch.
   *
   * @return the session creation time
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Sets creation time of the session.
   *
   * @param creationTime
   *          the session creation time in seconds since epoch
   */
  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  /**
   * Last accessed time expressed in seconds since epoch.
   *
   * @return last accessed time expressed in seconds since epoch
   */
  public long getLastAccessedTime() {
    return lastAccessedTime;
  }

  /**
   * Sets last accessed time of the session.
   *
   * @param lastAccessedTime
   *          the last accessed time expressed in seconds since epoch
   */
  public void setLastAccessedTime(long lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
  }

  /**
   * Returns maximum inactivity interval before session expires in seconds.
   *
   * @return maximum inactivity interval
   */
  public int getMaxInactiveInterval() {
    return maxInactiveInterval;
  }

  /**
   * Sets maximum inactivity interval before session expires
   *
   * @param maxInactiveInterval
   *          maximum inactivity interval in seconds
   */
  public void setMaxInactiveInterval(int maxInactiveInterval) {
    this.maxInactiveInterval = maxInactiveInterval;
  }

  /**
   * @return <code>true</code> if session is new
   */
  public boolean isNew() {
    return isNew;
  }

  /**
   * Sets flag if the session is new or not
   *
   * @param isNew
   *          <code>true</code> if session is new
   */
  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  /**
   * Checks if key is a key of an attribute that present in repository.
   *
   * @param name
   *          key of attribute
   * @return <code>true</code> if attribute may be in repository
   */
  public boolean isMaybeInRepository(String name) {
    return repositoryKeys == null || repositoryKeys.contains(name);
  }

  /**
   * Retrieves set of keys for this session that have been stored in repository.
   * If the value is <code>null</code>, then the set has yet not been retrieved
   * from repository
   *
   * @return set of keys present in session in repository or <code>null</code>
   *         if the set has not been retrieved from repository.
   */
  public Set<String> getRepositoryKeys() {
    return repositoryKeys;
  }

  /**
   * Sets the set of keys that have been stored in repository for this session.
   *
   * @param repositoryKeys
   *          set of keys that have been stored in repository
   */
  void setRepositoryKeys(Set<String> repositoryKeys) {
    this.repositoryKeys = repositoryKeys;
  }

  /**
   * Set of keys whose attributes must be directly stored and retrieved from
   * repository. This means that those attributes can't be cached using
   *
   * @return keys whose attributes must be stored and retrieved from repository
   */
  public Set<String> getMandatoryRemoteKeys() {
    return mandatoryRemoteKeys;
  }

  /**
   * Set keys whose attributes must be directly stored and retrieved from
   * repository.
   *
   * @param mandatoryRemoteKeys
   *          set of keys that must be directly backed by repository
   */
  public void setMandatoryRemoteKeys(Set<String> mandatoryRemoteKeys) {
    this.mandatoryRemoteKeys = mandatoryRemoteKeys;
  }

  /**
   * Last accessed instant retrieved from repository. Usually corresponds to
   * instant of the processing of the previous request.
   *
   * @return last accessed instant as retrieved from repository
   */
  public long getOriginalLastAccessed() {
    return originalLastAccessed;
  }

  /**
   * Returns id of the node that owned session in previous request
   *
   * @return id of node that owned session in previous request
   */
  public String getPreviousOwner() {
    return previousOwner;
  }

  /**
   * Returns instant when the session expires.
   *
   * @return instant when the session expires
   */
  public long expiresAt() {
    return lastAccessedTime + TimeUnit.SECONDS.toMillis(maxInactiveInterval);
  }

  /**
   * Returns <code>true</code> if attribute must be retrieved and stored
   * directly into repository.
   *
   * @param key
   *          the attribute key to checked
   * @return <code>true</code> if attribute must be backed by repository
   */
  public boolean isNonCacheable(String key) {
    return mandatoryRemoteKeys != null && mandatoryRemoteKeys.contains(key);
  }

  /**
   * Retrieves old session id or null if session id was not changed. Used if
   * session id has been changed.
   *
   * @return the newSessionId
   */
  public String getOldSessionId() {
    return oldSessionId;
  }

  /**
   * Sets new session id. Used to change session id.
   *
   * @param newSessionId
   *          the newSessionId to set
   */
  public void setNewSessionId(String newSessionId) {
    this.oldSessionId = id;
    this.id = newSessionId;
  }

  /**
   * Returns session id that was used to retrieve session from store. As the id
   * may change during request, we need original id to access the repository.
   *
   * @return original session id if session id was changed, otherwise current
   *         session id
   */
  public String getOriginalId() {
    return oldSessionId != null ? oldSessionId : id;
  }

  /**
   * Return <code>true</code> if session id has changed.
   *
   * @return <code>true</code> if session id has changed
   */
  public boolean isIdChanged() {
    return oldSessionId != null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + (int)(originalLastAccessed ^ (originalLastAccessed >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SessionData other = (SessionData)obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (originalLastAccessed != other.originalLastAccessed) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SessionData [id=").append(id).append(", oldSessionId=").append(oldSessionId)
        .append(", creationTime=").append(creationTime).append(", lastAccessedTime=").append(lastAccessedTime)
        .append(", maxInactiveInterval=").append(maxInactiveInterval).append(", isNew=").append(isNew)
        .append(", previousOwner=").append(previousOwner).append("]");
    return builder.toString();
  }
}
