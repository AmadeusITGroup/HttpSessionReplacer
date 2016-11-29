package com.amadeus.session.repository.redis;

import static com.codahale.metrics.MetricRegistry.name;
import static redis.clients.util.SafeEncoder.encode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SerializerDeserializer;
import com.amadeus.session.SessionData;
import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionRepository;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import redis.clients.jedis.Builder;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 * Main class for implementing Redis repository logic.
 */
class RedisSessionRepository implements SessionRepository {
  private static final Logger logger = LoggerFactory.getLogger(RedisSessionRepository.class);

  /**
   * The default prefix for each key and channel in Redis used by Session
   * management
   */
  static final String DEFAULT_SESSION_PREFIX = "com.amadeus.session:";

  /**
   * Meta attribute for timestamp (Unix time) of last access to session.
   */
  static final byte[] LAST_ACCESSED = encode("#:lastAccessed");
  /**
   * Meta attribute for maximum inactive interval of the session (in seconds).
   */
  static final byte[] MAX_INACTIVE_INTERVAL = encode("#:maxInactiveInterval");
  /**
   * Meta attribute for timestamp (Unix time) of the session creation.
   */
  static final byte[] CREATION_TIME = encode("#:creationTime");
  /**
   * Meta attribute that contains mark if the session is invalid (being deleted
   * or marked as invalid via API).
   */
  static final byte[] INVALID_SESSION = encode("#:invalidSession");
  /**
   * Meta attribute for the node owning the session.
   */
  static final byte[] OWNER_NODE = encode("#:owner");

  /**
   * All attributes starting with #: are internal (meta-atrributes).
   */
  private static final byte[] INTERNAL_PREFIX = new byte[] { '#', ':' };

  private static final int CREATION_TIME_INDEX = 2;
  private static final int INVALID_SESSION_INDEX = 3;
  private static final int OWNER_NODE_INDEX = 4;

  /**
   * Number of bits in byte. Used to allocate byte buffers to store Long and
   * Integer values.
   */
  private static final int BITS_IN_BYTE = 8;

  // Builds OK jedis response
  static final Builder<String> OK_BUILDER = new Builder<String>() {
    @Override
    public String build(Object data) {
      return "OK";
    }
  };

  private final String owner;
  private final byte[] ownerByteArray;
  private final String keyPrefix;
  private final byte[] keyPrefixByteArray;
  private byte[] redirectionsChannel;
  private final RedisFacade redis;
  final RedisExpirationStrategy expirationManager;
  private SessionManager sessionManager;
  private Meter failoverMetrics;
  private boolean sticky;
  private final String namespace;

  RedisSessionRepository(RedisFacade redis, String namespace, String owner, ExpirationStrategy strategy,
      boolean sticky) {
    this.redis = redis;
    this.owner = owner;
    this.namespace = namespace;
    this.ownerByteArray = encode(owner);
    String keyPrefixWithoutClusterGroup = DEFAULT_SESSION_PREFIX + ":" + namespace + ":";
    keyPrefix = keyPrefixWithoutClusterGroup + "{";
    keyPrefixByteArray = encode(keyPrefix);
    redirectionsChannel = encode(keyPrefixWithoutClusterGroup + "redirection");
    this.sticky = sticky;
    if (strategy == ExpirationStrategy.ZRANGE) {
      logger.info("Using ZRANGE (SortedSet) expiration managment");
      expirationManager = new SortedSetSessionExpirationManagement(redis, this, namespace);

    } else {
      logger.info("Using notification expiration managment");
      expirationManager = new NotificationExpirationManagement(redis, this, namespace, owner,
          keyPrefixWithoutClusterGroup, sticky);
    }
  }

  /**
   * This method starts a separate thread that listens to key expirations
   * events.
   *
   * @param sessionManager
   */
  @Override
  public void setSessionManager(final SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    MetricRegistry metrics = sessionManager.getMetrics();
    if (metrics != null) {
      // Cleanup old metrics related to this namespace
      metrics.removeMatching(new MetricFilter() {
        @Override
        public boolean matches(String name, Metric metric) {
          return name.startsWith(name(RedisConfiguration.METRIC_PREFIX, "redis"));
        }
      });
      if (sticky) {
        failoverMetrics = metrics.meter(name(RedisConfiguration.METRIC_PREFIX, namespace, "redis", "failover"));
      }

      redis.startMonitoring(metrics);
    }
    expirationManager.startExpiredSessionsTask(sessionManager);
  }

