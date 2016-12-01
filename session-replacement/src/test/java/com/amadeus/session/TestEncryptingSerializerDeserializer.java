package com.amadeus.session;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.Date;

import javax.crypto.IllegalBlockSizeException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.codahale.metrics.MetricRegistry;

public class TestEncryptingSerializerDeserializer {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testDeserialize() {
    EncryptingSerializerDeserializer serializer = new EncryptingSerializerDeserializer();
    SessionManager sessionManager = mock(SessionManager.class);
    SessionConfiguration configuration = new SessionConfiguration();
    configuration.setUsingEncryption(true);
    configuration.setEncryptionKey("test");
    when(sessionManager.getMetrics()).thenReturn(new MetricRegistry());
    when(sessionManager.getConfiguration()).thenReturn(configuration);
    serializer.setSessionManager(sessionManager);
    EncryptingSerializerDeserializer tempSerializer = new EncryptingSerializerDeserializer();
    SessionManager sm = mock(SessionManager.class);
    when(sm.getMetrics()).thenReturn(new MetricRegistry());
    when(sm.getConfiguration()).thenReturn(configuration);
    tempSerializer.setSessionManager(sm);
    Date now = new Date();
    byte[] serializedString = serializer.serialize("String");
    byte[] serializedDate = serializer.serialize(now);
    byte[] serializedNull = serializer.serialize(null);

    assertEquals(null, serializer.deserialize(serializedNull));
    assertEquals("String", serializer.deserialize(serializedString));
    assertEquals(now, serializer.deserialize(serializedDate));
  }

  @Test
  public void testBadKey() {
    EncryptingSerializerDeserializer serializer = new EncryptingSerializerDeserializer();
    exception.expectCause(isA(NullPointerException.class));
    serializer.initKey(null);
  }

  @Test
  public void testSerializerException() {
    EncryptingSerializerDeserializer serializer = new EncryptingSerializerDeserializer(
        mock(JdkSerializerDeserializer.class));
    serializer.initKey("test");
    exception.expectCause(isA(NullPointerException.class));
    serializer.serialize("String");
  }

  @Test
  public void testDecryptBadData() {
    EncryptingSerializerDeserializer serializer = new EncryptingSerializerDeserializer();
    SessionManager sessionManager = mock(SessionManager.class);
    SessionConfiguration configuration = new SessionConfiguration();
    configuration.setUsingEncryption(true);
    configuration.setEncryptionKey("test");
    when(sessionManager.getMetrics()).thenReturn(new MetricRegistry());
    when(sessionManager.getConfiguration()).thenReturn(configuration);
    serializer.setSessionManager(sessionManager);
    byte[] data = new byte[20];
    new SecureRandom().nextBytes(data);
    exception.expectCause(isA(IllegalBlockSizeException.class));
    serializer.deserialize(data);
  }
}
