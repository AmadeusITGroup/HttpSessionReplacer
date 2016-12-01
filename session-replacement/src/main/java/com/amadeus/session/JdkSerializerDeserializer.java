package com.amadeus.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

/**
 * Default serialization/deserialization logic uses JDK serialization to convert
 * objects to/from byte arrays.
 * <p>
 * The implementation will also measure the number of bytes that were serialized
 * or deserialized.
 * </p>
 */
public class JdkSerializerDeserializer implements SerializerDeserializer {
  private SessionManager sessionManager;
  private Counter serializedData;
  private Counter deserializedData;
  private Histogram serializedHistogram;
  private Histogram deserializedHistogram;

  @Override
  public byte[] serialize(Object value) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
      out.writeObject(value);
      byte[] ba = bos.toByteArray();
      // Incrementing metrics
      serializedData.inc(ba.length);
      serializedHistogram.update(ba.length);
      return ba;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize object. See stacktrace for more information.", e);
    }
  }

  @Override
  public Object deserialize(byte[] data) {
    if (data == null) {
      return null;
    }
    // For deserializing objects we use specific class loader of
    // the session manager to insure it was the same one used
    // when creating serialized objects.
    ClassLoader classLoader = sessionManager.getSessionClassLoader();
    if (classLoader == null) {
      classLoader = Thread.currentThread().getContextClassLoader();
    }
    try (ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ClassLoaderObjectInputStream(classLoader, in)) {
      Object obj = is.readObject();
      // Incrementing metrics
      deserializedData.inc(data.length);
      deserializedHistogram.update(data.length);
      return obj;
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Unable to deserialize object. See stacktrace for more information.", e);
    }
  }

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    MetricRegistry metrics = sessionManager.getMetrics();
    serializedData = metrics.counter("com.amadeus.session.serialized.bytes");
    deserializedData = metrics.counter("com.amadeus.session.deserialized.bytes");
    serializedHistogram = metrics.histogram("com.amadeus.session.serialized.distribution");
    deserializedHistogram = metrics.histogram("com.amadeus.session.deserialized.distribution");
  }
}
