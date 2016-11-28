package com.amadeus.session.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.instrument.Instrumentation;

import org.junit.Before;
import org.junit.Test;

public class TestSessionAgent {
  @Before
  public void cleanAgent() {
    SessionAgent.debugMode = false;
    SessionAgent.agentActive = false;
    System.getProperties().remove(SessionAgent.SESSION_FACTORY);
    System.getProperties().remove(SessionAgent.REPOSITORY_CONF_PROPERTY);
    System.getProperties().remove(SessionAgent.SESSION_TIMEOUT);
    System.getProperties().remove(SessionAgent.SESSION_MANAGEMENT_DISABLED);
    System.getProperties().remove(SessionAgent.SESSION_DISTRIBUTABLE);
  }
  @Test
  public void testDebugModeActive() {
    SessionAgent.readArguments("log=debug");
    assertTrue(SessionAgent.isDebugMode());
  }
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidDebugMode() {
    SessionAgent.readArguments("log=debg");
  }
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidArgument() {
    SessionAgent.readArguments("log=debug,provider=d,something=bad");
  }
  @Test
  public void testProviderDebug() {
    SessionAgent.readArguments("provider=d,log=debug");
    assertEquals("d", System.getProperty(SessionAgent.SESSION_FACTORY));
    assertTrue(SessionAgent.isDebugMode());
  }
  @Test
  public void testProviderDebugTimeout() {
    SessionAgent.readArguments("provider=d,log=debug,timeout=10");
    assertTrue(SessionAgent.isDebugMode());
    assertEquals("provider=d,log=debug,timeout=10", System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertEquals("d", System.getProperty(SessionAgent.SESSION_FACTORY));
    assertEquals("10", System.getProperty(SessionAgent.SESSION_TIMEOUT));
  }
  @Test
  public void testInterceptListeners() {
    SessionAgent.readArguments("interceptListeners=true");
    assertFalse(SessionAgent.isDebugMode());
    assertEquals("true", System.getProperty(SessionAgent.INTERCEPT_LISTENER_PROPERTY));
    assertNull(System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
  }
  @Test
  public void testReadTimeout() {
    SessionAgent.readArguments("timeout=100");
    assertFalse(SessionAgent.isDebugMode());
    assertEquals("100", System.getProperty(SessionAgent.SESSION_TIMEOUT));
    assertEquals("timeout=100", System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertNull(System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
  }
  @Test
  public void testReadTimeoutAndFactory() {
    SessionAgent.readArguments("timeout=200,provider=redis");
    assertEquals("200", System.getProperty(SessionAgent.SESSION_TIMEOUT));
    assertEquals("timeout=200,provider=redis", System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertEquals("redis", System.getProperty(SessionAgent.SESSION_FACTORY));
    assertNull(System.getProperty(SessionAgent.SESSION_DISTRIBUTABLE));
    assertNull(System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
  }
  @Test
  public void testReadDistributableAndFactory() {
    SessionAgent.readArguments("provider=redis,distributable=true");
    assertFalse(SessionAgent.isDebugMode());
    assertEquals("true", System.getProperty(SessionAgent.SESSION_DISTRIBUTABLE));
    assertNull(System.getProperty(SessionAgent.SESSION_TIMEOUT));
    assertEquals("provider=redis,distributable=true", System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertEquals("redis", System.getProperty(SessionAgent.SESSION_FACTORY));
    assertNull(System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
  }
  @Test
  public void testEmptyArguments() {
    SessionAgent.readArguments("");
    assertFalse(SessionAgent.isDebugMode());
    assertNull(System.getProperty(SessionAgent.SESSION_TIMEOUT));
    assertNull(System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertNull(System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
  }
  @Test
  public void testReadDisabledFactoryAndDebug() {
    SessionAgent.readArguments("disabled=true,log=debug");
    assertTrue(SessionAgent.isDebugMode());
    assertEquals("true", System.getProperty(SessionAgent.SESSION_MANAGEMENT_DISABLED));
    assertEquals("disabled=true,log=debug", System.getProperty(SessionAgent.REPOSITORY_CONF_PROPERTY));
    assertNull(System.getProperty(SessionAgent.SESSION_TIMEOUT));
    assertNull(System.getProperty(SessionAgent.SESSION_DISTRIBUTABLE));
    assertNull(System.getProperty(SessionAgent.SESSION_FACTORY));
  }

  @Test
  public void testAgentActive() {
    Instrumentation inst = mock(Instrumentation.class);
    SessionAgent.premain("", inst);
    assertTrue(SessionAgent.isAgentActive());
    verify(inst).addTransformer(any(SessionSupportTransformer.class));
  }
  @Test
  public void testAgentInterceptListneres() {
    Instrumentation inst = mock(Instrumentation.class);
    SessionAgent.premain("interceptListeners=true", inst);
    assertTrue(SessionAgent.isAgentActive());
    verify(inst).addTransformer(any(SessionSupportTransformer.class));
  }
  @Test
  public void testAgentInactive() {
    Instrumentation inst = mock(Instrumentation.class);
    SessionAgent.premain("disabled=true", inst);
    assertTrue(SessionAgent.isAgentActive());
    verify(inst, never()).addTransformer(any(SessionSupportTransformer.class));
  }
  @Test(expected=IllegalStateException.class)
  public void testMultipleInvocations() {
    Instrumentation inst = mock(Instrumentation.class);
    SessionAgent.premain("", inst);
    assertTrue(SessionAgent.isAgentActive());
    SessionAgent.premain("", inst);
  }
}
