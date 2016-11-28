package com.amadeus.session.sample;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Test;

import com.amadeus.session.servlet.SessionHelpers;

@SuppressWarnings("javadoc")
public class ListenerITInstrument {
  @Test
  public void testSessionListener() throws Exception {
    SessionHelpers.resetForTests();
    SessionListener sl = new SessionListener();
    HttpSessionEvent event = mock(HttpSessionEvent.class);
    HttpSession session = mock(HttpSession.class);
    ServletContext context = new MockServletContext();
    when(session.getServletContext()).thenReturn(context);
    when(event.getSession()).thenReturn(session);
    sl.sessionCreated(event);
    assertEquals(1, SessionHelpers.interceptListener.get());
  }

  @Test
  public void testAttributeListener() throws Exception {
    SessionHelpers.resetForTests();
    AttributeListener sl = new AttributeListener();
    HttpSessionBindingEvent event = mock(HttpSessionBindingEvent.class);
    HttpSession session = mock(HttpSession.class);
    ServletContext context = new MockServletContext();
    when(session.getServletContext()).thenReturn(context);
    when(event.getSession()).thenReturn(session);
    sl.attributeAdded(event);
    assertEquals(1, SessionHelpers.interceptListener.get());
  }
}
