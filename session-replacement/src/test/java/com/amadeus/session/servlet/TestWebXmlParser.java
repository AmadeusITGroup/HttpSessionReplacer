package com.amadeus.session.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp25.WebAppDescriptor;
import org.junit.Test;

import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
public class TestWebXmlParser {
  static final String invalidDescriptor = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" ";

  static final String externalEntity = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" "
      +   "\"http://java.sun.com/dtd/web-app_2_3.dtd\">"
      + "<web-app></web-app>";

  static final String withInvalidSessionDuration = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">"
      + "<session-config>" + "<session-timeout>A</session-timeout>" + "</session-config>" + "</web-app>";

  static final String withEmptySessionDuration = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">"
      + "<session-config>" + "<session-timeout>A</session-timeout>" + "</session-config>" + "</web-app>";

  static final String withInvalidTrackingMode = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">"
      + "<session-config><tracking-mode>BAD</tracking-mode></session-config>"
      + "</web-app>";
  static final String withUrlTrackingMode = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">"
      + "<session-config><tracking-mode>URL</tracking-mode></session-config>"
      + "</web-app>";
  static final String withSessionConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">"
      + "<session-config><cookie-config><secure>false</secure><http-only>true</http-only><path>/test</path></cookie-config>"
      + "<tracking-mode>COOKIE</tracking-mode></session-config>"
      + "</web-app>";

  @Test
  public void testExternalEntity() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(externalEntity.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      sessionConfiguration.setDistributable(false);
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals(1800, sessionConfiguration.getMaxInactiveInterval());
      assertFalse(sessionConfiguration.isDistributable());
    }
  }

  @Test
  public void testDistributable() throws IOException {
    String webXml = Descriptors.create(WebAppDescriptor.class).version("3.0").distributable().exportAsString();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(webXml.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      sessionConfiguration.setDistributable(false);
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals(1800, sessionConfiguration.getMaxInactiveInterval());
      assertTrue(sessionConfiguration.isDistributable());
    }
  }

  @Test
  public void testNotDistributable() throws IOException {
    String webXml = Descriptors.create(WebAppDescriptor.class).version("3.0").exportAsString();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(webXml.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      sessionConfiguration.setDistributable(true);
      WebXmlParser.parseStream(sessionConfiguration, bais);
    }
  }

  @Test
  public void testNoDescriptor() throws IOException {
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.setDistributable(true);
    WebXmlParser.parseStream(sessionConfiguration, null);
  }

  @Test
  public void testParseStreamDefaultSession0() throws IOException {
    String webXml = Descriptors.create(WebAppDescriptor.class).version("3.0").createSessionConfig().sessionTimeout(0)
        .up().exportAsString();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(webXml.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals(0, sessionConfiguration.getMaxInactiveInterval());
    }
  }

  @Test
  public void testParseStreamDefaultSession1000() throws IOException {
    String webXml = Descriptors.create(WebAppDescriptor.class).version("3.0").createSessionConfig().sessionTimeout(1000)
        .up().exportAsString();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(webXml.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals(1000, sessionConfiguration.getMaxInactiveInterval());
    }
  }

  @Test
  public void testParseStreamDefaultSessionEmpty() throws IOException {
    String webXml = Descriptors.create(WebAppDescriptor.class).version("3.0").exportAsString();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(webXml.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals(1800, sessionConfiguration.getMaxInactiveInterval());
    }
  }

  @Test(expected = NumberFormatException.class)
  public void testParseStreamDefaultSessionInvalid() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withInvalidSessionDuration.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
    }
  }

  @Test(expected = NumberFormatException.class)
  public void testParseStreamDefaultSessionAbsent() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withEmptySessionDuration.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testInvalidDescriptor() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(invalidDescriptor.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
    }
  }

  @Test
  public void testInvalidTrackingMode() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withInvalidTrackingMode.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals("DEFAULT", sessionConfiguration.getSessionTracking());
    }
  }

  @Test
  public void testWithSessionConfig() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withSessionConfig.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals("COOKIE", sessionConfiguration.getSessionTracking());
      assertEquals("true", sessionConfiguration.getAttribute(CookieSessionTracking.COOKIE_HTTP_ONLY_PARAMETER, null));
      assertEquals("false", sessionConfiguration.getAttribute(CookieSessionTracking.SECURE_COOKIE_PARAMETER, null));
      assertEquals("/test", sessionConfiguration.getAttribute(CookieSessionTracking.COOKIE_CONTEXT_PATH_PARAMETER, null));
    }
  }

  @Test
  public void testWithUrlTracking() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withUrlTrackingMode.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      WebXmlParser.parseStream(sessionConfiguration, bais);
      assertEquals("URL", sessionConfiguration.getSessionTracking());
    }
  }

  @Test
  public void testParseXml() throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(withUrlTrackingMode.getBytes("UTF-8"))) {
      SessionConfiguration sessionConfiguration = new SessionConfiguration();
      ServletContext context = mock(ServletContext.class);
      when(context.getResourceAsStream("/WEB-INF/web.xml")).thenReturn(bais);
      WebXmlParser.parseWebXml(sessionConfiguration, context);
      assertEquals("URL", sessionConfiguration.getSessionTracking());
    }
  }
}
