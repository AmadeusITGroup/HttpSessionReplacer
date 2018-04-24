package com.amadeus.session.servlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.RepositoryBackedSession;

@SuppressWarnings("javadoc")
public class TestHttpSessionNotifier {

  private HttpSessionNotifier notifier;
  private RepositoryBackedSession session;
  private ServletContextDescriptor descriptor;

  @Before
  public void initMocks() {
    descriptor = new ServletContextDescriptor(mock(ServletContext.class));
    notifier = new HttpSessionNotifier(descriptor);
    session = mock(RepositoryBackedHttpSession.class);
  }

  @Test
  public void testSessionCreated() {
    HttpSessionListener listener = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener);
    notifier.sessionCreated(session);
    verify(listener).sessionCreated(any(HttpSessionEvent.class));
    HttpSessionListener listener2 = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener2);
    notifier.sessionCreated(session);
    verify(listener, times(2)).sessionCreated(any(HttpSessionEvent.class));
    verify(listener2).sessionCreated(any(HttpSessionEvent.class));
  }

  @Test
  public void testAttributeAdded() {
    HttpSessionAttributeListener listener = mock(HttpSessionAttributeListener.class);
    descriptor.addHttpSessionAttributeListener(listener);
    notifier.attributeAdded(session, "Test", "value");
    verify(listener).attributeAdded(any(HttpSessionBindingEvent.class));
    HttpSessionBindingListener bindingListener = mock(HttpSessionBindingListener.class);
    notifier.attributeAdded(session, "Test", bindingListener);
    verify(listener, times(2)).attributeAdded(any(HttpSessionBindingEvent.class));
    verify(bindingListener).valueBound(any(HttpSessionBindingEvent.class));
  }

  @Test
  public void testAttributeReplaced() {
    HttpSessionAttributeListener listener = mock(HttpSessionAttributeListener.class);
    notifier.attributeReplaced(session, "Test", "very-old-value");
    verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
    descriptor.addHttpSessionAttributeListener(listener);
    notifier.attributeReplaced(session, "Test", "old-value");
    verify(listener).attributeReplaced(any(HttpSessionBindingEvent.class));
    HttpSessionBindingListener bindingListener = mock(HttpSessionBindingListener.class);
    notifier.attributeReplaced(session, "Test", bindingListener);
    verify(listener, times(2)).attributeReplaced(any(HttpSessionBindingEvent.class));
    verify(bindingListener).valueUnbound(any(HttpSessionBindingEvent.class));
  }

  @Test
  public void testAttributeRemoved() {
    notifier.attributeRemoved(session, "Test", "very-old-value");
    HttpSessionAttributeListener listener = mock(HttpSessionAttributeListener.class);
    descriptor.addHttpSessionAttributeListener(listener);
    notifier.attributeRemoved(session, "Test", "old-value");
    verify(listener).attributeRemoved(any(HttpSessionBindingEvent.class));
    HttpSessionBindingListener bindingListener = mock(HttpSessionBindingListener.class);
    notifier.attributeRemoved(session, "Test", bindingListener);
    verify(listener, times(2)).attributeRemoved(any(HttpSessionBindingEvent.class));
    verify(bindingListener).valueUnbound(any(HttpSessionBindingEvent.class));
  }

  @Test
  public void testAttributeBeingStored() {
    HttpSessionActivationListener object = mock(HttpSessionActivationListener.class);
    notifier.attributeBeingStored(mock(RepositoryBackedSession.class), "Test", object);
    notifier.attributeBeingStored(session, "Test", "dummy");
    verify(object, never()).sessionWillPassivate(any(HttpSessionEvent.class));
    notifier.attributeBeingStored(session, "Test", object);
    verify(object).sessionWillPassivate(any(HttpSessionEvent.class));
  }

  @Test
  public void testAttributeHasBeenRestored() {
    HttpSessionActivationListener object = mock(HttpSessionActivationListener.class);
    notifier.attributeHasBeenRestored(mock(RepositoryBackedSession.class), "Test", object);
    notifier.attributeHasBeenRestored(mock(RepositoryBackedSession.class), "Test", "dummy");
    notifier.attributeHasBeenRestored(session, "Test", "dummy");
    verify(object, never()).sessionWillPassivate(any(HttpSessionEvent.class));
    notifier.attributeHasBeenRestored(session, "Test", object);
    verify(object).sessionDidActivate(any(HttpSessionEvent.class));
  }

  @Test
  public void testSessionDestroyed() {
    HttpSessionListener listener = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener);
    notifier.sessionDestroyed(session, false);
    verify(listener).sessionDestroyed(any(HttpSessionEvent.class));
    HttpSessionListener listener2 = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener2);
    notifier.sessionDestroyed(session, false);
    verify(listener, times(2)).sessionDestroyed(any(HttpSessionEvent.class));
    verify(listener2).sessionDestroyed(any(HttpSessionEvent.class));
  }

  @Test
  public void testShutdown() {
    HttpSessionListener listener = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener);
    notifier.sessionDestroyed(session, true);
    verify(listener).sessionDestroyed(any(HttpSessionEvent.class));
    HttpSessionListener listener2 = mock(HttpSessionListener.class);
    descriptor.addHttpSessionListener(listener2);
    notifier.sessionDestroyed(session, true);
    verify(listener, times(2)).sessionDestroyed(any(HttpSessionEvent.class));
    verify(listener2).sessionDestroyed(any(HttpSessionEvent.class));
  }
}
