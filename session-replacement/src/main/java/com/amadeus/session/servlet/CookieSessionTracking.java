package com.amadeus.session.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionTracking;

/**
 * This class propagates session using HTTP cookies.
 * <p>
 * In case of HTTPS request, cookie is marked as secure. For Servlet 3.x and
 * later containers, cookie is marked as HTTP only.
 * <p>
 * Cookie applies only on the context path of the {@link ServletContext}. I.e.
 * it is only send for URL that are prefixed by context path.
 * <p>
 * The cookie expiration is set only if session has expired, and the value of
 * expiration is 0 (i.e. immediately).
 * <p>
 * The cookie name is decided in order of priority:
 * <ul>
 * <li>using {@link SessionConfiguration#SESSION_ID_NAME} {@link ServletContext}
 * initialization parameter
 * <li>using {@link SessionConfiguration#SESSION_ID_NAME} system property
 * <li>value of {@link SessionConfiguration#DEFAULT_SESSION_ID_NAME}
 * </ul>
 *
 * ServletContext initialization parameters can be used to configure:
 * <ul>
 * <li>if the path of the path of the cookie is using context path or root
 * (com.amadeus.session.cookie.contextPath set to true to use context path). By
 * default cookie applies to root path.
 * <li>using http only cookies (controlled by
 * com.amadeus.session.cookie.httpOnly). By default cookies are httpOnly.
 * </ul>
 *
 * the path of the cookie:
 *
 */
class CookieSessionTracking extends BaseSessionTracking implements SessionTracking {
  static final String DEFAULT_CONTEXT_PATH = "/";
  /**
   * Used to configure context path of the cookie.
   */
  static final String COOKIE_CONTEXT_PATH_PARAMETER = "com.amadeus.session.cookie.contextPath";
  /**
   * Used to specify that cookie should be marked as secure. Secure cookies are propagated only
   * over HTTPS.
   */
  static final String SECURE_COOKIE_PARAMETER = "com.amadeus.session.cookie.secure";
  /**
   * Cookie should be marked as secure only if incoming request is over secure connection.
   */
  static final String SECURE_COOKIE_ON_SECURED_REQUEST_PARAMETER = "com.amadeus.session.cookie.secure.on.secured.request";
  /**
   * Used to specify that cookie should be marked as HttpOnly. Those cookies are not available
   * to javascript.
   */
  static final String COOKIE_HTTP_ONLY_PARAMETER = "com.amadeus.session.cookie.httpOnly";
  private boolean httpOnly = true;
  private String contextPath;
  private Boolean secure;
  private Boolean secureOnlyOnSecuredRequest;

  @Override
  public String retrieveId(RequestWithSession request) {
    Cookie[] cookies = ((HttpServletRequest)request).getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (idName.equals(cookie.getName())) {
          return clean(cookie.getValue());
        }
      }
    }
    return null;
  }

  @Override
  public void propagateSession(RequestWithSession request, Object response) {
    Cookie cookie = new Cookie(idName, "");
    RepositoryBackedSession session = request.getRepositoryBackedSession(false);
    if (session != null && !session.isValid()) {
      session = null;
    }
    if (session == null) {
      cookie.setMaxAge(0);
    } else {
      cookie.setValue(session.getId());
    }
    if (ServletLevel.isServlet3) {
      cookie.setHttpOnly(httpOnly);
    }
    HttpServletRequest httpRequest = (HttpServletRequest)request;
    if (secure) {
      cookie.setSecure(secureOnlyOnSecuredRequest ? httpRequest.isSecure() : true);
    }
    cookie.setPath(cookiePath());
    ((HttpServletResponse)response).addCookie(cookie);
  }

  private String cookiePath() {
    if (contextPath != null) {
      return contextPath;
    }
    return DEFAULT_CONTEXT_PATH;
  }

  @Override
  public void configure(SessionConfiguration conf) {
    super.configure(conf);
    httpOnly = Boolean.valueOf(conf.getAttribute(COOKIE_HTTP_ONLY_PARAMETER, "true"));
    secure = Boolean.valueOf(conf.getAttribute(SECURE_COOKIE_PARAMETER, "false"));
    secureOnlyOnSecuredRequest = Boolean.valueOf(conf.getAttribute(SECURE_COOKIE_ON_SECURED_REQUEST_PARAMETER, "false"));
    contextPath = conf.getAttribute(COOKIE_CONTEXT_PATH_PARAMETER, null);
  }

  @Override
  public boolean isCookieTracking() {
    return true;
  }
}
