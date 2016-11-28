package com.amadeus.session;

/**
 * Implementations of this interface provide serialization/deserialization logic
 * for objects in session.
 */
public interface SerializerDeserializer {
  /**
   * Serializes object into a byte array.
   *
   * @param value
   *          the object to serialize
   * @return byte array containing serialized object
   */
  byte[] serialize(Object value);

  /**
   * Deserializes object from byte array
   *
   * @param data
   *          serialized form of the object
   * @return deserialized object
   */
  Object deserialize(byte[] data);

  /**
   * Allows associating this instance with a {@link SessionManager}. This, for
   * example, allows using specific {@link ClassLoader} for configuring
   * {@link SerializerDeserializer}.
   *
   * @param sessionManager
   *          the associated to this instance
   */
  void setSessionManager(SessionManager sessionManager);
}
