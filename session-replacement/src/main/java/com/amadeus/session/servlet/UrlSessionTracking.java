package com.amadeus.session.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionTracking;

/**
 * This class propagates session id using URL re-writing. The session is
 * appended to path element of the URL and the format of the session element is
 * <code>;&lt;id-name&gt;=&lt;sessionId&gt;</code>. The <code>id-name</code> is
 * specified in order of priority:
 * <ul>
 * <li>using {@link SessionConfiguration#SESSION_ID_NAME} {@link ServletContext}
 * initialization parameter
 * <li>using {@link SessionConfiguration#SESSION_ID_NAME} system property
 * <li>value of {@link SessionConfiguration#DEFAULT_SESSION_ID_NAME}
 * </ul>
 */
public class UrlSessionTracking extends BaseSessionTracking implements SessionTracking {
  private String sessionIdPathItem;

  @Override
  public void configure(SessionConfiguration configuration) {
    super.configure(configuration);

    sessionIdPathItem = ";" + idName + '=';
  }

  @Override
  public String retrieveId(RequestWithSession request) {
    String requestUri = ((HttpServletRequest)request).getRequestURI();
    int sessionIdStart = requestUri.lastIndexOf(sessionIdPathItem);
    if (sessionIdStart > -1) {
      sessionIdStart += sessionIdPathItem.length();
      String sessionId = requestUri.substring(sessionIdStart);
      return clean(sessionId);
    }
    return null;
  }

  @Override
  public void propagateSession(RequestWithSession request, Object response) {
    // No logic here, as the session is propagated via URL
  }

  @Override
  public String encodeUrl(RequestWithSession request, String url) {
    RepositoryBackedSession session = request.getRepositoryBackedSession(false);
    if (session == null || !session.isValid()) {
      return url;
    }
    String encodedSessionAlias;
    try {
      encodedSessionAlias = URLEncoder.encode(session.getId(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("This exception should never happen!", e);
    }

    int queryStart = url.indexOf('?');
    if (queryStart < 0) {
      return url + sessionIdPathItem + encodedSessionAlias;
    }
    String path = url.substring(0, queryStart);
    String query = url.substring(queryStart + 1, url.length());
    path += sessionIdPathItem + encodedSessionAlias;

    return path + '?' + query;
  }
}
