package com.amadeus.session;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class ResetManager {
  private final MetricRegistry monitoring;

  private JmxReporter reporter;

  private final ErrorTracker errorTracker;

  private final Meter create_meter;

  private final Meter notConnected_meter;

  private final Meter connected_meter;

  private final SessionConfiguration configuration;

  protected final ExecutorFacade executors;

  final String SESSIONS_METRIC_MANAGER_PREFIX = "com.amadeus.session.manager";

  final String RESETMANAGER_CREATED_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "initialized");

  final String RESETMANAGER_NOTCONNECTED_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "notConnected");

  final String RESETMANAGER_CONNECTED_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "connected");

  public ResetManager(ExecutorFacade executors, SessionConfiguration configuration) {
    this.executors = executors;

    monitoring = new MetricRegistry();

    this.configuration = configuration;

    create_meter = monitoring.meter(RESETMANAGER_CREATED_METRIC);
    notConnected_meter = monitoring.meter(RESETMANAGER_NOTCONNECTED_METRIC);
    connected_meter = monitoring.meter(RESETMANAGER_CONNECTED_METRIC);

    errorTracker = new ErrorTracker(configuration.getTrackerInterval(), configuration.getTrackerLimits());

    startMonitoring();

    create_meter.mark();
  }

  /**
   * Starts monitoring this session manager. The method will expose all metrics through JMX.
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

  public ErrorTracker getErrorTracker() {
    return errorTracker;
  }

  public void reset() {
    errorTracker.reset();
    create_meter.mark();
  }

  public void connected() {
    connected_meter.mark();
  }

  public void notConnected() {
    notConnected_meter.mark();
  }

}
