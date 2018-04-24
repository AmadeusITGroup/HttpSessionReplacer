package com.amadeus.session;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class ResetManager {
  private final MetricRegistry monitoring;
  private JmxReporter reporter; 
  private final ErrorTracker errorTracker ;
  
   
  private final Meter create_metrics ;
  private final Meter notConnected_metrics ; 
  private final Meter connected_metrics ;
  
  
  private final SessionConfiguration configuration;
  protected final ExecutorFacade executors;

  public ResetManager(ExecutorFacade executors , SessionConfiguration configuration ) {
    this.executors = executors;

    monitoring = new MetricRegistry ();
    
    String SESSIONS_METRIC_MANAGER_PREFIX = "com.amadeus.session.manager";
    String ResetManager_created_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "initialized");
    
    String ResetManager_notConnected_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "notConnected");
    String ResetManager_Connected_METRIC = name(SESSIONS_METRIC_MANAGER_PREFIX, "connected");
    
    this.configuration = configuration;  
     
    create_metrics = monitoring.meter(ResetManager_created_METRIC);
    notConnected_metrics = monitoring.meter(ResetManager_notConnected_METRIC);
    connected_metrics = monitoring.meter(ResetManager_Connected_METRIC);
    
    errorTracker = new ErrorTracker  ( configuration.getTrackerInterval() , configuration.getTrackerLimits() );
    
    startMonitoring();
    
    create_metrics.mark();
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

  public ErrorTracker getErrorTracker() {
    return errorTracker;
  }

  public void reset() {
    errorTracker.reset();
    create_metrics.mark();    
  }

  public void connected() {
    connected_metrics.mark();        
  }

  public void notConnected() {
    notConnected_metrics.mark();    
  }
  
  
}
