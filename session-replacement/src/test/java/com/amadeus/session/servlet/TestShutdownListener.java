package com.amadeus.session.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Before;
import org.junit.Test;

import com.amadeus.session.SessionManager;

public class TestShutdownListener {

  private ShutdownListener listener;
  private ServletContext context;
  private SessionManager manager;

  @Before
  public void setup() {
    listener = new ShutdownListener();
    context = mock(ServletContext.class);
    manager = mock(SessionManager.class);
  }

  @Test
  public void testContextDestroyed() {
    ServletContextEvent event = new ServletContextEvent(context);
    listener.contextDestroyed(event);
    verify(manager, never()).close();
  }


  @Test
  public void testContextDestroyedWithSessionManager() {
    when(context.getAttribute(Attributes.SESSION_MANAGER)).thenReturn(manager);
    ServletContextEvent event = new ServletContextEvent(context);
    listener.contextDestroyed(event);
    verify(manager).close();
  }
}
