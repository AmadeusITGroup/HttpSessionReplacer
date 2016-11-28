package com.amadeus.session.servlet2x;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@SuppressWarnings("javadoc")
public class CallCountingSessionListener implements HttpSessionListener {
  public static AtomicInteger numberOfTimesCreateCalled = new AtomicInteger();
  public static AtomicInteger numberOfTimesDestroyedCalled = new AtomicInteger();

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    numberOfTimesCreateCalled.incrementAndGet();
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    numberOfTimesDestroyedCalled.incrementAndGet();
  }
}