  /**
   * This method retrieves session data from repository. The data retrieved from
   * repository contains meta attributes such as: last accessed time, creation
   * time, maximum inactive interval, flag if session is invalid (session
   * becomes invalid when it is deleted or marked invalid), and if session
   * stickiness is active, previous owner node id. All meta attributes start
   * with following characters <code>#:</code>
   *
   * @param id
   *          session id
   */
  @Override
  public SessionData getSessionData(String id) {
    byte[] key = sessionKey(id);
    // If sticky session, retrieve last owner also
    List<byte[]> values = sticky
        ? redis.hmget(key, LAST_ACCESSED, MAX_INACTIVE_INTERVAL, CREATION_TIME, INVALID_SESSION, OWNER_NODE)
        : redis.hmget(key, LAST_ACCESSED, MAX_INACTIVE_INTERVAL, CREATION_TIME, INVALID_SESSION);
    if (!checkConsistent(id, values)) {
      return null;
    }
    long lastAccessed = longFrom(values.get(0));

    long creationTime = longFrom(values.get(CREATION_TIME_INDEX));
    String previousOwner = null;
    if (sticky) {
      // For sticky sessions, we need to parse owner node and
      // check if it is this one.
      byte[] prevOwnerBuffer = values.get(OWNER_NODE_INDEX);
      if (prevOwnerBuffer != null) {
        previousOwner = encode(prevOwnerBuffer);
        if (!previousOwner.equals(owner)) {
          // Notify that session fail-over occurred
          logger.info("Retrieved session {}, last node {} to this node {}", id, previousOwner, owner);
          if (failoverMetrics != null) {
            failoverMetrics.mark();
          }
        }
      }
    }
    return new SessionData(id, lastAccessed, intFrom(values.get(1)), creationTime, previousOwner);
  }

  /**
   * Verifies if values retrieved from redis are consistent. Basically just
   * sanity checks.
   *
   * @param sessionId
   * @param values
   * @return <code>true</code> if session data is consistent.
   */
  private boolean checkConsistent(String sessionId, List<byte[]> values) {
    byte[] invalidSessionFlag = values.get(INVALID_SESSION_INDEX);
    // If we have invalid session flag, then session is (clearly) not valid
    if (invalidSessionFlag != null && invalidSessionFlag.length == 1 && invalidSessionFlag[0] == 1) {
      return false;
    }
    if (values.get(0) == null || values.get(1) == null) {
      if (values.get(0) != null || values.get(1) != null) {
        logger.warn(
            "Session in redis repository is not consistent for sessionId: '{}' "
                + "One of last accessed (index 0 in array), max inactive interval (index 1 in array) was null: {}",
            sessionId, values);
      }
      return false;
    }
    return true;
  }

  /**
   * Get integer from byte array
   *
   * @param b
   * @return
   */
  private static int intFrom(byte[] b) {
    return ByteBuffer.wrap(b).getInt();
  }

  /**
   * Get long from byte array
   *
   * @param b
   * @return
   */
  private static long longFrom(byte[] b) {
    return ByteBuffer.wrap(b).getLong();
  }

  /**
   * Adds long value to redis attribute map.
   *
   * @param attributes
   * @param attr
   * @param value
   */
  private static void addLong(Map<byte[], byte[]> attributes, byte[] attr, long value) {
    // In JDK 1.8 we can use Long.BYTES
    ByteBuffer b = ByteBuffer.allocate(Long.SIZE / BITS_IN_BYTE);
    b.putLong(value);

    attributes.put(attr, b.array());
  }

