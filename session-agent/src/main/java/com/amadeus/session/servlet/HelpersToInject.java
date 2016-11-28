package com.amadeus.session.servlet;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * This class contains various methods that are called either from session
 * enabled filters, or from code injected by <code>SessionAgent</code>.
 */
public class HelpersToInject {
  private static final String SESSION_HELPERS = "com.amadeus.session.servlet.SessionHelpers";
  private static final String SESSION_HELPER_METHODS = "com.amadeus.session.servlet.SessionHelpers.methods";

  private static boolean $$isServlet3; // NOSONAR Prefer this naming convention
                                       // here

  static {
    $$isServlet3 = $$isServlet3();
  }

  public final static class FilterHelpers {
    private static final int INIT_SESSION_MANAGEMENT_IF_NEEDED = -1;
    private static final int PREPARE_RESPONSE = 3;
    private static final int PREPARE_REQUEST = 4;
    private static final int COMMIT_REQUEST = 5;
    private static final String DEBUG_ACTIVE = "com.amadeus.session.debug";

    public static boolean $$debugMode;

    static {
      $$debugMode = Boolean.parseBoolean($$getPropertySecured(DEBUG_ACTIVE));
      $$isServlet3 = $$isServlet3();
    }

    /**
     * This method is called from SessionFilter or from instrumented
     * javax.servlet.Filter implementations modified by SessionAgent. The method
     * wraps javax.servlet.ServletRequest in HttpRequestWrapper.
     * <p>
     * The method will wrap request at most once per request and will only wrap
     * instances of {@link HttpServletRequest}.
     * </p>
     *
     * @param request
     *          request received by filter
     * @param response
     *          response received by filter
     * @param filterContext
     *          {@link ServletContext} used when filter was initialized
     * @return wrapped or original request
     */
    public static ServletRequest $$prepareRequest(ServletRequest request, ServletResponse response,
        ServletContext filterContext) {
      return (ServletRequest)$$call($$context(request, filterContext), PREPARE_REQUEST, request, response,
          filterContext);
    }

    private static ServletContext $$context(ServletRequest request, ServletContext context) {
      if ($$isServlet3) {
        ServletContext sc = request.getServletContext();
        return sc == null ? context : sc;
      }
      return context;
    }

    /**
     * This method is called from SessionFilter or from instrumented
     * javax.servlet.Filter implementations modified by SessionAgent. The method
     * retrieves response stored in HttpRequestWrapper.
     *
     * @param request
     *          request received by filter
     * @param response
     *          response received by filter
     * @param filterContext
     *          servlet context of the filter
     * @return wrapped or original response
     */
    public static ServletResponse $$prepareResponse(ServletRequest request, ServletResponse response,
        ServletContext filterContext) {
      return (ServletResponse)$$call($$context(request, filterContext), PREPARE_RESPONSE, request, response);
    }

    /**
     * This method initializes session management for a given
     * {@link ServletContext}. This method is called from
     * SessionFilter.init(javax.servlet.FilterConfig).
     *
     * @param servletContext
     *          servlet context of the filter
     */
    public static void $$initSessionManagement(ServletContext servletContext) {
      $$call(servletContext, INIT_SESSION_MANAGEMENT_IF_NEEDED, servletContext);
    }

    /**
     * Commits request and stores session in repository. This method is called
     * from the filter. The commit is only done if the filter is the one that
     * wrapped the request into HttpRequestWrapper.
     * <p>
     * The logic to check if the caller filter is the one that wrapped request
     * is based on requirement that original request and the one used by filter
     * are different and that original request is not HttpRequestWrapper .
     *
     * @param request
     *          potentially wrapped request
     * @param oldRequest
     *          original request received by filter
     * @param filterContext
     *          servlet context of the filter
     */
    public static void $$commitRequest(ServletRequest request, ServletRequest oldRequest,
        ServletContext filterContext) {
      $$call($$context(request, filterContext), COMMIT_REQUEST, request, oldRequest);
    }

    /**
     * Called to send debug information from instrumented call. Uses standard out for output.
     *
     * @param string
     *          String.format message format
     * @param args
     *          arguments for message
     */
    private static void $$debug(String format, Object... args) {
      if ($$debugMode) {
        System.out.println(String.format("SessionAgent: %s", String.format(format, args))); // NOSONAR
        if (args != null && args.length > 1 && args[args.length - 1] instanceof Throwable) {
          ((Throwable)args[args.length - 1]).printStackTrace(System.out); // NOSONAR
        }
      }
    }

    private static String $$getPropertySecured(String key) {
      try {
        return System.getProperty(key, null);
      } catch (SecurityException e) {
        $$debug("Security exception when trying to get system property", e);
        return null;
      }
    }

  }

