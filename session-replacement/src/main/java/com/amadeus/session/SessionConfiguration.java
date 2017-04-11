package com.amadeus.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main configuration parameters for the session management. This includes
 * information if session can be distributed and default timeout.
 * <p>
 * The parameters are configured using following system properties:
 * <p>
 * <code>com.amadeus.session.distributable</code> set to true enables session to
 * be stored in distributed storage.
 * <p>
 * <code>com.amadeus.session.timeout</code> specifies default session inactivity
 * timeout in seconds. Default value is 1800 seconds.
 * <p>
 * <code>com.amadeus.session.non-cachable</code> specifies comma-separated list
 * of session attributes that must be synchronized with repository. By default,
 * this list is empty. <code>com.amadeus.session.replication-trigger</code>
 * specifies repository behavior when accessing attributes. See
 * {@link ReplicationTrigger} enumeration for details. Default value is
 * {@link ReplicationTrigger#SET_AND_NON_PRIMITIVE_GET}.
 * <p>
 * <code>com.amadeus.session.logging.mdc.enabled</code> activates adding current
 * session id to logging systems Mapped Diagnostic Context (MDC).
 * <p>
 * <code>com.amadeus.session.logging.mdc.name</code> specifies key used to store
 * current session id to logging systems Mapped Diagnostic Context (MDC).
 *
 */
public class SessionConfiguration implements Serializable {
  private static final long serialVersionUID = -4538053252686416412L;
  private static final Logger logger = LoggerFactory.getLogger(SessionConfiguration.class);

  /**
   * Strategies for detecting when does an attribute change.
   */
  public enum ReplicationTrigger {
    /**
     * Session data is replicated on set of the attribute and when an attribute
     * retrieved via Attribute contains a non-primitive type. This means that
     * the get of an attribute of the well-known Java type such as Boolean,
     * Character, Number (Double, Float, Integer, Long), doesn't trigger
     * replication to repository, but getAttribute operation for other types of
     * attribute will trigger update in the repository.
     */
    SET_AND_NON_PRIMITIVE_GET(true),
    /**
     * This option assumes that the application will explicitly call
     * setAttribute on the session when the data needs to be replicated. It
     * prevents unnecessary replication and can benefit overall performance, but
     * is inherently unsafe as attributes that were changed after the get, but
     * where never
     */
    SET(false);

    private final boolean replicateOnGet;

    private ReplicationTrigger(boolean replicateOnTrigger) {
      this.replicateOnGet = replicateOnTrigger;
    }

    /**
     * Returns <code>true</code> if session should be replicated on getAttribute
     * operation
     *
     * @return <code>true</code> if session should be replicated on get
     */
    public boolean isReplicateOnGet() {
      return replicateOnGet;
    }

    /**
     * Checks passed value against allowed values in enumeration. If it is not
     * valid, returns default value and logs error.
     *
     * @param replicationValue
     *          value to check
     * @return parsed or default value
     */
    public static ReplicationTrigger validate(String replicationValue) {
      try {
        return ReplicationTrigger.valueOf(replicationValue);
      } catch (IllegalArgumentException e) { // NOSONAR
        logger.error(
            "Invalid configuration '{}': "
                + "Replication trigger value was not one of [{}]. The value was {}. Using default value {}",
            SESSION_REPLICATION_TRIGGER, Arrays.asList(ReplicationTrigger.values()), replicationValue,
            SET_AND_NON_PRIMITIVE_GET);
      }
      return DEFAULT_REPLICATION_TRIGGER;
    }
  }

  /**
   * Attribute provider can be used via call to
   * {@link SessionConfiguration#initializeFrom(AttributeProvider)} to set up
   * SessionConfiguration.
   */
  public interface AttributeProvider {
    /**
     * Returns value of the attribute
     *
     * @param key
     *          the key for the attribute
     * @return value of the attribute
     */
    String getAttribute(String key);

    /**
     * Describes source of attributes. Used for logging and error reporting.
     *
     * @return source of attributes.
     */
    Object source();
  }

  /**
   * Indicates if sessions can be distributed or not.
   */
  public static final String DISTRIBUTABLE_SESSION = "com.amadeus.session.distributable";
  /**
   * Non-distributable web apps will be treated as distributable if this
   * parameter is set to <code>true</code>.
   */
  public static final String FORCE_DISTRIBUTABLE = "com.amadeus.session.distributable.force";
  /**
   * Default session timeout in seconds.
   */
  public static final String DEFAULT_SESSION_TIMEOUT = "com.amadeus.session.timeout";
  /**
   * List of attributes that must be retrieved from repository (i.e. they can't
   * be cached locally).
   */
  public static final String NON_CACHEABLE_ATTRIBUTES = "com.amadeus.session.non-cacheable";
  /**
   * Strategy for for triggering replication. See {@link ReplicationTrigger}
   * enumeration.
   */
  public static final String SESSION_REPLICATION_TRIGGER = "com.amadeus.session.replication-trigger";
  /**
   * Default strategy for detecting that attribute changed.
   */
  public static final ReplicationTrigger DEFAULT_REPLICATION_TRIGGER = ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET;

  /**
   * Is session id stored in logging MDC.
   */
  public static final String LOG_MDC_SESSION_ENABLED = "com.amadeus.session.logging.mdc.enabled";
  /**
   * The name of the logging MDC attribute where session id is stored.
   */
  public static final String LOG_MDC_SESSION_NAME = "com.amadeus.session.logging.mdc.name";
  /**
   * Default key used to store session id in logging system's MDC.
   */
  public static final String LOGGING_MDC_DEFAULT_KEY = "JSESSIONID";

  /**
   * Activates sticky session strategy. When activated, implementation should
   * try to handle all session activity on the last node that processed client
   * request that impacts the session.
   */
  public static final String STICKY_SESSIONS = "com.amadeus.session.sticky";
  /**
   * By default, sessions are sticky.
   */
  public static final String DEFAULT_STICKY_SESSIONS = "true";

  /**
   * The namespace for sessions. It is best practice to have sessions in
   * different applications or webapps should having different namespaces. If
   * applications want to share sessions, then they can use same namespace. In
   * case of webapps, namespace can be defined using servlet init parameter, or
   * if not present, the context name of the webapp is used as namespace.
   */
  public static final String SESSION_NAMESPACE = "com.amadeus.session.namespace";
  
  /**
   * Default session namespace is <code>default</code>
   */
  public static final String DEFAULT_SESSION_NAMESPACE = "default";

  /**
   * Indicate if generated session prefixed with!Timestamp.
   */
  public static final String SESSION_TIMESTAMP = "com.amadeus.session.timestamp";
  
  /**
   * Default session timestamp is <code>false</code>
   */
  public static final String DEFAULT_SESSION_TIMESTAMP = "false";
  
  /**
   * Default session timeout is 30 minutes.
   */
  public static final int DEFAULT_SESSION_TIMEOUT_VALUE_NUM = 1800;
  // String version of default session timeout
  private static final String DEFAULT_SESSION_TIMEOUT_VALUE = String.valueOf(DEFAULT_SESSION_TIMEOUT_VALUE_NUM);

  /**
   * Enables overriding the name of the host. By default it is retrieved from
   * environment. See {@link #initNode()} for details.
   */
  public static final String SESSION_HOST = "com.amadeus.session.host";
  /**
   * ServletContext parameter or system property containing the configuration
   * for the {@link SessionRepository}. See each implementation for detailed
   * format. By convention, items in the format are separated by commas.
   */
  public static final String PROVIDER_CONFIGURATION = "com.amadeus.session.repository.conf";
  /**
   * ServletContext parameter or system property containing the name of
   * {@link SessionRepositoryFactory} implementation. The value is either name
   * of the class or registered repository name such as <code>redis</code>,
   * <code>in-memory</code>
   */
  public static final String REPOSITORY_FACTORY_NAME = "com.amadeus.session.repository.factory";
  /**
   * ServletContext parameter or system property containing the name of the
   * {@link SessionTracking} implementation or value from
   * <code>com.amadeus.session.servlet.SessionPropagation</code> enumeration.
   */
  public static final String SESSION_PROPAGATOR_NAME = "com.amadeus.session.tracking";
  /**
   * ServletContext parameter or system property containing the name of the
   * cookie or URL element for propagating session.
   *
   * @see #DEFAULT_SESSION_ID_NAME
   */
  public static final String SESSION_ID_NAME = "com.amadeus.session.sessionName";

  /**
   * Default name for the cookie or URL element.
   *
   * @see #SESSION_ID_NAME
   */
  public static final String DEFAULT_SESSION_ID_NAME = "JSESSIONID";

  /**
   * System property containing the id of node.
   */
  public static final String NODE_ID = "com.amadeus.session.node";

  /**
   * ServletContext parameter or system property disabled session management.
   */
  public static final String DISABLED_SESSION = "com.amadeus.session.disabled";
  /**
   * ServletContext parameter or system property activating sharing of instances
   * of session during concurrent requests.
   */
  public static final String REUSE_CONCURRENT_SESSION = "com.amadeus.session.reuse.concurrent";
  /**
   * ServletContext parameter or system property indicating what provider
   * generates session ids.
   */
  public static final String SESSION_ID_PROVIDER = "com.amadeus.session.id";
  /**
   * ServletContext parameter or system property specifying the length of the
   * generates session ids. Used in {@link RandomIdProvider}.
   */
  public static final String SESSION_ID_LENGTH = "com.amadeus.session.id.length";

  /**
   * Default session id length when using {@link RandomIdProvider} is 30 bytes.
   */
  public static final String DEFAULT_SESSION_ID_LENGTH = "30";

  /**
   * Specifies if listeners should be discovered using interception of native
   * session.
   */
  public static final String INTERCEPT_LISTENERS = "com.amadeus.session.intercept.listeners";
  /**
   * Specifies if commit should be done on all concurrent requests to session.
   */
  public static final String COMMIT_ON_ALL_CONCURRENT = "com.amadeus.session.commit.concurrent";
  /**
   * Specifies key to be used for encryption. When present activates encryption
   * automatically. If key specifies a URL, key will be loaded from specified
   * address. Otherwise it is treated literally.
   */
  public static final String SESSION_ENCRYPTION_KEY = "com.amadeus.session.encryption.key";

  private int maxInactiveInterval;
  private boolean distributable;
  private boolean sticky;
  private boolean timestampSufix;
  private boolean allowedCachedSessionReuse;
  private boolean interceptListeners;
  private boolean forceDistributable;
  private boolean loggingMdcActive;
  private boolean usingEncryption;
  private String loggingMdcKey;
  private String node;
  private String namespace;
  private String providerConfiguration;
  private String repositoryFactory;
  private String sessionTracking;
  private String sessionIdName;
  private String encryptionKey;

  private Set<String> nonCacheable;
  private ReplicationTrigger replicationTrigger;
  private Properties attributes;
  private transient AttributeProvider currentAttributeProvider;
  private boolean commitOnAllConcurrent;

  /**
   * Default constructor.
   */
  public SessionConfiguration() {
    attributes = new Properties();
    distributable = Boolean.parseBoolean(getPropertySecured(DISTRIBUTABLE_SESSION, "true"));
    sticky = Boolean.parseBoolean(getPropertySecured(STICKY_SESSIONS, DEFAULT_STICKY_SESSIONS));
    timestampSufix = Boolean.parseBoolean(getPropertySecured(SESSION_TIMESTAMP, DEFAULT_SESSION_TIMESTAMP));
    loggingMdcActive = Boolean.parseBoolean(getPropertySecured(LOG_MDC_SESSION_ENABLED, "true"));
    loggingMdcKey = getPropertySecured(LOG_MDC_SESSION_NAME, LOGGING_MDC_DEFAULT_KEY);
    namespace = getPropertySecured(SESSION_NAMESPACE, null);
    sessionIdName = getPropertySecured(SESSION_ID_NAME, DEFAULT_SESSION_ID_NAME);
    providerConfiguration = getPropertySecured(PROVIDER_CONFIGURATION, null);
    repositoryFactory = getPropertySecured(REPOSITORY_FACTORY_NAME, null);
    sessionTracking = getPropertySecured(SESSION_PROPAGATOR_NAME, null);
    allowedCachedSessionReuse = Boolean.parseBoolean(getPropertySecured(REUSE_CONCURRENT_SESSION, null));
    interceptListeners = Boolean.parseBoolean(getPropertySecured(INTERCEPT_LISTENERS, null));
    forceDistributable = Boolean.parseBoolean(getPropertySecured(FORCE_DISTRIBUTABLE, null));
    commitOnAllConcurrent = Boolean.parseBoolean(getPropertySecured(COMMIT_ON_ALL_CONCURRENT, null));

    setNonCacheable(getPropertySecured(NON_CACHEABLE_ATTRIBUTES, null));
    String replicationValue = getPropertySecured(SESSION_REPLICATION_TRIGGER, DEFAULT_REPLICATION_TRIGGER.toString());
    replicationTrigger = ReplicationTrigger.validate(replicationValue);
    maxInactiveInterval = DEFAULT_SESSION_TIMEOUT_VALUE_NUM;
    String inactiveValue = getPropertySecured(DEFAULT_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_VALUE);
    try {
      if (nonEmpty(inactiveValue)) {
        maxInactiveInterval = Integer.parseInt(inactiveValue);
      }
    } catch (NumberFormatException e) {
      logger.error("`{}` system property was not an integer: {}, using default {}", DEFAULT_SESSION_TIMEOUT,
          inactiveValue, maxInactiveInterval);
    }
    node = initNode();
    setEncryptionKey(getPropertySecured(SESSION_ENCRYPTION_KEY, null));
  }

  /**
   * Sets encryption key to use. If key is <code>null</code>, encryption is
   * deactivated.
   *
   * @param key
   *          encryption key to use or <code>null</code>
   */
  public void setEncryptionKey(String key) {
    encryptionKey = key;
    usingEncryption = key != null;
  }

  static boolean allowedProtocol(String protocol) {
    return "http".equals(protocol) || "https".equals(protocol) || "file".equals(protocol);
  }

  /**
   * Allows setting up configuration from external source. It is expected that
   * external source offers the same attributes as the ones read from system
   * properties.
   *
   * @param provider
   *          the external source for attributes
   */
  public void initializeFrom(AttributeProvider provider) {
    currentAttributeProvider = provider;
    distributable = read(DISTRIBUTABLE_SESSION, distributable);
    sticky = read(STICKY_SESSIONS, sticky);
    timestampSufix = read(SESSION_TIMESTAMP, timestampSufix);
    interceptListeners = read(INTERCEPT_LISTENERS, interceptListeners);
    allowedCachedSessionReuse = read(REUSE_CONCURRENT_SESSION, allowedCachedSessionReuse);
    sessionTracking = read(SESSION_PROPAGATOR_NAME, sessionTracking);
    sessionIdName = read(SESSION_ID_NAME, sessionIdName);
    repositoryFactory = read(REPOSITORY_FACTORY_NAME, repositoryFactory);
    providerConfiguration = read(PROVIDER_CONFIGURATION, providerConfiguration);
    namespace = read(SESSION_NAMESPACE, namespace);
    node = read(SESSION_HOST, node);
    loggingMdcActive = read(LOG_MDC_SESSION_ENABLED, loggingMdcActive);
    loggingMdcKey = read(LOG_MDC_SESSION_NAME, loggingMdcKey);
    forceDistributable = read(FORCE_DISTRIBUTABLE, forceDistributable);
    setEncryptionKey(provider.getAttribute(SESSION_ENCRYPTION_KEY));

    String value = provider.getAttribute(SESSION_ENCRYPTION_KEY);
    if (nonEmpty(value)) {
      setEncryptionKey(value);
    }
    value = provider.getAttribute(SESSION_REPLICATION_TRIGGER);
    if (nonEmpty(value)) {
      replicationTrigger = ReplicationTrigger.validate(value);
    }
    value = provider.getAttribute(NON_CACHEABLE_ATTRIBUTES);
    if (nonEmpty(value)) {
      setNonCacheable(value);
    }
    initMaxInactiveInterval(provider);
  }

  private void initMaxInactiveInterval(AttributeProvider provider) {
    String val = provider.getAttribute(DEFAULT_SESSION_TIMEOUT);
    if (nonEmpty(val)) {
      try {
        maxInactiveInterval = Integer.parseInt(val);
      } catch (NumberFormatException e) {
        logger.warn("`{}` configuration attribute was not an integer: {} for source {}", DEFAULT_SESSION_TIMEOUT, val,
            provider.source());
      }
    }
  }

  private boolean read(String key, boolean defaultValue) {
    String value = currentAttributeProvider.getAttribute(key);
    if (nonEmpty(value)) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  private String read(String key, String defaultValue) {
    String value = currentAttributeProvider.getAttribute(key);
    if (nonEmpty(value)) {
      return value;
    }
    return defaultValue;
  }

  private boolean nonEmpty(String value) {
    return value != null && !value.isEmpty();
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
   *          inactivity interval in seconds
   */
  public void setMaxInactiveInterval(int maxInactiveInterval) {
    this.maxInactiveInterval = maxInactiveInterval;
  }

  /**
   * Returns <code>true</code> if session can be stored remote repository.
   *
   * @return <code>true</code> if session can be stored remote repository
   */
  public boolean isDistributable() {
    return distributable;
  }

  /**
   * Sets whether session can be stored in remote repository.
   *
   * @param distributable
   *          <code>true</code> if session can be stored remote repository
   */
  public void setDistributable(boolean distributable) {
    this.distributable = distributable;
  }

  /**
   * Returns set of keys that should not be cached locally.
   *
   * @return set of attribute names that can't be cached locally
   */
  public Set<String> getNonCacheable() {
    return nonCacheable;
  }

  /**
   * Sets set of keys that should not be cached locally.
   *
   * @param nonCacheable
   *          set of attribute names that can't be cached locally
   */
  public void setNonCacheable(Set<String> nonCacheable) {
    this.nonCacheable = Collections.unmodifiableSet(new HashSet<>(nonCacheable));
  }

  /**
   * Sets set of keys that should not be cached locally by extracting keys from
   * comma-separated list provided as parameter.
   *
   * @param nonCacheableAttributesCsv
   *          list of non cacheable attributes in comma-separated list
   */
  public void setNonCacheable(String nonCacheableAttributesCsv) {
    if (nonCacheableAttributesCsv != null) {
      String[] attrs = nonCacheableAttributesCsv.split(",");
      nonCacheable = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(attrs)));
    }
  }

  /**
   * Returns strategy used to detect if attribute changed.
   *
   * @return the current replication strategy
   */
  public ReplicationTrigger getReplicationTrigger() {
    return replicationTrigger;
  }

  /**
   * Sets strategy used to detect if attribute changed.
   *
   * @param replicationTrigger
   *          new replication strategy to use
   */
  public void setReplicationTrigger(ReplicationTrigger replicationTrigger) {
    this.replicationTrigger = replicationTrigger;
  }

  /**
   * Returns <code>true</code> if session information should be stored in
   * logging systems Mapped Diagnostic Context (MDC).
   *
   * @return <code>true</code> if session information should be stored in MDC
   */
  public boolean isLoggingMdcActive() {
    return loggingMdcActive;
  }

  /**
   * Sets whether session information should be stored in logging systems Mapped
   * Diagnostic Context (MDC).
   *
   * @param loggingMdcActive
   *          <code>true</code> if logging should add information to MDC
   */
  public void setLoggingMdcActive(boolean loggingMdcActive) {
    this.loggingMdcActive = loggingMdcActive;
  }

  /**
   * Returns key used to store current session id to logging system's Mapped
   * Diagnostic Context (MDC).
   *
   * @return the key used for session id during logging
   */
  public String getLoggingMdcKey() {
    return loggingMdcKey != null ? loggingMdcKey : LOGGING_MDC_DEFAULT_KEY;
  }

  /**
   * Sets key used to store current session id to logging system's Mapped
   * Diagnostic Context (MDC).
   *
   * @param loggingMdcKey
   *          the key value to use. If value is null,
   *          {@link #LOGGING_MDC_DEFAULT_KEY} is used as key
   */
  public void setLoggingMdcKey(String loggingMdcKey) {
    this.loggingMdcKey = loggingMdcKey;
  }

  /**
   * Returns <code>true</code> if sessions are sticky. Sticky sessions should be
   * expired on the last node that used them.
   *
   * @return <code>true</code> if session is sticky
   */
  public boolean isSticky() {
    return sticky;
  }

  /**
   * Sets stickiness of sessions. See {@link #isSticky()}.
   *
   * @param sticky
   *          <code>true</code> if session is stick
   */
  public void setSticky(boolean sticky) {
    this.sticky = sticky;
  }

  /**
   * Returns <code>true</code> if session id is expected to be suffixed by !timestamp.
   *
   * @return <code>true</code> if session id to be suffixed by !timestamp.
   */
  public boolean isTimestampSufix() {
    return timestampSufix;
  }

  /**
   * Add timestamp suffix to session id if set to true. See {@link #isTimestampSufix()}.
   *
   * @param timestamp
   *          <code>true</code> if session is expected to be suffixed by timestamp
   */
  public void setTimestampSufix(boolean timestamp) {
    this.timestampSufix = timestamp;
  }
  
  /**
   * Returns id of the current node
   *
   * @return id/name of the current node
   */
  public String getNode() {
    return node;
  }

  /**
   * Sets id of the node
   *
   * @param node
   *          id/name of the current node
   */
  public void setNode(String node) {
    this.node = node;
  }

  /**
   * Namespace for session. It will never be <code>null</code> (if null, value
   * <code>default</code>).
   *
   * @return namespace for session
   */
  public String getNamespace() {
    return namespace == null ? DEFAULT_SESSION_NAMESPACE : namespace;
  }

  /**
   * Returns configured namespace of the session (may be <code>null</code>).
   *
   * @return the truely configured name of the session
   */
  public String getTrueNamespace() {
    return namespace;
  }

  /**
   * Sets namespace of the session.
   *
   * @param namespace
   *          name to use
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /**
   * Returns configuration string for the repository provider.
   *
   * @return provider configuration string
   */
  public String getProviderConfiguration() {
    return providerConfiguration;
  }

  /**
   * Sets configuration string for the repository provider.
   *
   * @param providerConfiguration
   *          the string used to configure provider
   */
  public void setProviderConfiguration(String providerConfiguration) {
    this.providerConfiguration = providerConfiguration;
  }

  /**
   * Gets the class name or the unique identifier of the repository factory.
   *
   * @return name of the repository factory
   */
  public String getRepositoryFactory() {
    return repositoryFactory;
  }

  /**
   * Sets the class name or the unique identifier of the repository factory.
   *
   * @param repositoryFactory
   *          the name of the repository factory
   */
  public void setRepositoryFactory(String repositoryFactory) {
    this.repositoryFactory = repositoryFactory;
  }

  /**
   * Returns class name or unique id of the session tracking.
   * {@link SessionTracking} is the class responsible for reading and
   * propagating session id.
   *
   * @return the name of the session tracking class
   */
  public String getSessionTracking() {
    return sessionTracking;
  }

  /**
   * Sets class name or unique id of the session tracking. See
   * {@link #getSessionTracking()}.
   *
   * @param sessionTracking
   *          the unique id or name of the session tracking class
   */
  public void setSessionTracking(String sessionTracking) {
    this.sessionTracking = sessionTracking;
  }

  /**
   * Returns name of identifier that is used to stored session id. E.g. this
   * will be cookie name if session uses cookie propagation.
   *
   * @return the session identifier
   */
  public String getSessionIdName() {
    return sessionIdName;
  }

  /**
   * Sets name of session identifier.
   *
   * @param sessionIdName
   *          new session identifier
   */
  public void setSessionIdName(String sessionIdName) {
    this.sessionIdName = sessionIdName;
  }

  /**
   * Returns <code>true</code> if multiple concurrent threads that operate on
   * same session can re-use session from local cache.
   *
   * @return <code>true</code> if concurrent threads can access same session
   */
  public boolean isAllowedCachedSessionReuse() {
    return allowedCachedSessionReuse;
  }

  /**
   * Enables or disables sharing of session instances between multiple
   * concurrent threads.
   *
   * @param allowedCachedSessionReuse
   *          <code>true</code> if concurrent threads can access same session
   */
  public void setAllowedCachedSessionReuse(boolean allowedCachedSessionReuse) {
    this.allowedCachedSessionReuse = allowedCachedSessionReuse;
  }

  /**
   * Initializes node id. Node id is read either from property, or from
   * environment variables depending on OS.
   *
   * @return node id
   */
  static String initNode() {
    String node = getPropertySecured(NODE_ID, null);
    if (node != null) {
      return node;
    }
    // On Windows try the 'COMPUTERNAME' variable
    try {
      if (getPropertySecured("os.name", null).startsWith("Windows")) {
        node = System.getenv("COMPUTERNAME");
      } else {
        // If it is not Windows asume it is Unix-like OS and that it
        // has HOSTNAME variable
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null) {
          node = hostname;
        }
      }
    } catch (SecurityException e) {
      logger.info("Security exception when trying to get environmnet variable", e);
    }
    if (node == null) {
      // Try portable way
      try {
        node = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        logger.info("Unable to resolve local host, that's a strange error, but somehow it occured.", e);
        // OK, last attempt: call hostname executable
        node = "unknown";
      }
    }
    return node;
  }

  /**
   * Returns value of system property, and logs information if access to
   * properties is protected.
   *
   * @param key
   *          property key
   * @param def
   *          default value for property
   * @return value of the property or default value if property is not defined
   *         or if access is protected
   */
  public static String getPropertySecured(String key, String def) {
    try {
      return System.getProperty(key, def);
    } catch (SecurityException e) {
      logger.info("Security exception when trying to get system property", e);
      return def;
    }
  }

  /**
   * Returns value of attribute from attribute provider if it was supplied or
   * from system property if it is not present or if there is no attribute
   * provider
   *
   * @param key
   *          key that identifies attribute or system property
   * @param defaultValue
   *          default value for key
   * @return value of the attribute, or property, or default value if it was not
   *         defined or if access is protected
   */
  public String getAttribute(String key, String defaultValue) {
    String value = attributes.getProperty(key);
    if (value == null) {
      if (currentAttributeProvider != null) {
        value = currentAttributeProvider.getAttribute(key);
      }
      if (value == null) {
        value = getPropertySecured(key, defaultValue);
      }
    }
    return value;
  }

  /**
   * Sets attribute value. Used to add configuration items not supported by
   * {@link SessionConfiguration}.
   *
   * @param key
   *          the name of the attribute
   * @param value
   *          the value of the attribute
   */
  public void setAttribute(String key, String value) {
    attributes.setProperty(key, value);
  }

  /**
   * @return the interceptListeners
   */
  public boolean isInterceptListeners() {
    return interceptListeners;
  }

  /**
   * @param interceptListeners
   *          the interceptListeners to set
   */
  public void setInterceptListeners(boolean interceptListeners) {
    this.interceptListeners = interceptListeners;
  }

  /**
   * Returns <code>true</code> if distribution/replication should be used even
   * when web app is marked as non-distributable.
   *
   * @return <code>true</code> if distribution/replication should be used even
   *         when web app is marked as non-distributable.
   */
  public boolean isForceDistributable() {
    return forceDistributable;
  }

  /**
   * Sets whether distribution/replication should be used even when web app is
   * marked as non-distributable.
   *
   * @param forceDistributable
   *          <code>true</code> if distribution/replication should be used even
   *          when web app is marked as non-distributable.
   */
  public void setForceDistributable(boolean forceDistributable) {
    this.forceDistributable = forceDistributable;
  }

  /**
   * Returns <code>true</code> if commit of attributes should be done on all
   * concurrent requests to session.
   *
   * @return <code>true</code> if commit should be done on all concurrent
   *         requests
   */
  public boolean isCommitOnAllConcurrent() {
    return commitOnAllConcurrent;
  }

  /**
   * Controls if commit of attributes should be done on all concurrent requests
   * to session.
   *
   * @param commitOnAllConcurrent
   *          <code>true</code> if commit should be done on all concurrent
   *          requests
   */
  public void setCommitOnAllConcurrent(boolean commitOnAllConcurrent) {
    this.commitOnAllConcurrent = commitOnAllConcurrent;
  }

  /**
   * Returns <code>true</code> if session should be encrypted before storing in
   * repository.
   *
   * @return <code>true</code> if session should be encrypted before storing in
   *         repository
   */
  public boolean isUsingEncryption() {
    return usingEncryption;
  }

  /**
   * Sets whether the session data is stored in encrypted form in repository. If
   * set to <code>true</code>, encryption key must be set also.
   *
   * @param usingEncryption
   *          <code>true</code> if session data is stored in encrypted form
   */
  public void setUsingEncryption(boolean usingEncryption) {
    this.usingEncryption = usingEncryption;
  }

  /**
   * Returns encryption key to use. If encryption is disabled, returns
   * <code>null</code>.
   *
   * @return the encryption key
   */
  public String getEncryptionKey() {
    if (!usingEncryption) {
      return null;
    }
    try {
      URL url = new URL(encryptionKey);
      if (allowedProtocol(url.getProtocol())) {
        return loadKeyFromUrl(url);
      }
      throw new IllegalStateException(
          "Unknown protocol in url `" + url + "`. Supported protocols are file, http and https. ");
    } catch (MalformedURLException e) { // NOSONAR Ignore
      // When exception occurs, key is not URL
      logger.info("Key was not provided via url.");
    }
    return encryptionKey;
  }

  /**
   * Loads encryption key from specified URL.
   *
   * @param url
   *          from which to load the key
   * @return the loaded encyption key
   */
  private String loadKeyFromUrl(URL url) {
    try (InputStream is = url.openStream(); Scanner s = new Scanner(is)) {
      s.useDelimiter("\\A");
      if (s.hasNext()) {
        encryptionKey = s.next();
        logger.info("Loaded ecnryption key from url `{}`", url);
        return encryptionKey;
      }
      throw new IllegalStateException("Unable to load key from url `" + url + "`. Destination was empty.");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load key from url `" + url + "`.", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SessionConfiguration [namespace=").append(namespace).append(", node=").append(node)
           .append(", sessionIdName=").append(sessionIdName).append(", maxInactiveInterval=")
           .append(maxInactiveInterval).append(", distributable=").append(distributable).append(", sticky=")
           .append(sticky).append(", allowedCachedSessionReuse=").append(allowedCachedSessionReuse)
           .append(", interceptListeners=").append(interceptListeners).append(", forceDistributable=")
           .append(forceDistributable).append(", loggingMdcActive=").append(loggingMdcActive)
           .append(", usingEncryption=").append(usingEncryption).append(", loggingMdcKey=").append(loggingMdcKey)
           .append(", providerConfiguration=").append(providerConfiguration).append(", repositoryFactory=")
           .append(repositoryFactory).append(", sessionTracking=").append(sessionTracking).append(", encryptionKey=")
           .append(encryptionKey).append(", nonCacheable=").append(nonCacheable).append(", replicationTrigger=")
           .append(replicationTrigger).append(", attributes=").append(attributes).append(", commitOnAllConcurrent=")
           .append(commitOnAllConcurrent).append(", timestamp=").append(timestampSufix).append("]");
    return builder.toString();
  }
}
