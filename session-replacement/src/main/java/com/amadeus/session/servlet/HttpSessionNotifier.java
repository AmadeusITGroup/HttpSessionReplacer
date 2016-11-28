package com.amadeus.session.servlet;

import java.util.EventListener;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.amadeus.session.RepositoryBackedSession;
import com.amadeus.session.SessionNotifier;

/**
 * This class sends notifications about session changes to known instances of
 * {@link HttpSessionListener} and {@link HttpSessionAttributeListener} that are
 * associated with the servlet context.
 */
class HttpSessionNotifier implements SessionNotifier {
  private ServletContextDescriptor descriptor;

  HttpSessionNotifier(ServletContextDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.http.SessionNotifier#sessionCreated(com.amadeus.session
   * .RepositoryBackedSession)
   */
  @Override
  public void sessionCreated(RepositoryBackedSession session) {
    if (session instanceof HttpSession) {
      HttpSessionEvent event = new HttpSessionEvent((HttpSession)session);
      for (HttpSessionListener listener : descriptor.getHttpSessionListeners()) {
        listener.sessionCreated(event);
      }
    }
  }

  /**
   * Notifies listeners that attribute was added. See {@link SessionNotifier}
   * {@link #attributeAdded(RepositoryBackedSession, String, Object)}.
   * <p>
   * If the added attribute <code>value</code> is a HttpSessionBindingListener,
   * it will receive the {@link HttpSessionBindingEvent}. If there are
   * {@link HttpSessionAttributeListener} instances associated to
   * {@link ServletContext}, they will be notified via
   * {@link HttpSessionAttributeListener#attributeAdded(HttpSessionBindingEvent)}
   * .
   */
  @Override
  public void attributeAdded(RepositoryBackedSession session, String key, Object value) {
    // If the
    if (session instanceof HttpSession && value instanceof HttpSessionBindingListener) {
      ((HttpSessionBindingListener)value).valueBound(new HttpSessionBindingEvent((HttpSession)session, key));
    }
    HttpSessionBindingEvent event = new HttpSessionBindingEvent((HttpSession)session, key, value);
    for (HttpSessionAttributeListener listener : descriptor.getHttpSessionAttributeListeners()) {
      listener.attributeAdded(event);
    }
  }

  /**
   * Notifies listeners that attribute was replaced. See {@link SessionNotifier}
   * {@link #attributeReplaced(RepositoryBackedSession, String, Object)}.
   * <p>
   * If the the old value of attribute <code>replacedValue</code> is a
   * HttpSessionBindingListener, it will receive the
   * {@link HttpSessionBindingEvent}. If there are
   * {@link HttpSessionAttributeListener} instances associated to
   * {@link ServletContext}, they will be notified via
   * {@link HttpSessionAttributeListener#attributeReplaced(HttpSessionBindingEvent)}
   * .
   */
  @Override
  public void attributeReplaced(RepositoryBackedSession session, String key, Object replacedValue) {
    if (session instanceof HttpSession && replacedValue instanceof HttpSessionBindingListener) {
      ((HttpSessionBindingListener)replacedValue).valueUnbound(new HttpSessionBindingEvent((HttpSession)session, key));
    }
    HttpSessionBindingEvent event = new HttpSessionBindingEvent((HttpSession)session, key, replacedValue);
    for (HttpSessionAttributeListener listener : descriptor.getHttpSessionAttributeListeners()) {
      listener.attributeReplaced(event);
    }
  }


  /**
   * Notifies listeners that attribute was removed. See {@link SessionNotifier}
   * {@link #attributeRemoved(RepositoryBackedSession, String, Object)}.
   * <p>
   * If the the old value of attribute <code>removedValue</code> is a
   * HttpSessionBindingListener, it will receive the
   * {@link HttpSessionBindingEvent}. If there are
   * {@link HttpSessionAttributeListener} instances associated to
   * {@link ServletContext}, they will be notified via
   * {@link HttpSessionAttributeListener#attributeRemoved(HttpSessionBindingEvent)}
   * .
   */
  @Override
  public void attributeRemoved(RepositoryBackedSession session, String key, Object removedValue) {
    if (session instanceof HttpSession && removedValue instanceof HttpSessionBindingListener) {
      ((HttpSessionBindingListener)removedValue).valueUnbound(new HttpSessionBindingEvent((HttpSession)session, key));
    }
    HttpSessionBindingEvent event = new HttpSessionBindingEvent((HttpSession)session, key);
    for (HttpSessionAttributeListener listener : descriptor.getHttpSessionAttributeListeners()) {
      listener.attributeRemoved(event);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.amadeus.session.http.SessionNotifier#attributeBeingStored(com.amadeus.
   * session.RepositoryBackedSession, java.lang.String, java.lang.Object)
   */
  @Override
  public void attributeBeingStored(RepositoryBackedSession session, String key, Object value) {
    if (session instanceof HttpSession && value instanceof HttpSessionActivationListener) {
      ((HttpSessionActivationListener)value).sessionWillPassivate(new HttpSessionEvent((HttpSession)session));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.http.SessionNotifier#attributeHasBeenRestored(com.
   * amadeus.session.RepositoryBackedSession, java.lang.String,
   * java.lang.Object)
   */
  @Override
  public void attributeHasBeenRestored(RepositoryBackedSession session, String key, Object value) {
    if (session instanceof HttpSession && value instanceof HttpSessionActivationListener) {
      ((HttpSessionActivationListener)value).sessionDidActivate(new HttpSessionEvent((HttpSession)session));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.amadeus.session.http.SessionNotifier#sessionDestroyed(com.amadeus.
   * session.RepositoryBackedSession, boolean)
   */
  @Override
  public void sessionDestroyed(RepositoryBackedSession session, boolean shutdown) {
    if (session instanceof HttpSession) {
      // We notify all session attribute listeners that each attribute is removed
      for (String key : session.getAttributeNamesWithValues()) {
        HttpSessionBindingEvent event = new HttpSessionBindingEvent((HttpSession)session, key);
        for (HttpSessionAttributeListener listener : descriptor.getHttpSessionAttributeListeners()) {
          listener.attributeRemoved(event);
        }
      }
      // Notifying HttpSessionListeners. If we are doing shutdown, as per
      // Servlet specification, we notify listeners in reverse order
      HttpSessionEvent event = new HttpSessionEvent((HttpSession)session);
      if (shutdown) {
        List<HttpSessionListener> listeners = descriptor.getHttpSessionListeners();
        for (int i = listeners.size() - 1; i >= 0; i--) {
          listeners.get(i).sessionDestroyed(event);
        }
      } else {
        for (HttpSessionListener listener : descriptor.getHttpSessionListeners()) {
          listener.sessionDestroyed(event);
        }
      }
    }
  }

  @Override
  public void sessionIdChanged(RepositoryBackedSession session, String oldId) {
    // Session id change is only supported for Servlet 3.1+
    if (!ServletLevel.isServlet31) {
      return;
    }
    if (session instanceof HttpSession) {
      HttpSessionEvent event = new HttpSessionEvent((HttpSession)session);
      for (EventListener listener : descriptor.getHttpSessionIdListeners()) {
        ((HttpSessionIdListener)listener).sessionIdChanged(event, oldId);
      }
    }
  }
}
