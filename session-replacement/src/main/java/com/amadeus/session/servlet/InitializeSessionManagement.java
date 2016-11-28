package com.amadeus.session.servlet;

import static com.amadeus.session.SessionConfiguration.DISABLED_SESSION;
import static com.amadeus.session.SessionConfiguration.getPropertySecured;
import static com.amadeus.session.servlet.Attributes.PROVIDERS;
import static com.amadeus.session.servlet.SessionHelpersFacade.initSessionManagement;

import java.util.HashMap;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.SessionRepositoryFactory;

/**
 * This class adds default filter if no filters were registered by the
 * container.
 */
public class InitializeSessionManagement implements ServletContainerInitializer {
  private static final Logger logger = LoggerFactory.getLogger(InitializeSessionManagement.class);

  private static boolean disabled = Boolean.parseBoolean(getPropertySecured(DISABLED_SESSION, null));

  @Override
  public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
    // If session support is disabled, just log it
    if (disabled || Boolean.parseBoolean(context.getInitParameter(DISABLED_SESSION))) {
      logger.warn("Session management disabled for {}, {}, {}", context.getContextPath(), disabled,
          context.getInitParameter(DISABLED_SESSION));
      return;
    }
    setupProviders(context);
    initSessionManagement(context);
  }

  /**
   * This method will register built-in {@link SessionRepositoryFactory} classes.
   * @param context
   */
  static void setupProviders(ServletContext context) {
    HashMap<String, String> providerMapping = new HashMap<>();
    // Register default implementations
    providerMapping.put("redis", "com.amadeus.session.repository.redis.RedisSessionRepositoryFactory");
    providerMapping.put("in-memory", "com.amadeus.session.repository.inmemory.InMemoryRepositoryFactory");
    if (logger.isDebugEnabled()) {
      logger.debug("Known session repository providers: {} for servlet context {}", providerMapping.keySet(),
          context.getContextPath());
    }
    context.setAttribute(PROVIDERS, providerMapping);
  }
}
