package com.amadeus.session.servlet;

import com.amadeus.session.SessionTracking;

/**
 * This enum contains supported session tracking mechanism. Currently COOKIE and URL session
 * tracking are supported. See also {@link CookieSessionTracking} and {@link UrlSessionTracking}.
 */
enum SessionPropagation {
  /**
   * Use cookie based session tracking (session id is stored in cookie).
   */
  COOKIE(CookieSessionTracking.class),
  /**
   * Use url based session tracking (session id is appended to URL).
   */
  URL(UrlSessionTracking.class),
  /**
   * Default session tracking is cookie based
   */
  DEFAULT(CookieSessionTracking.class);

  private Class<? extends SessionTracking> trackingClass;

  private SessionPropagation(Class<? extends SessionTracking> clazz) {
    this.trackingClass = clazz;
  }

  /**
   * Instantiates a {@link SessionTracking} class for this enum
   * @return an instance of {@link SessionTracking} class.
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public SessionTracking get() throws InstantiationException, IllegalAccessException {
    return trackingClass.newInstance();
  }
}
