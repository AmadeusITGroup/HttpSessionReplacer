package com.amadeus.session.agent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.junit.Test;

public class TestServletContextAndFilterTransformer {

  @Test
  public void testTransform() throws IllegalClassFormatException, IOException {
    SessionSupportTransformer sct = new SessionSupportTransformer(false);
    Class<MockClass> clazz = MockClass.class;
    InputStream is = clazz.getClassLoader().getResourceAsStream("com/amadeus/session/agent/MockClass.class");
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    byte[] classfileBuffer = readFully(is);
    byte[] result = sct.transform(clazz.getClassLoader(),
        clazz.getName(), clazz, protectionDomain, classfileBuffer);
    assertNull(result);
  }

  @Test
  public void testTransformFilter() throws IllegalClassFormatException, IOException {
    SessionSupportTransformer sct = new SessionSupportTransformer(false);
    Class<MockFilter> clazz = MockFilter.class;
    InputStream is = clazz.getClassLoader().getResourceAsStream("com/amadeus/session/agent/MockFilter.class");
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    byte[] classfileBuffer = readFully(is);
    byte[] result = sct.transform(clazz.getClassLoader(),
        clazz.getName(), clazz, protectionDomain, classfileBuffer);
    assertNotNull(result);
  }

  @Test
  public void testTransformServletContext() throws IllegalClassFormatException, IOException {
    SessionSupportTransformer sct = new SessionSupportTransformer(false);
    Class<MockServletContext> clazz = MockServletContext.class;
    InputStream is = clazz.getClassLoader().getResourceAsStream("com/amadeus/session/agent/MockServletContext.class");
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    byte[] classfileBuffer = readFully(is);
    byte[] result = sct.transform(clazz.getClassLoader(),
        clazz.getName(), clazz, protectionDomain, classfileBuffer);
    assertNotNull(result);
  }

  @Test
  public void testTransformInheritedFilter() throws IllegalClassFormatException, IOException {
    SessionSupportTransformer sct = new SessionSupportTransformer(false);
    sct.filterClasses.add(MockFilter.class.getName().replace('.', '/'));
    Class<MockSubFilter> clazz = MockSubFilter.class;
    InputStream is = clazz.getClassLoader().getResourceAsStream("com/amadeus/session/agent/MockSubFilter.class");
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    byte[] classfileBuffer = readFully(is);
    byte[] result = sct.transform(clazz.getClassLoader(),
        clazz.getName(), clazz, protectionDomain, classfileBuffer);
    assertNotNull(result);
  }

  @Test
  public void testTransformInheritedServletContext() throws IllegalClassFormatException, IOException {
    SessionSupportTransformer sct = new SessionSupportTransformer(false);
    sct.filterClasses.add(MockServletContext.class.getName().replace('.', '/'));
    Class<MockSubServletContext> clazz = MockSubServletContext.class;
    InputStream is = clazz.getClassLoader().getResourceAsStream("com/amadeus/session/agent/MockSubServletContext.class");
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    byte[] classfileBuffer = readFully(is);
    byte[] result = sct.transform(clazz.getClassLoader(),
        clazz.getName(), clazz, protectionDomain, classfileBuffer);
    assertNotNull(result);
  }

  public static byte[] readFully(InputStream input) throws IOException
  {
      byte[] buffer = new byte[8192];
      int bytesRead;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      while ((bytesRead = input.read(buffer)) != -1)
      {
          output.write(buffer, 0, bytesRead);
      }
      return output.toByteArray();
  }}
