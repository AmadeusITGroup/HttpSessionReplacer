package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TestSessionData {

  int hash(String sessionId, long creationTime, int maxInactiveInterval) {
    return new SessionData(sessionId, creationTime, maxInactiveInterval).hashCode();
  }

  @Test
  public void testHashCode() {
    assertEquals(hash("1", 100L, 200), hash("1", 100L, 300));
    assertNotEquals(hash("1", 100L, 200), hash("2", 100L, 300));
    assertNotEquals(hash("1", 100L, 200), hash("1", 200L, 300));
  }

  @Test
  public void testEqualsObject() {
    assertEquals(new SessionData("1", 100L, 200), new SessionData("1", 100L, 200));
    assertEquals(new SessionData("1", 100L, 200), new SessionData("1", 100L, 300));
    assertNotEquals(new SessionData("1", 100L, 200), null);
    assertNotEquals(new SessionData("1", 100L, 200), "23");
    assertNotEquals(new SessionData("1", 100L, 200), new SessionData(null, 100L, 300));
    assertEquals(new SessionData(null, 100L, 200), new SessionData(null, 100L, 200));
    assertNotEquals(new SessionData(null, 100L, 200), new SessionData("1", 100L, 200));
    assertNotEquals(new SessionData("1", 100L, 200), new SessionData("1", 200L, 300));
  }

}
