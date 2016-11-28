package com.amadeus.session.agent;

import static java.lang.String.format;

import java.lang.instrument.Instrumentation;
import java.text.Format;

/**
 * This agent enables instrumentation of ServletContext and Filter classes. See
 * {@link FilterAdapter} and {@link ServletContextAdapter} for details of
 * changes and {@link SessionSupportTransformer} for instrumentation logic.
 *
 * <p>
 * To use agent, add following to the options passed to JVM:
 * <code>-javagent:session-agent.jar=arg,arg,arg</code>
 * <p>
 * Supported arguments:
 *</p>
 * <ul>
 * <li>provider=factory class for repository or name of the provider.
 * See @SessionRepositoryProvider annotation.
 * <li>log=debug activates debug mode
 * <li>timeout=default maximum inactive interval
 * <li>distributable=true if by default all sessions should be distributed,
 * false if it should be managed by web.xml
 * </ul>
 * <p>
 * The agent will set following system properties:
 * <ul>
 * <li>com.amadeus.session.repository.conf - specifies configuration for
 * repository provider. If not set, will be set to the value of the agent
 * argument string.
 * <li>com.amadeus.session.repository.factory - specifies factory class for
 * repository provider. Can be overridden by agent parameter
 * <code>provider</code>.
 * <li>com.amadeus.session.timeout - default maximum inactive interval. Can be
 * overridden by agent parameter <code>timeout</code>.
 * <li>com.amadeus.session.distributable - 'true' if all sessions should be
 * distributed. Can be overridden by agent parameter <code>timeout</code>.
 * <li>com.amadeus.session.disable - 'true' if session replacement should be
 * disabled. Can be overridden by agent parameter <code>disabled</code>.
 * <li>com.amadeus.session.intercept.listeners - Activates discovery of
 * listeners via interception of native session. Used to support Servlet 2.x
 * containers as well as containers that don't use addListner(Object) to
 * register listeners. Additional code injection is done into
 * HttpSessionListeners classes to support detecting of active listeners. Can be
 * overridden by agent parameter <code>interceptListeners</code>.
 * </ul>
 */
public final class SessionAgent {
  static final String DEBUG_ACTIVE = "com.amadeus.session.debug";
  static final String SESSION_FACTORY = "com.amadeus.session.repository.factory";
  static final String SESSION_DISTRIBUTABLE = "com.amadeus.session.distributable";
  static final String SESSION_TIMEOUT = "com.amadeus.session.timeout";
  static final String SESSION_MANAGEMENT_DISABLED = "com.amadeus.session.disabled";
  static final String REPOSITORY_CONF_PROPERTY = "com.amadeus.session.repository.conf";
  static final String INTERCEPT_LISTENER_PROPERTY = "com.amadeus.session.intercept.listeners";
  private static final String PROVIDER = "provider=";
  private static final String TIMEOUT = "timeout=";
  private static final String DISTRIBUTABLE = "distributable=";
  private static final String DISABLED = "disabled=";
  private static final String INTERCEPT_LISTENER = "interceptListeners=";

  static boolean agentActive;
  static boolean debugMode;
  static boolean interceptListener;

  private SessionAgent() {
    // hide implicit constructor
  }

  /**
   * Main entry point for the agent. It will parse arguments and add
   * {@link SessionSupportTransformer} to instrumentation. System property
   * com.amadeus.session.disabled can be used to deactivate the agent.
   *
   * @param agentArgs
   *          arguments passed on command line
   * @param inst
   *          the JVM instrumentation instance
   */
  public static void premain(String agentArgs, Instrumentation inst) {
    if (agentActive) {
      throw new IllegalStateException("Agent started multiple times.");
    }
    agentActive = true;
    debugMode = Boolean.parseBoolean(System.getProperty(DEBUG_ACTIVE));
    readArguments(agentArgs);
    debug("Agent arguments: %s", agentArgs);

    boolean disabled = Boolean.parseBoolean(System.getProperty(SESSION_MANAGEMENT_DISABLED));
    interceptListener = Boolean.parseBoolean(System.getProperty(INTERCEPT_LISTENER_PROPERTY));

    if (!disabled) {
      debug("Code transformation is active");
      if (interceptListener) {
        debug("Will modify listeners to capture registered ones.");
      }

      inst.addTransformer(new SessionSupportTransformer(interceptListener));
    } else {
      debug("Agent is disabled.");
    }
  }

