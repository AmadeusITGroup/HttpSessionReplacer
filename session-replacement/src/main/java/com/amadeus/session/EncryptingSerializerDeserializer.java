package com.amadeus.session;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts and decrypts session data before storing it in session repository.
 * Session is encrypted using AES/CBC/PKCS5Padding transformation. The
 * implementation delegates serializing and deserializing to a wrapped
 * {@link SerializerDeserializer}.
 * <p>
 * The key must be provided either by calling {@link #initKey(String)} or via
 * configuration property.
 */
public class EncryptingSerializerDeserializer implements SerializerDeserializer {
  private final SerializerDeserializer wrapped;
  private SecretKeySpec secretKey;
  private SecureRandom random;

  /**
   * Default constructor wraps {@link JdkSerializerDeserializer} instance.
   */
  public EncryptingSerializerDeserializer() {
    this(new JdkSerializerDeserializer());
  }

  /**
   * Constructor that allows wrapping arbitrary {@link SerializerDeserializer}
   * instance.
   *
   * @param wrapped
   *          the instance to wrap
   */
  public EncryptingSerializerDeserializer(SerializerDeserializer wrapped) {
    this.wrapped = wrapped;
    random = new SecureRandom();
  }

  /**
   * Sets up encryption key to use.
   *
   * @param key
   *          encryption key to use
   */
  void initKey(String key) {
    try {
      byte[] keyArray = key.getBytes("UTF-8");
      MessageDigest sha = MessageDigest.getInstance("SHA-1");
      keyArray = sha.digest(keyArray);
      keyArray = Arrays.copyOf(keyArray, 16);
      secretKey = new SecretKeySpec(keyArray, "AES");
    } catch (Exception e) { // NOSONAR
      throw new IllegalStateException(e);
    }
  }

  @Override
  public byte[] serialize(Object value) {
    byte[] arrayToEncrypt = wrapped.serialize(value);
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      byte[] iv = new byte[16];
      random.nextBytes(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
      ByteBuffer output = ByteBuffer.allocate(iv.length + cipher.getOutputSize(arrayToEncrypt.length));
      output.put(iv);
      cipher.doFinal(ByteBuffer.wrap(arrayToEncrypt), output);
      return output.array();
    } catch (Exception e) { // NOSONAR
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Object deserialize(byte[] data) {
    byte[] decrypted;
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      byte[] iv = new byte[16];
      System.arraycopy(data, 0, iv, 0, iv.length);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
      decrypted = cipher.doFinal(data, iv.length, data.length-iv.length);
    } catch (Exception e) { // NOSONAR
      throw new IllegalStateException(e);
    }
    return wrapped.deserialize(decrypted);
  }

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    wrapped.setSessionManager(sessionManager);
    initKey(sessionManager.getConfiguration().getEncryptionKey());
  }
}
