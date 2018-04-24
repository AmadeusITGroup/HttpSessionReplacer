package com.amadeus.session;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.Closeable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amadeus.session.RepositoryBackedSession.Committer;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Main class responsible for managing sessions. The class offers strategy for
 * retrieving, creating, propagating and deleting session. It also offers
 * services for scheduling and executing task asynchronously.
 * <p>
 * In case of servlet engines, one session manager will be created per
 * {@link ServletContext}.
 * <p>
 * The manager provides following metrics:
 * <ul>
 * <li>`com.amadeus.session.created` measures total number of created sessions
 * as well as rate of sessions created in last 1, 5 and 15 minutes
 * <li>`com.amadeus.session.deleted` measures total number of deleted sessions
 * as well as rate of sessions measures rate of sessions deleted in last 1, 5
 * and 15 minutes
 * <li>`com.amadeus.session.missing` measures total number of session which were
 * not found in repository, as measures rate of such occurrences in last 1, 5 and
 * 15 minutes
 * <li>`com.amadeus.session.retrieved` measures total number of session
 * retrievals as well as measures rate of sessions retrieval from store in last
 * 1, 5 and 15 minutes
 * <li>`com.amadeus.session.timers.commit` measures histogram (distribution) of
 * elapsed time during commit as well as total number of commits and rate of
 * commits over last 1, 5 and 15 minutes
 * <li>`com.amadeus.session.timers.fetch` measures histogram (distribution) of
 * elapsed time during fetch of session data from repository as well as total
 * number of fetch requests and rate of fetch requests over last 1, 5 and 15
 * minutes
 * </ul>
 */
