package com.amadeus.session.sample;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener {
  @Override
  public void sessionCreated(HttpSessionEvent se) {
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
  }

}
