package com.amadeus.session;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.amadeus.session.SessionConfiguration.AttributeProvider;
import com.amadeus.session.SessionConfiguration.ReplicationTrigger;

@SuppressWarnings("javadoc")
public class TestSessionConfiguration {

  @Test
  public void testSessionConfigurationDefault() {
    SessionConfiguration sc = new SessionConfiguration();
    assertTrue(sc.isDistributable());
    assertFalse(sc.isAllowedCachedSessionReuse());
    assertTrue(sc.isLoggingMdcActive());
    assertTrue(sc.isSticky());
    assertEquals(SessionConfiguration.DEFAULT_SESSION_NAMESPACE, sc.getNamespace());
    assertEquals(SessionConfiguration.DEFAULT_REPLICATION_TRIGGER, sc.getReplicationTrigger());
    assertEquals(SessionConfiguration.DEFAULT_SESSION_TIMEOUT_VALUE_NUM, sc.getMaxInactiveInterval());
    assertNull(sc.getNonCacheable());
  }

  @Test
  public void testNonCacheable() {
    System.setProperty(SessionConfiguration.NON_CACHEABLE_ATTRIBUTES, "a,b,c");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(3, sc.getNonCacheable().size());
    assertTrue(sc.getNonCacheable().contains("a"));
    assertTrue(sc.getNonCacheable().contains("b"));
    assertTrue(sc.getNonCacheable().contains("c"));
    assertFalse(sc.getNonCacheable().contains("a,b,c"));
    System.getProperties().remove(SessionConfiguration.NON_CACHEABLE_ATTRIBUTES);
  }

  @Test
  public void testInvalidSessionTimeout() {
    System.setProperty(SessionConfiguration.DEFAULT_SESSION_TIMEOUT, "ABC");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(SessionConfiguration.DEFAULT_SESSION_TIMEOUT_VALUE_NUM, sc.getMaxInactiveInterval());
    System.getProperties().remove(SessionConfiguration.DEFAULT_SESSION_TIMEOUT);
  }

  @Test
  public void testSessionTimeout() {
    System.setProperty(SessionConfiguration.DEFAULT_SESSION_TIMEOUT, "100");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(100, sc.getMaxInactiveInterval());
    System.getProperties().remove(SessionConfiguration.DEFAULT_SESSION_TIMEOUT);
  }

  @Test
  public void testInvalidReplicationTriggerIgnored() {
    System.setProperty(SessionConfiguration.SESSION_REPLICATION_TRIGGER, "SET_AND_GET");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(SessionConfiguration.DEFAULT_REPLICATION_TRIGGER, sc.getReplicationTrigger());
    System.getProperties().remove(SessionConfiguration.SESSION_REPLICATION_TRIGGER);
  }

  @Test
  public void testReplicationTrigger() {
    System.setProperty(SessionConfiguration.SESSION_REPLICATION_TRIGGER, "SET");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(ReplicationTrigger.SET, sc.getReplicationTrigger());
    System.getProperties().remove(SessionConfiguration.SESSION_REPLICATION_TRIGGER);
  }

  @Test
  public void testInitializeFromCalls() {
    SessionConfiguration sc = new SessionConfiguration();
    AttributeProvider provider = Mockito.mock(AttributeProvider.class);
    sc.initializeFrom(provider);
    Mockito.verify(provider).getAttribute(SessionConfiguration.DISTRIBUTABLE_SESSION);
  }

  @Test
  public void testInitializeFromCheckValues() {
    SessionConfiguration sc = new SessionConfiguration();
    AttributeProvider provider = Mockito.mock(AttributeProvider.class);
    Mockito.when(provider.getAttribute(SessionConfiguration.DISTRIBUTABLE_SESSION)).thenReturn("false");
    Mockito.when(provider.getAttribute(SessionConfiguration.DEFAULT_SESSION_TIMEOUT)).thenReturn("100");
    Mockito.when(provider.getAttribute(SessionConfiguration.NON_CACHEABLE_ATTRIBUTES)).thenReturn("A,B");
    Mockito.when(provider.getAttribute(SessionConfiguration.SESSION_REPLICATION_TRIGGER)).thenReturn("SET");
    assertTrue(sc.isDistributable());
    sc.initializeFrom(provider);
    assertFalse(sc.isDistributable());
    assertEquals(100, sc.getMaxInactiveInterval());
    assertThat(sc.getNonCacheable(), hasItems("A", "B"));
    assertEquals(ReplicationTrigger.SET, sc.getReplicationTrigger());
  }

  @Test
  public void testInitNode() {
    String node = SessionConfiguration.initNode();
    assertNotNull(node);
    System.setProperty(SessionConfiguration.NODE_ID, "test");
    node = SessionConfiguration.initNode();
    assertEquals("test", node);
    System.getProperties().remove(SessionConfiguration.NODE_ID);
    String os = System.getProperty("os.name");
    node = SessionConfiguration.initNode();
    if (os.startsWith("Windows")) {
      if (System.getenv("COMPUTERNAME") != null) {
        assertEquals(System.getenv("COMPUTERNAME"), node);
      }
    } else {
      if (System.getenv("HOSTNAME") != null) {
        assertEquals(System.getenv("HOSTNAME"), node);
      }
    }
  }

}