public class SessionManager implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

  static final String SESSIONS_METRIC_PREFIX = "com.amadeus.session";
  

  
  
  private static final String COMMIT_TIMER_METRIC = name(SESSIONS_METRIC_PREFIX, "timer", "commit");
  private static final String FETCH_TIMER_METRIC = name(SESSIONS_METRIC_PREFIX, "timer", "fetch");
  private static final String CREATED_SESSIONS_METRIC = name(SESSIONS_METRIC_PREFIX, "created");
  private static final String DELETED_SESSIONS_METRIC = name(SESSIONS_METRIC_PREFIX, "deleted");
  private static final String MISSING_SESSIONS_METRIC = name(SESSIONS_METRIC_PREFIX, "missing");
  private static final String RETRIEVED_SESSIONS_METRIC = name(SESSIONS_METRIC_PREFIX, "retrieved");

  static final String INVALIDATION_ON_EXPIRY_ERRORS_METRIC = name(SESSIONS_METRIC_PREFIX, "invalidation", "errors",
      "expiry");

  static final String INVALIDATION_ERRORS_METRIC = name(SESSIONS_METRIC_PREFIX, "invalidation", "errors");

  static final String SESSION_PROPAGATED = "com.amadeus.session.sessionPropagated";

  protected final SessionRepository repository;
  protected final SessionNotifier notifier;
  protected final SessionTracking tracking;
  protected final SessionFactory factory;
  protected final ExecutorFacade executors;
  protected final SessionConfiguration configuration;
  protected final SerializerDeserializer serializerDeserializer;

  private final Meter createdSessions;
  private final Meter deletedSessions;
  private final Meter retrievedSessions;
  private final Meter invalidationErrors;
  private final Meter invalidationExpiryErrors;
  private final Meter missingSessions;
  private final Timer commitTimer;
  private final Timer fetchTimer;

  private final MetricRegistry monitoring;

  private final ClassLoader classLoader;

  private JmxReporter reporter;

  /**
   * Constructor.
   *
   * @param executors
   *          thread scheduler
   * @param factory
   *          creates memory representation of session
   * @param repository
   *          repository should be namespace aware - if two
   *          {@link SessionManager} from different namespaces request access to
   *          session with same id, repository should give different sessions
   *          (unless a special configuration overrides this).
   * @param tracking
   *          propagates session information to clients
   * @param notifier
   *          may trigger notifications for different session events
   * @param configuration
   *          the configuration
   * @param classLoader
   *          the class loader to use
   */
  public SessionManager(ExecutorFacade executors, SessionFactory factory, SessionRepository repository,
      SessionTracking tracking, SessionNotifier notifier, SessionConfiguration configuration, ClassLoader classLoader ) {

    this.repository = repository;
    this.tracking = tracking;
    this.notifier = notifier;
    this.configuration = configuration;
    this.factory = factory;
    this.executors = executors;
    this.classLoader = classLoader;

    monitoring = new MetricRegistry();
    
    createdSessions = monitoring.meter(CREATED_SESSIONS_METRIC);
    deletedSessions = monitoring.meter(DELETED_SESSIONS_METRIC);
    retrievedSessions = monitoring.meter(RETRIEVED_SESSIONS_METRIC);
    missingSessions = monitoring.meter(MISSING_SESSIONS_METRIC);
    invalidationErrors = monitoring.meter(INVALIDATION_ERRORS_METRIC);
    invalidationExpiryErrors = monitoring.meter(INVALIDATION_ON_EXPIRY_ERRORS_METRIC);
    commitTimer = monitoring.timer(COMMIT_TIMER_METRIC);
    fetchTimer = monitoring.timer(FETCH_TIMER_METRIC);
    
    serializerDeserializer = configuration.isUsingEncryption() ?
        new EncryptingSerializerDeserializer() :
        new JdkSerializerDeserializer();
    serializerDeserializer.setSessionManager(this);

    factory.setSessionManager(this);
    repository.setSessionManager(this);
    startMonitoring();
  }

  /**
   * Starts monitoring this session manager. The method will expose all metrics
   * through JMX.
   */
  private void startMonitoring() {
    executors.startMetrics(monitoring);
    reporter = JmxReporter.forRegistry(monitoring).inDomain(getJmxDomain()).build();
    reporter.start();
  }

  /**
   * Returns JMX domain for metrics for this session manager.
   *
   * @return JMX domain for metrics
   */
  private String getJmxDomain() {
    return "metrics.session." + configuration.getNamespace();
  }

  /**
   * Fetch the session from the repository. If session was with given id
   * retrieved, but has expired, it will be cleaned up.
   *
   * @param sessionId
   *          session id
   * @param updateTimestamp
   *          <code>true</code> if the session timestamp should be updated
   *          (usually at the start of request)
   * @return session or <code>null</code> if session is not in repository.
   */
  private RepositoryBackedSession fetchSession(String sessionId, boolean updateTimestamp) {
    logger.debug("Fetching session from cache, sessionId: '{}'", sessionId);

    SessionData sessionData;
    // Following variable is used in try/finally to measure execution time
    try (Timer.Context ctx = fetchTimer.time()) { // NOSONAR
      sessionData = repository.getSessionData(sessionId);
    }

    if (sessionData == null) {
      missingSessions.mark();
      logger.debug("Session was not found in cache, considered expired or invalid, sessionId: {}", sessionId);
      return null;
    }

    // Configure session descriptor
    sessionData.setRepositoryKeys(configuration.getNonCacheable());
    // Build session from factory
    RepositoryBackedSession session = factory.build(sessionData);
    retrievedSessions.mark();

    if (session.isExpired()) {
      // Session is expired, we return null, but before we may need to
      // invalidate it.
      logger.debug("Session was in cache, but it was expired, sessionId: {}", sessionId);

      if (session.isValid()) {
        markSessionDeletion(sessionId);
        session.doInvalidate(true);
      }
      return null;
    }
    if (updateTimestamp) {
      sessionData.setLastAccessedTime(System.currentTimeMillis());
      repository.storeSessionData(sessionData);
    }

    return session;
  }

  /**
   * Creates new session from the given id
   *
   * @param requestClass
   *
   * @param sessionId
   *          id of the session
   * @return new session
   */
  private RepositoryBackedSession newSession(String sessionId) {
    RepositoryBackedSession session = factory
        .build(new SessionData(sessionId, System.currentTimeMillis(), configuration.getMaxInactiveInterval()));
    createdSessions.mark();
    notifier.sessionCreated(session);
    return session;
  }

  /**
   * Builds or retrieves session. If session is found in repository, it is
   * retrieved, if not, and create parameter is set to <code>true</code>, then a
   * new session is created. Session id is generated according to
   * {@link SessionTracking} implementation.
   * <p>
   * In some cases, in servlet engine, request can be forwarded from one web
   * application to another one. In this case, the first web application that
   * received request, is responsible for managing session id, and other web
   * application down the chain will reuse this id.
   *
   * @param request
   *          the request being servet
   * @param create
   *          <code>true</code> if session should be created
   * @param forceId
   *          forces usage of this session id.
   *
   * @return existing or new session
   */
  public RepositoryBackedSession getSession(RequestWithSession request, boolean create, String forceId) {
    String id = retrieveId(request, forceId);
    putIdInLoggingMdc(id);
    request.setRequestedSessionId(id);
    RepositoryBackedSession session = null;
    if (id != null) {
      session = fetchSession(id, true);
      if (session == null && !request.isRepositoryChecked()) {
        logger.info("Session with sessionId: '{}' but it was not in repository!", id);
        request.repositoryChecked();
      }
    }
    if (session == null && create) {
      id = forceId != null ? forceId : tracking.newId();
      putIdInLoggingMdc(id);
      logger.info("Creating new session with sessionId: '{}'", id);
      session = newSession(id);
    }
    if (session != null) {
      session.checkUsedAndLock();
    }
    return session;
  }

  /**
   * Changes session id of the passed session. Session id can change only once
   * per request.
   *
   * @param session
   *          the session whose id needs to change
   */
  private void putIdInLoggingMdc(String id) {
    if (configuration.isLoggingMdcActive()) {
      if (id == null) {
        MDC.remove(configuration.getLoggingMdcKey());
      } else {
        MDC.put(configuration.getLoggingMdcKey(), id);
      }
    }
  }

  /**
   * Retrieves id from request. The forceId parameter is used to force usage of
   * certain id (e.g. when forwarding request from one web app to another).
   *
   * @param request
   *          the request that contains session id
   * @param forceId
   *          it provided, this will be used as id
   * @return the session id or <code>null</code>
   */
  private String retrieveId(RequestWithSession request, String forceId) {
    if (forceId != null) {
      return forceId;
    }
    if (request.isIdRetrieved()) {
      return request.getRequestedSessionId();
    }
    return tracking.retrieveId(request);
  }

  /**
   * Propagates the session id to the response. The propagation is done once per
   * request.
   *
   * @param request
   *          the current request
   * @param response
   *          the current response
   */
  public void propagateSession(RequestWithSession request, ResponseWithSessionId response) {
    if (request.getAttribute(SESSION_PROPAGATED) == null) {
      request.setAttribute(SESSION_PROPAGATED, Boolean.TRUE);
      tracking.propagateSession(request, response);
    }
  }

  /**
   * Deletes session from repository and performs orderly cleanup. Called when
   * session expires or when application closes
   *
   * @param sessionId
   *          the id of the session to delete
   * @param expired
   *          <code>true</code> if session is deleted because it has expired
   */
  public void delete(String sessionId, boolean expired) {
    RepositoryBackedSession session = fetchSession(sessionId, false);
    if (session != null) {
      markSessionDeletion(sessionId);
      session.doInvalidate(expired);
    } else if (!expired) {
      logger.debug("Session not found in repository for sessionId: '{}'", sessionId);
    }
  }

  private void markSessionDeletion(String sessionId) {
    logger.info("deleting session with sessionId: '{}'", sessionId );
    deletedSessions.mark();
  }

  
  /**
   * Called when request has been finished. Notifies repository of that fact.
   */
  public void requestFinished() {
    repository.requestFinished();
  }

  /**
   * Executes task in separate thread. This is used to launch blocking or
   * long-running tasks.
   *
   * @param timer
   *          if not null, the time to execute task will be measured and stored
   *          under timer with given name
   * @param task
   *          the task to run
   * @return the future for the runnable. Note that runnable has no result.
   */
  public Future<?> submit(String timer, Runnable task) {
    if (timer != null) {
      return executors.submit(new RunnableWithTimer(timer, task));
    } else {
      return executors.submit(task);
    }
  }

  /**
   * Schedules the tasks to execute with a {@link ScheduledExecutorService} with
   * the specified period.
   *
   * @param timer
   *          if not null, the time to execute task will be measured and stored
   *          under timer with given name
   * @param task
   *          the task to run
   * @param period
   *          period between invocations in seconds
   * @return the scheduled future for the task. Note that runnable has no
   *         result.
   */
  public ScheduledFuture<?> schedule(String timer, Runnable task, long period) {
    if (timer != null) {
      return executors.scheduleAtFixedRate(new RunnableWithTimer(timer, task), period, period, TimeUnit.SECONDS);
    }
    return executors.scheduleAtFixedRate(task, period, period, TimeUnit.SECONDS);
  }

  /**
   * Deletes list of sessions. The deletion might be run in separate thread.
   *
   * @param sessionId
   *          session id to delete
   * @param expired
   *          <code>true</code> if session is deleted because it has expired
   */
  public void deleteAsync(final String sessionId, final boolean expired) {
    Runnable task = new Runnable() {
      @Override
      public void run() {
        try {
          delete(sessionId, expired);
        } catch (Exception e) { // NOSONAR Any exception can occur here
          logger.error("Exception occured while deleting sessionId '{}'", sessionId, e);
        }
      }
    };
    submit("delete-async", task);
  }

  /**
   * Returns {@link SessionRepository} used by this instance.
   *
   * @return {@link SessionRepository} used by this instance.
   */
  public SessionRepository getRepository() {
    return repository;
  }

  /**
   * Returns {@link SessionNotifier} used by this instance.
   *
   * @return {@link SessionNotifier} used by this instance.
   */
  public SessionNotifier getNotifier() {
    return notifier;
  }

  /**
   * Calls {@link Committer} for the passed {@link RepositoryBackedSession} and
   * measures time of execution.
   *
   * @param session
   *          the session to commit
   */
  public void invokeCommit(RepositoryBackedSession session) {
    // Following variable is used in try/finally to measure execution time
    try (Timer.Context ctx = commitTimer.time()) { // NOSONAR
      // Commit is done in request thread.
      // It could be interesting to do it async.
      session.getCommitter().run(); // NOSONAR we use run intentionally
    } catch (Exception e) { // NOSONAR Any exception can occur here
      logger.error("Exception occured while commiting sessionId: '" + session.getId() + "'", e);
    }
  }

  /**
   * Configuration for this {@link SessionManager}
   *
   * @return the active configuration
   */
  public SessionConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Returns class loader to be used for session objects.
   *
   * @return the class loader for session objects
   */
  public ClassLoader getSessionClassLoader() {
    return classLoader;
  }

  /**
   * Serialiazer/deserializer to use when storing to repository. Serialaizer and
   * deserializers can be configured.
   *
   * @return serialiazer/deserializer to use
   */
  public SerializerDeserializer getSerializerDeserializer() {
    return serializerDeserializer;
  }

  /**
   * Runs passed {@link Runnable} task while measuring execution time.
   */
  final class RunnableWithTimer implements Runnable {
    final Runnable task;
    final Timer timer;

    /**
     * Helper {@link Runnable} class used to measure the execution time of
     * passed task and stores it in metrics.
     *
     * @param timerName
     *          name of the timer where the processing time is stored
     * @param task
     *          task to execute
     * @return instance of {@link Runnable} that measures execution time
     */
    RunnableWithTimer(String timerName, Runnable task) {
      this.task = task;
      this.timer = monitoring.timer(name(SESSIONS_METRIC_PREFIX, "timers", timerName));
    }

    @Override
    public void run() {
      // Following variable is used in try/finally to measure execution time
      try (Timer.Context ctx = timer.time()) { // NOSONAR
        task.run(); // NOSONAR Ignore we are already in a thread
      } catch (Exception e) { // NOSONAR any exception may occur here
        logger.warn("Unexpected exception occured in time measured session related task in category " + timer, e);
      }
    }
  }

  /**
   * Called by {@link RepositoryBackedSession} when a conflict occurs during
   * invalidation of session. Conflict may be due to fact that session is stil
   * used.
   *
   * @param session
   *          the session to remove
   * @param onExpiry
   *          conflict occurred during expiration event
   */
  public void invalidationConflict(RepositoryBackedSession session, boolean onExpiry) {
    if (onExpiry) {
      invalidationExpiryErrors.mark();
      logger.warn("Conflict on removing session: {}", session.getId());
    } else {
      invalidationErrors.mark();
      logger.info("Conflict on removing session during exipre management: {}", session.getId());
    }
  }

  /**
   * Retrieves metric registry for this session manager.
   *
   * @return the metric registry
   */
  public MetricRegistry getMetrics() {
    return monitoring;
  }

  /**
   * Called to encode URL based on session tracking.
   *
   * @param request
   *          the current request
   * @param url
   *          the URL to encode
   * @return encoded URL
   */
  public String encodeUrl(RequestWithSession request, String url) {
    return tracking.encodeUrl(request, url);
  }

 
  
  /**
   * Called to shutdown the session manager and perform needed cleanup.
   */
  
  public void reset() {
	  if (reporter != null) {
		  reporter.stop();
		  reporter.close();
	  }
	  
	  repository.reset();	  
	  executors.shutdown();
  }  
  /**
   * Called to shutdown the session manager and perform needed cleanup.
   */
  @Override
  public void close() {
    if (reporter != null) {
      reporter.close();
    }
    if (repository.cleanSessionsOnShutdown()) {
      for (String sessionId : repository.getOwnedSessionIds()) {
        delete(sessionId, false);
      }
    } else {
      logger.warn("Cleanup of sessions on shutdown is not supported by the session repository {} "
          + "used by session manager {}", repository, this);
    }
    repository.close();
    executors.shutdown();
  }

  /**
   * Changes session id of the passed session. Session id can change only once
   * per request.
   *
   * @param session
   *          the session whose id needs to change
   */
  public void switchSessionId(RepositoryBackedSession session) {
    SessionData sessionData = session.getSessionData();
    boolean switched = false;
    // As there may be multiple concurrent accesses to session
    // we need to synchronize here
    synchronized (sessionData) {
      // Only one session switch per request is allowed.
      if (!sessionData.isIdChanged()) {
        String newId = tracking.newId();
        logger.info("Switching session id {} to {}", sessionData.getId(), newId);
        sessionData.setNewSessionId(newId);
        putIdInLoggingMdc(newId);
        switched = true;
      } else {
        logger.warn("Session id was already switched for session: {}", sessionData);
      }
    }
    // Notify about switch
    if (switched) {
      repository.sessionIdChange(sessionData);
      factory.sessionIdChange(sessionData);
      notifier.sessionIdChanged(session, sessionData.getOldSessionId());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder().append("SessionManager [namespace=").append(configuration.getNamespace()).append("]")
        .toString();
  }

  public void remove(SessionData sessionData) {
    markSessionDeletion( sessionData.getId() );
    getRepository().remove(sessionData);
  }
  
  public boolean isConnected(){
    return this.repository.isConnected();
  }
  
}