  /**
   * Parses arguments. Arguments are separated with commas.
   *
   * @param agentArgs
   *          the agent arguments from command line
   */
  static void readArguments(String agentArgs) {
    if (agentArgs == null) {
      return;
    }
    String[] args = agentArgs.split(",");
    for (String arg : args) {
      parseArgument(agentArgs, arg.trim());
    }
    if (!agentArgs.isEmpty() && System.getProperty(REPOSITORY_CONF_PROPERTY) == null) {
      System.setProperty(REPOSITORY_CONF_PROPERTY, agentArgs);
    }
  }

  private static void parseArgument(String agentArgs, String arg) {
    if ("log=debug".equals(arg)) {
      debugMode = true;
      System.setProperty(DEBUG_ACTIVE, "true");
    } else {
      normalArguments(agentArgs, arg);
    }
  }

  private static void normalArguments(String agentArgs, String arg) {
    if (arg.startsWith(TIMEOUT)) {
      parseTimeout(agentArgs, arg);
    } else if (arg.startsWith(DISTRIBUTABLE)) {
      System.setProperty(SESSION_DISTRIBUTABLE, arg.substring(DISTRIBUTABLE.length()));
    } else if (arg.startsWith(PROVIDER)) {
      System.setProperty(SESSION_FACTORY, arg.substring(PROVIDER.length()));
    } else if (arg.startsWith(DISABLED)) {
      System.setProperty(SESSION_MANAGEMENT_DISABLED, arg.substring(DISABLED.length()));
    } else if (arg.startsWith(INTERCEPT_LISTENER)) {
      System.setProperty(INTERCEPT_LISTENER_PROPERTY, arg.substring(INTERCEPT_LISTENER.length()));
    } else if (!arg.isEmpty()) {
      throw new IllegalArgumentException("Unrecognized argument " + arg + ". Full agent arguments were: " + agentArgs);
    }
  }

  static void parseTimeout(String agentArgs, String arg) {
    try {
      int timeout = Integer.parseInt(arg.substring(TIMEOUT.length()));
      System.setProperty(SESSION_TIMEOUT, Integer.toString(timeout));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Unrecognized timeout in argument " + arg + ". Full agent arguments were: " + agentArgs);
    }
  }

  /**
   * Used to log debug information from agent or injected code. Custom log
   * implementation that doesn't depend on a logging framework.
   *
   * @param format
   *          {@link Format} for message
   * @param args
   *          message arguments
   */
  public static void debug(String format, Object... args) {
    if (debugMode) {
      System.out.println(format("SessionAgent: %s", format(format, args))); // NOSONAR
      if (args != null && args.length > 1 && args[args.length - 1] instanceof Throwable) {
        ((Throwable)args[args.length - 1]).printStackTrace(System.out); // NOSONAR
      }
    }
  }

  /**
   * Used to log errors from agent or injected code. Custom log implementation
   * that doesn't depend on a logging framework.
   *
   * @param format
   *          {@link Format} for message
   * @param args
   *          message arguments
   */
  public static void error(String format, Object... args) {
    System.err.println(format("SessionAgent: [ERROR] %s", format(format, args))); // NOSONAR
    if (args != null && args.length > 1 && args[args.length - 1] instanceof Throwable) {
      ((Throwable)args[args.length - 1]).printStackTrace(System.err); // NOSONAR
    }
  }

  /**
   * Returns <code>true</code> if agent is active
   *
   * @return <code>true</code> if agent is active
   */
  static boolean isAgentActive() {
    return agentActive;
  }

  /**
   * Returns <code>true</code> if debug logging for agent or injected code is
   * active.
   *
   * @return <code>true</code> if debug logging for agent or injected code is
   *         active.
   */
  public static boolean isDebugMode() {
    return debugMode;
  }
}
