package com.amadeus.session;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.amadeus.session.SessionConfiguration.AttributeProvider;
import com.amadeus.session.SessionConfiguration.ReplicationTrigger;

@SuppressWarnings("javadoc")
public class TestSessionConfiguration {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testSessionConfigurationDefault() {
    SessionConfiguration sc = new SessionConfiguration();
    assertTrue(sc.isDistributable());
    assertFalse(sc.isAllowedCachedSessionReuse());
    assertTrue(sc.isLoggingMdcActive());
    assertTrue(sc.isSticky());
    assertFalse(sc.isTimestampSufix());
    assertEquals(SessionConfiguration.DEFAULT_SESSION_NAMESPACE, sc.getNamespace());
    assertEquals(SessionConfiguration.DEFAULT_REPLICATION_TRIGGER, sc.getReplicationTrigger());
    assertEquals(SessionConfiguration.DEFAULT_SESSION_TIMEOUT_VALUE_NUM, sc.getMaxInactiveInterval());
    assertNull(sc.getNonCacheable());
    
    assertEquals(SessionConfiguration.DEFAULT_TRACKER_ERROR_INTERVAL_MILLISECONDS_NUM, sc.getTrackerInterval());
    assertEquals(SessionConfiguration.DEFAULT_TRACKER_ERROR_LIMITS_NUMBER, sc.getTrackerLimits());
    
  }

  @Test
  public void testTrackerInterval() {
    System.setProperty(SessionConfiguration.TRACKER_ERROR_INTERVAL_MILLISECONDS_KEY, "1");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(1, sc.getTrackerInterval());
    System.getProperties().remove(SessionConfiguration.TRACKER_ERROR_INTERVAL_MILLISECONDS_KEY);
  }

  
  @Test
  public void testTrackerLimits() {
    System.setProperty(SessionConfiguration.TRACKER_ERROR_LIMITS_NUMBER_KEY, "1");
    SessionConfiguration sc = new SessionConfiguration();
    assertEquals(1, sc.getTrackerLimits());
    System.getProperties().remove(SessionConfiguration.TRACKER_ERROR_LIMITS_NUMBER_KEY);
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
    Mockito.when(provider.getAttribute(SessionConfiguration.SESSION_ID_NAME)).thenReturn("SOMEID");
    Mockito.when(provider.getAttribute(SessionConfiguration.LOG_MDC_SESSION_NAME)).thenReturn("");
    Mockito.when(provider.getAttribute(SessionConfiguration.TRACKER_ERROR_INTERVAL_MILLISECONDS_KEY)).thenReturn("5");
    Mockito.when(provider.getAttribute(SessionConfiguration.TRACKER_ERROR_LIMITS_NUMBER_KEY)).thenReturn("50");
    
    
    assertTrue(sc.isDistributable());
    sc.initializeFrom(provider);
    assertFalse(sc.isDistributable());
    assertEquals(100, sc.getMaxInactiveInterval());
    assertThat(sc.getNonCacheable(), hasItems("A", "B"));
    assertEquals(ReplicationTrigger.SET, sc.getReplicationTrigger());
    assertEquals("SOMEID", sc.getSessionIdName());
    assertEquals(SessionConfiguration.LOGGING_MDC_DEFAULT_KEY, sc.getLoggingMdcKey());
    
    assertEquals(sc.getTrackerInterval(), 5);
    assertEquals(sc.getTrackerLimits(), 50);    
    
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

  @Test
  public void testEncryptionKey() {
    SessionConfiguration sc = new SessionConfiguration();
    assertFalse(sc.isUsingEncryption());
    assertNull(sc.getEncryptionKey());
    sc.setEncryptionKey("test");
    assertTrue(sc.isUsingEncryption());
    assertEquals("test", sc.getEncryptionKey());
    sc.setUsingEncryption(false);
    assertFalse(sc.isUsingEncryption());
    assertNull(sc.getEncryptionKey());
    sc.setEncryptionKey(getClass().getResource("test.key").toString());
    assertEquals("key-contents", sc.getEncryptionKey());
  }

  @Test
  public void testEncryptionKeyFromProvider() {
    SessionConfiguration sc = new SessionConfiguration();
    AttributeProvider provider = Mockito.mock(AttributeProvider.class);
    Mockito.when(provider.getAttribute(SessionConfiguration.SESSION_ENCRYPTION_KEY)).thenReturn("a-key");
    sc.initializeFrom(provider);
    assertTrue(sc.isUsingEncryption());
    assertEquals("a-key", sc.getEncryptionKey());
  }

  @Test
  public void testEncryptionKeyBadProtocol() {
    SessionConfiguration sc = new SessionConfiguration();
    assertFalse(sc.isUsingEncryption());
    sc.setEncryptionKey("ftp://example.com");
    exception.expectMessage("Unknown protocol");
    sc.getEncryptionKey();
  }

  @Test
  public void testEncryptionKeyBadRetrieval() {
    SessionConfiguration sc = new SessionConfiguration();
    assertFalse(sc.isUsingEncryption());
    sc.setEncryptionKey(getClass().getResource("test.key").toString() + ".dummy");
    exception.expectCause(isA(IOException.class));
    sc.getEncryptionKey();
  }

  @Test
  public void testEncryptionKeyEmpty() {
    SessionConfiguration sc = new SessionConfiguration();
    assertFalse(sc.isUsingEncryption());
    sc.setEncryptionKey(getClass().getResource("empty.key").toString());
    exception.expectMessage("Destination was empty.");
    sc.getEncryptionKey();
  }
}