  /**
   * Adds int value to redis attribute map.
   *
   * @param attributes
   * @param attr
   * @param value
   */
  private static void addInt(Map<byte[], byte[]> attributes, byte[] attr, int value) {
    ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / BITS_IN_BYTE);
    b.putInt(value);
    attributes.put(attr, b.array());
  }

  /**
   * This class implements transaction that is executed at session commit time.
   * The transaction will store added/modified session attribute keys using
   * single HMSET redis command and it will also delete removed session
   * attribute keys using HDEL command. It uses underlying {@link RedisFacade}
   * support for transactions on the session key (redis MULTI command), and
   * executes those those commands in atomic way. The meta-attribute for
   * transactions are also updated.
   */
  class RedisSessionTransaction implements SessionRepository.CommitTransaction, RedisFacade.RedisTransaction<String> {
    private Map<byte[], byte[]> attributes = new HashMap<>();
    private List<byte[]> toRemove = new ArrayList<>();
    private byte[] key;
    private SessionData session;

    RedisSessionTransaction(SessionData session) {
      key = sessionKey(session.getId());
      this.session = session;
    }

    @Override
    public void addAttribute(String attribute, Object value) {
      attributes.put(encode(attribute), serializerDeserializer().serialize(value));
    }

    @Override
    public void removeAttribute(String attribute) {
      toRemove.add(encode(attribute));
    }

    /**
     * During commit, we add meta/attributes. See
     * {@link RedisSessionRepository#getSessionData(String)}. for list of meta
     * attributes.
     */
    @Override
    public void commit() {
      if (session.isNew()) {
        addLong(attributes, CREATION_TIME, session.getCreationTime());
      }
      int maxInactiveInterval = session.getMaxInactiveInterval();
      addInt(attributes, MAX_INACTIVE_INTERVAL, maxInactiveInterval);
      addLong(attributes, LAST_ACCESSED, session.getLastAccessedTime());
      if (sessionManager.getConfiguration().isSticky()) {
        attributes.put(OWNER_NODE, ownerByteArray);
      }
      getRedis().transaction(key, this);
      expirationManager.sessionTouched(session);
    }

    @Override
    public Response<String> run(Transaction transaction) {
      if (!toRemove.isEmpty()) {
        byte[][] arr = toRemove.toArray(new byte[0][]);
        transaction.hdel(key, arr);
      }
      if (!attributes.isEmpty()) {
        transaction.hmset(key, attributes);
      }
      return new Response<>(OK_BUILDER);
    }

    @Override
    public boolean isSetAllAttributes() {
      // As we use hash, we don't need to update all attributes on redis
      return false;
    }

    @Override
    public boolean isDistributing() {
      // Redis sessions are surely distributed
      return true;
    }
  }

  @Override
  public CommitTransaction startCommit(SessionData session) {
    return new RedisSessionTransaction(session);
  }

  @Override
  public void remove(SessionData session) {
    redis.del(sessionKey(session.getId()));
    expirationManager.sessionDeleted(session);
  }

  /**
   * Returns session key used to index session in redis.
   *
   * @param sessionId
   * @return key as byte array
   */
  public byte[] sessionKey(String sessionId) {
    return encode(new StringBuilder(keyPrefix.length() + sessionId.length() + 1).append(keyPrefix).append(sessionId)
        .append('}').toString());
  }

  /**
   * Returns session key used to index session in Redis
   *
   * @param session
   *          session data
   * @return key as byte array
   */
  private byte[] sessionKey(SessionData session) {
    return sessionKey(session.getId());
  }

  @Override
  public Object getSessionAttribute(SessionData session, String attribute) {
    List<byte[]> values = redis.hmget(sessionKey(session), encode(attribute));
    return serializerDeserializer().deserialize(values.get(0));
  }

  /**
   * Checks if attribute has internal prefix. See {@link #INTERNAL_PREFIX}.
   *
   * @param buf
   *          buffer containing attribute
   * @return true if attribute is an internal attribute.
   */
  static boolean hasInternalPrefix(byte[] buf) {
    if (buf != null && buf.length > INTERNAL_PREFIX.length) {
      for (int i = 0; i < INTERNAL_PREFIX.length; i++) {
        if (INTERNAL_PREFIX[i] != buf[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Builds session key from session id presented as byte array
   *
   * @param session
   * @return session key as byte array
   */
  byte[] getSessionKey(byte[] session) {
    int prefixLength = keyPrefixByteArray.length;
    byte[] copy = Arrays.copyOf(keyPrefixByteArray, prefixLength + session.length + 1);
    for (int i = 0; i < session.length; i++) {
      copy[prefixLength + i] = session[i];
    }
    copy[prefixLength + session.length] = '}';
    return copy;
  }

  @Override
  public boolean prepareRemove(SessionData session) {
    Long result = redis.hsetnx(sessionKey(session.getId()), INVALID_SESSION, Protocol.BYTES_TRUE);
    return result.intValue() == 1;
  }

  /**
   * Retrieves all attribute keys associated with session. No
   *
   * @param session
   */
  @Override
  public Set<String> getAllKeys(SessionData session) {
    Set<String> keys = new HashSet<>();
    for (byte[] key : redis.hkeys(sessionKey(session))) {
      if (!hasInternalPrefix(key)) {
        keys.add(encode(key));
      }
    }
    return Collections.unmodifiableSet(keys);
  }

  /**
   * The method stores session metadata in redis and marks session as accessed
   * (resets session expire instant).
   */
  @Override
  public void storeSessionData(SessionData sessionData) {
    Map<byte[], byte[]> attributes = new HashMap<>();
    addInt(attributes, MAX_INACTIVE_INTERVAL, sessionData.getMaxInactiveInterval());
    addLong(attributes, LAST_ACCESSED, sessionData.getLastAccessedTime());
    addLong(attributes, CREATION_TIME, sessionData.getCreationTime());
    if (sessionManager.getConfiguration().isSticky()) {
      attributes.put(OWNER_NODE, ownerByteArray);
    }
    redis.hmset(sessionKey(sessionData.getId()), attributes);
    expirationManager.sessionTouched(sessionData);
  }

  @Override
  public void requestFinished() {
    redis.requestFinished();
  }

  @Override
  public void setSessionAttribute(SessionData session, String name, Object value) {
    redis.hset(sessionKey(session), encode(name), serializerDeserializer().serialize(value));
  }

  private SerializerDeserializer serializerDeserializer() {
    return sessionManager.getSerializerDeserializer();
  }

  @Override
  public void removeSessionAttribute(SessionData session, String name) {
    redis.hdel(sessionKey(session), encode(name));
  }

  @Override
  public boolean cleanSessionsOnShutdown() {
    return false;
  }

  @Override
  public Collection<String> getOwnedSessionIds() {
    throw new UnsupportedOperationException("Redis repository doesn't support retrieval of session ids owned by node.");
  }

  @Override
  public void close() {
    redis.close();
    expirationManager.close();
  }

  RedisFacade getRedis() {
    return redis;
  }

  /**
   * This method extracts session id from session key used in Redis. Session
   * keys is located between braces ({}).
   *
   * @param body
   *          string containing session key
   * @return session id
   */
  static String extractSessionId(String body) {
    int beginIndex = body.lastIndexOf(':') + 1;
    String sessionId = body.substring(beginIndex);

    int braceOpening = sessionId.indexOf('{');
    if (braceOpening >= 0) {
      int braceClosing = sessionId.indexOf('}', braceOpening + 1);
      if (braceClosing > braceOpening) {
        int idLen = sessionId.length();
        StringBuilder sb = new StringBuilder(idLen - 2); // NOSONAR
        if (braceOpening > 0) {
          sb.append(sessionId, 0, braceOpening);
        }
        sb.append(sessionId, braceOpening + 1, braceClosing).append(sessionId, braceClosing + 1, idLen);
        sessionId = sb.toString();
      }
    }
    return sessionId;
  }

  /**
   * Changes session id. This renames key in redis and publishes the redis event
   * if other nodes need to be notified.
   *
   * @param sessionData
   */
  @Override
  public void sessionIdChange(SessionData sessionData) {
    redis.rename(sessionKey(sessionData.getOldSessionId()), sessionKey(sessionData.getId()));
    redis.publish(redirectionsChannel, encode(sessionData.getOldSessionId() + ':' + sessionData.getId()));
    expirationManager.sessionIdChange(sessionData);
  }
}
