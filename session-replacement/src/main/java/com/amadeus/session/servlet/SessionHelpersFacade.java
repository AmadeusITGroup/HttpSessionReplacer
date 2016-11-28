package com.amadeus.session.servlet;

import java.util.EventListener;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains various methods that are called either from session
 * enabled filters, or from code injected by <code>SessionAgent</code>.
 */
public final class SessionHelpersFacade {

  private static final Logger logger = LoggerFactory.getLogger(SessionHelpersFacade.class);

  private static final String DISABLED_SESSION = "com.amadeus.session.disabled";

  static boolean disabled = Boolean.parseBoolean(getPropertySecured(DISABLED_SESSION));

  private static String getPropertySecured(String key) {
    try {
      return System.getProperty(key, null);
    } catch (SecurityException e) {
      logger.info("Security exception when trying to get system property", e);
      return null;
    }
  }

  private SessionHelpersFacade() {
  }

  /**
   * This method is called from {@link SessionFilter} or from {@link Filter}
   * implementations modified by SessionAgent. The method wraps
   * {@link ServletRequest} in {@link HttpRequestWrapper}.
   * <p>
   * The method will wrap request at most once per request and will only wrap
   * instances of {@link HttpServletRequest}.
   *
   * @param request
   *          request received by filter
   * @param response
   *          response received by filter
   * @param filterContext
   *          {@link ServletContext} used when filter was initialized
   * @return wrapped or original request
   */
  public static ServletRequest prepareRequest(ServletRequest request, ServletResponse response,
      ServletContext filterContext) {
    return helpers(context(request, filterContext)).prepareRequest(request, response, filterContext);
  }

  private static ServletContext context(ServletRequest request, ServletContext context) {
    if (ServletLevel.isServlet3) {
      ServletContext sc = request.getServletContext();
      return sc == null ? context : sc;
    }
    return context;
  }

  /**
   * This method is called from {@link SessionFilter} or from {@link Filter}
   * implementations modified by SessionAgent. The method retrieves response
   * stored in {@link HttpRequestWrapper}.
   *
   * @param request
   *          request received by filter
   * @param response
   *          response received by filter
   * @param filterContext
   *          servlet context of the filter
   * @return wrapped or original response
   */
  public static ServletResponse prepareResponse(ServletRequest request, ServletResponse response,
      ServletContext filterContext) {
    return helpers(context(request, filterContext)).prepareResponse(request, response);
  }

  /**
   * This method initializes session management for a given
   * {@link ServletContext}. This method is called from
   * {@link SessionFilter#init(javax.servlet.FilterConfig)}.
   *
   * @param servletContext
   *          the servlet context where filter is registered
   *
   */
  public static void initSessionManagement(ServletContext servletContext) {
    helpers(servletContext).initSessionManagement(servletContext);
  }

  /**
   * Commits request and stores session in repository. This method is called
   * from the filter. The commit is only done if the filter is the one that
   * wrapped the request into HttpRequestWrapper.
   * <p>
   * The logic to check if the caller filter is the one that wrapped request is
   * based on requirement that original request and the one used by filter are
   * different and that original request is not {@link HttpRequestWrapper}.
   *
   * @param request
   *          potentially wrapped request
   * @param oldRequest
   *          original request received by filter
   * @param filterContext
   *          servlet context of the filter
   */
  public static void commitRequest(ServletRequest request, ServletRequest oldRequest, ServletContext filterContext) {
    helpers(context(request, filterContext)).commitRequest(request, oldRequest);
  }

  /**
   * Call to this method is injected by agent into implementations of
   * {@link HttpSessionAttributeListener} and {@link HttpSessionListener} inside
   * Servlet 2.5 containers. It's roll is to collect session listeners so they
   * can be invoked by the library when it manages sessions.
   *
   * @param listener
   *          listener where event was received
   * @param event
   *          event that was received
   */
  public static void interceptHttpListener(Object listener, HttpSessionEvent event) {
    if (!(listener instanceof EventListener)) {
      logger.error("Tried registering listener {} but it was not EventListener", listener);
      return;
    }
    helpers(event.getSession().getServletContext()).interceptHttpListener((EventListener)listener, event);
  }

  /**
   * This method is used by injected code to register listeners for
   * {@link ServletContext}. If object argument is a {@link ServletContext} and
   * listener argument contains {@link HttpSessionListener} or
   * {@link HttpSessionAttributeListener}, the method will add them to list of
   * known listeners associated to {@link ServletContext}
   *
   * @param object
   *          the object that should be servlet context
   * @param listener
   *          the listener object
   */
  public static void onAddListener(Object object, Object listener) {
    if (!(object instanceof ServletContext)) {
      logger.error("Tried registering listener {} for object {} but object was not ServletContext", listener, object);
      return;
    }
    ServletContext servletContext = (ServletContext)object;
    helpers(servletContext).onAddListener(servletContext, listener);
  }

  /**
   * This method retrieves or creates session helpers associated to servlet
   * context {@link SessionHelpers}.
   *
   * @param servletContext
   *          the active servlet context
   * @return session helpers associated to servlet context
   */
  private static SessionHelpers helpers(ServletContext servletContext) {
    SessionHelpers helpers = (SessionHelpers)servletContext.getAttribute(SessionHelpers.SESSION_HELPERS);
    if (helpers == null) {
      helpers = new SessionHelpers();
    }
    return helpers;
  }
}
