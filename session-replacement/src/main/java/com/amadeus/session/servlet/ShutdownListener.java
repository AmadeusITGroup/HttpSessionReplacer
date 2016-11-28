package com.amadeus.session.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.amadeus.session.SessionManager;

/**
 * This listener is responsible for triggering session manager shutdown.
 */
public class ShutdownListener implements ServletContextListener {
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    // If we have session manager we need to close it
    SessionManager sessionManager = (SessionManager)event.getServletContext().getAttribute(Attributes.SESSION_MANAGER);
    if (sessionManager != null) {
      sessionManager.close();
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    // Do nothing here
  }

}