  static class ListenerHelpers {
    private static final int INTERCEPT_HTTP_LISTENER = 1;

    /**
     * Call to this method is injected by agent into implementations of
     * {@link HttpSessionAttributeListener} and {@link HttpSessionListener}
     * inside Servlet 2.5 containers. Its role is to collect session listeners
     * so they can be invoked by the library when it manages sessions.
     *
     * @param listener
     *          listener where event was received
     * @param event
     *          event that was received
     */
    public static void $$interceptHttpListener(Object listener, HttpSessionEvent event) {
      if (!(listener instanceof EventListener)) {
        $$error("Tried registering listener %s but it was not EventListener", listener);
        return;
      }
      $$call(event.getSession().getServletContext(), INTERCEPT_HTTP_LISTENER, listener, event);
    }
  }

  static class ServletContextHelpers {
    private static final int ON_ADD_LISTENER = 0;

    /**
     * This method is used by injected code to register listeners for
     * {@link ServletContext}. If object argument is a {@link ServletContext}
     * and listener argument contains {@link HttpSessionListener} or
     * {@link HttpSessionAttributeListener}, the method will add them to list of
     * known listeners associated to {@link ServletContext}
     *
     * @param object
     *          the current servlet context
     * @param listener
     *          the listener that where the code was injected
     */
    public static void $$onAddListener(Object object, Object listener) {
      if (!(object instanceof ServletContext)) {
        $$error("Tried registering listener %s for object %s but object was not ServletContext", listener, object);
        return;
      }
      ServletContext servletContext = (ServletContext)object;
      $$call(servletContext, ON_ADD_LISTENER, servletContext, listener);
    }
  }

  /**
   * This method is used to invoke directly methods that are in SessionHelper
   * object.
   *
   * @param servletContext
   *          the current servlet context
   * @param key
   *          index of the method to call
   * @param args
   *          arguments to pass to method
   * @return the method result
   */
  private static Object $$call(ServletContext servletContext, int key, Object... args) {
    MethodHandle[] methods = (MethodHandle[])servletContext.getAttribute(SESSION_HELPER_METHODS);
    if (methods == null) {
      ClassLoader classLoader = null;
      if ($$isServlet3) {
        classLoader = servletContext.getClassLoader();
      }
      if (classLoader == null) {
        classLoader = Thread.currentThread().getContextClassLoader();
      }
      try {
        Class<?> clazz = classLoader.loadClass(SESSION_HELPERS);
        Object instance = clazz.newInstance();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = methodType(MethodHandle[].class, ServletContext.class);
        MethodHandle initSessionManagement = lookup.bind(instance, "initSessionManagement", mt);
        methods = (MethodHandle[])initSessionManagement.invokeWithArguments(servletContext);
      } catch (InstantiationException e) {
        throw $$invalidState(e);
      } catch (IllegalAccessException e) {
        throw $$invalidState(e);
      } catch (ClassNotFoundException e) {
        throw $$invalidState(e);
      } catch (NoSuchMethodException e) {
        throw $$invalidState(e);
      } catch (Error e) { // NOSONAR
        throw e;
      } catch (RuntimeException e) {
        throw e;
      } catch (Throwable e) { // NOSONAR
        throw $$invalidState(e);
      }
    }
    if (key < 0) {
      return null;
    }
    try {
      return methods[key].invokeWithArguments(args);
    } catch (Error e) { // NOSONAR
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) { // NOSONAR
      throw $$invalidState(e);
    }
  }

  private static IllegalStateException $$invalidState(Throwable e) {
    return new IllegalStateException("Unable to load or instrument " + SESSION_HELPERS, e);
  }

  /**
   * Called to send error information from instrumented call. Uses STDERR for output.
   *
   * @param format
   *          String.format message format
   * @param args
   *          arguments for message
   */
  public static void $$error(String format, Object... args) {
    System.err.println(String.format("SessionAgent: [ERROR] %s", String.format(format, args))); // NOSONAR
    if (args != null && args.length > 1 && args[args.length - 1] instanceof Throwable) {
      ((Throwable)args[args.length - 1]).printStackTrace(System.err); // NOSNAR
    }
  }

  /**
   * Returns true if the Servlet 3 APIs are detected.
   *
   * @return true if the Servlet 3 API is detected
   */
  private static boolean $$isServlet3() {
    try {
      ServletRequest.class.getMethod("startAsync");
      return true;
    } catch (NoSuchMethodException e) { // NOSONAR no method, so no servlet 3
      return false;
    }
  }
}