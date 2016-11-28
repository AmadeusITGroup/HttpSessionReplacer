package com.amadeus.session.servlet;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds all known {@link HttpSessionListener} and
 * {@link HttpSessionAttributeListener} for a given {@link ServletContext}.
 */
class ServletContextDescriptor {
  private static final Logger logger = LoggerFactory.getLogger(ServletContextDescriptor.class);

  private final HashSet<HttpSessionListener> httpSessionListenersSet = new HashSet<>();
  private final HashSet<HttpSessionAttributeListener> httpSessionAttributeListenersSet = new HashSet<>();
  private final HashSet<EventListener> httpSessionIdListenersSet = new HashSet<>();
  private final List<HttpSessionListener> httpSessionListeners = new ArrayList<>();
  private final List<HttpSessionAttributeListener> httpSessionAttributeListeners = new ArrayList<>();
  private final List<EventListener> httpSessionIdListeners = new ArrayList<>();

  private String contextPath;

  ServletContextDescriptor(ServletContext servletContext) {
    contextPath = servletContext.getContextPath();
  }

  /**
   * Adds {@link HttpSessionListener} associated to {@link ServletContext} to
   * list of known listeners. Listener is added only if it has not been seen
   * before.
   *
   * @param listener
   */
  void addHttpSessionListener(HttpSessionListener listener) {
    if (httpSessionListenersSet.add(listener)) {
      httpSessionListeners.add(listener);
      logger.info("Registered HttpSessionListener {} for context {}", listener, contextPath);
    }
  }

  /**
   * Adds {@link HttpSessionAttributeListener} associated to
   * {@link ServletContext} to list of known listeners. Listener is added only
   * if it has not been seen before.
   *
   * @param listener
   */
  void addHttpSessionAttributeListener(HttpSessionAttributeListener listener) {
    if (httpSessionAttributeListenersSet.add(listener)) {
      httpSessionAttributeListeners.add(listener);
      logger.info("Registered HttpSessionAttributeListener {} for context {}", listener, contextPath);
    }
  }

  /**
   * Adds {@link HttpSessionIdListener} associated to {@link ServletContext} to
   * list of known listeners. Listener is added only if it has not been seen
   * before. As session id listeners were introduced in Servlet 3.1, the method
   * takes EventListener as parameter in order to be compatible older
   * containers.
   *
   * @param listener
   */
  void addHttpSessionIdListener(EventListener listener) {
    if (httpSessionIdListenersSet.add(listener)) {
      httpSessionIdListeners.add(listener);
      logger.info("Registered HttpSessionIdListener {} for context {}", listener, contextPath);
    }
  }

  /**
   * Returns list of {@link HttpSessionListener}
   *
   * @return list of listeners or empty if there are nor registered listeners
   */
  List<HttpSessionListener> getHttpSessionListeners() {
    return httpSessionListeners;
  }

  /**
   * Returns list of {@link HttpSessionAttributeListener}
   *
   * @return list of listeners or empty if there are nor registered listeners
   */
  List<HttpSessionAttributeListener> getHttpSessionAttributeListeners() {
    return httpSessionAttributeListeners;
  }

  /**
   * Returns list of {@link HttpSessionIdListener}. As session id listeners were
   * introduced in Servlet 3.1, the method has EventListener in signature in
   * order to be compatible older containers.
   *
   * @return list of listeners or empty if there are nor registered listeners
   */
  List<EventListener> getHttpSessionIdListeners() {
    return httpSessionIdListeners;
  }

}
