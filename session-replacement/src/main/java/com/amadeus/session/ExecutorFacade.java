package com.amadeus.session;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

/**
 * Support class that provides methods for launching and scheduling of tasks.
 * The implementation will use either managed thread factory when supported by
 * JEE container, or {@link Executors#defaultThreadFactory()} if the managed one
 * was not available.
 * <p>
 * The implementation also provides metrics about number of thread in pool and
 * number of active threads.
 * </p>
 */
public class ExecutorFacade implements UncaughtExceptionHandler, ThreadFactory {
  private static final Logger logger = LoggerFactory.getLogger(ExecutorFacade.class);

  private static final String THREAD_JNDI = "com.amadeus.session.thread.jndi";
  private static final String WORK_QUEUE_SIZE = "com.amadeus.session.thread.queue";
  private static final String METRIC_PREFIX = "com.amadeus.session";

  private static final int WAIT_FOR_SHUTDOWN = 10;
  private static final int CORE_THREADS_IN_POOL = 4;
  private static final int SCHEDULER_THREADS_IN_POOL = 2;
  private static final int THREAD_KEEPALIVE_TIME = 10;
  private static final int MAXIMUM_THREADS_IN_POOL = 40;
  private static final String MAXIMUM_WORK_QUEUE_SIZE = String.valueOf(100);

  private final ThreadPoolExecutor executor;
  private final ScheduledThreadPoolExecutor scheduledExecutor;
  private final ThreadFactory baseThreadFactory;
  private final String namespace;
  private final AtomicLong count;

  /**
   * Default constructor
   *
   * @param conf
   *          namespace for metrics
   */
  public ExecutorFacade(SessionConfiguration conf) {
    ThreadFactory tf;
    count = new AtomicLong(0);
    this.namespace = conf.getNamespace();
    String threadFactoryJndi = conf.getAttribute(THREAD_JNDI, "java:comp/DefaultManagedThreadFactory");
    try {
      // Try to get JEE 6 managed thread factory,
      // and if not available, fall back to built-in default one.
      tf = InitialContext.doLookup(threadFactoryJndi);
    } catch (NamingException e) {
      logger.warn("Unable to use ManagedThreadFactory from JNDI {}, using built-in thread pool. Cause: '{}'. "
          + "Activate debug tracing for more information.", threadFactoryJndi, e.getMessage());
      logger.debug("Unable to use ManagedThreadFactory from JNDI, stack trace follows. ", e);
      tf = Executors.defaultThreadFactory();
    }
    baseThreadFactory = tf;
    int queueSize = Integer.parseInt(conf.getAttribute(WORK_QUEUE_SIZE, MAXIMUM_WORK_QUEUE_SIZE));
    ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueSize);
    executor = new ThreadPoolExecutor(CORE_THREADS_IN_POOL, MAXIMUM_THREADS_IN_POOL, THREAD_KEEPALIVE_TIME, SECONDS,
        workQueue, this, new ThreadPoolExecutor.CallerRunsPolicy());
    scheduledExecutor = new ScheduledThreadPoolExecutor(SCHEDULER_THREADS_IN_POOL, this, new DiscardAndLog());
  }

  /**
   * This method creates new thread from pool and add namespace to the thread
   * name.
   */
  @Override
  public Thread newThread(Runnable r) {
    Thread thread = baseThreadFactory.newThread(r);
    thread.setName("pool-" + ExecutorFacade.this.namespace + "-" + count.getAndIncrement());
    thread.setUncaughtExceptionHandler(ExecutorFacade.this);
    return thread;
  }

  /**
   * Submits a Runnable task for execution and returns a Future representing
   * that task. The Future's {@code get} method will return {@code null} upon
   * <em>successful</em> completion.
   *
   * @param task
   *          the task to submit
   * @return a Future representing pending completion of the task
   * @throws RejectedExecutionException
   *           if the task cannot be scheduled for execution
   * @throws NullPointerException
   *           if the task is null
   */
  public Future<?> submit(Runnable task) {
    return executor.submit(task);
  }

  /**
   * Creates and executes a periodic action that becomes enabled first after the
   * given initial delay, and subsequently with the given period; that is
   * executions will commence after {@code initialDelay} then
   * {@code initialDelay+period}, then {@code initialDelay + 2 * period}, and so
   * on. If any execution of the task encounters an exception, subsequent
   * executions are suppressed. Otherwise, the task will only terminate via
   * cancellation or termination of the executor. If any execution of this task
   * takes longer than its period, then subsequent executions may start late,
   * but will not concurrently execute.
   *
   * @param task
   *          the task to execute
   * @param initialDelay
   *          the time to delay first execution
   * @param period
   *          the period between successive executions
   * @param unit
   *          the time unit of the initialDelay and period parameters
   * @return a ScheduledFuture representing pending completion of the task, and
   *         whose {@code get()} method will throw an exception upon
   *         cancellation
   * @throws RejectedExecutionException
   *           if the task cannot be scheduled for execution
   * @throws NullPointerException
   *           if command is null
   * @throws IllegalArgumentException
   *           if period less than or equal to zero
   */
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
    return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
  }

  /**
   * Starts monitoring executor pools.
   *
   * @param metrics
   *          the registry to use for metrics
   */
  void startMetrics(MetricRegistry metrics) {
    monitorTreadPoolExecutor(name(METRIC_PREFIX, "threads"), executor, metrics);
    monitorTreadPoolExecutor(name(METRIC_PREFIX, "scheduled-threads"), scheduledExecutor, metrics);
    metrics.register(name(METRIC_PREFIX, "scheduled-threads", "tasks"), new Gauge<Long>() {
      @Override
      public Long getValue() {
        return scheduledExecutor.getTaskCount();
      }
    });
  }

  /**
   * Registers monitoring for {@link ThreadPoolExecutor} using passed
   * {@link MetricRegistry}.
   *
   * @param name
   *          the prefix for the metrics
   * @param pool
   *          pool that is monitored
   * @param metrics
   *          registry used for metrics
   */
  private void monitorTreadPoolExecutor(String name, final ThreadPoolExecutor pool, MetricRegistry metrics) {
    // If this was JDK 1.8+ only, this would use lambdas
    metrics.register(name(name, "active"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return pool.getActiveCount();
      }
    });
    metrics.register(name(name, "largest"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return pool.getLargestPoolSize();
      }
    });
    metrics.register(name(name, "pool"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return pool.getPoolSize();
      }
    });
    metrics.register(name(name, "waiting"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return pool.getQueue().size();
      }
    });
  }

  /**
   * Helper class that is used to discard tasks for which there are no free
   * threads. The implementation will simply log this occurrence.
   */
  static class DiscardAndLog implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      logger.error("Discarding submitted task: {}", r);
    }
  }

  /**
   * The method will log uncaught exceptions that occurred during thread
   * execution.
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    logger.error("Uncaught exeception occured while execting thread " + t, e);
    if (e instanceof Error) {
      throw (Error)e;
    }
  }

  /**
   * Called to shutdown the executor and finish all submitted tasks.
   */
  public void shutdown() {
    logger.info("Shutting down the executor.");
    executor.shutdown();
    scheduledExecutor.shutdown();
    try {
      executor.awaitTermination(WAIT_FOR_SHUTDOWN, SECONDS);
      scheduledExecutor.awaitTermination(WAIT_FOR_SHUTDOWN, SECONDS);
    } catch (InterruptedException e) { // NOSONAR Termination was interrupted  
      logger.error("Task termination thread was interrupted.", e);
    }
  }
}
