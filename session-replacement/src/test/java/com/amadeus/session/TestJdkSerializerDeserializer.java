package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

public class TestJdkSerializerDeserializer {

  private JdkSerializerDeserializer serializer;
  private SessionManager sessionManager;
  private byte[] serializedString;
  private Date now;
  private byte[] serializedDate;

  @Before
  public void setUp() {
    serializer = new JdkSerializerDeserializer();
    sessionManager = mock(SessionManager.class);
    when(sessionManager.getMetrics()).thenReturn(new MetricRegistry());
    serializer.setSessionManager(sessionManager);
    JdkSerializerDeserializer tempSerializer = new JdkSerializerDeserializer();
    SessionManager sm = mock(SessionManager.class);
    when(sm.getMetrics()).thenReturn(new MetricRegistry());
    tempSerializer.setSessionManager(sm);
    now = new Date();
    serializedString = serializer.serialize("String");
    serializedDate = serializer.serialize(now);
  }

  @Test
  public void testDeserialize() {
    assertEquals(null, serializer.deserialize(null));
    assertEquals("String", serializer.deserialize(serializedString));
  }

  @Test(expected=IllegalStateException.class)
  public void testDeserializeWithClassNotFoundException() throws ClassNotFoundException {
    ClassLoader cl = mock(ClassLoader.class);
    when(sessionManager.getSessionClassLoader()).thenReturn(cl);
    assertEquals(now, serializer.deserialize(serializedDate));
  }

}
