package com.amadeus.session.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.amadeus.session.SessionConfiguration;

/**
 * This class parses web.xml and extracts session related configuration.
 */
final class WebXmlParser {
  private static final Logger logger = LoggerFactory.getLogger(WebXmlParser.class);

  // Hide default constructor
  private WebXmlParser() {
  }

  /**
   * Parse <code>web.xml</code> of the servlet context and extract session
   * information:
   *
   * <ul>
   * <li>timeout
   * <li>is the session distributable
   * <li>what kind of session propagation is used
   * <li>should cookies be http-only
   * <li>should cookies be secure
   * </ul>
   *
   * @param conf
   * @param context
   */
  static void parseWebXml(SessionConfiguration conf, ServletContext context) {
    InputStream is = context.getResourceAsStream("/WEB-INF/web.xml");
    parseStream(conf, is);
  }

  /**
   * Parses web.xml stream if one was found.
   *
   * @param conf
   * @param is
   * @param logger
   */
  static void parseStream(SessionConfiguration conf, InputStream is) {
    if (is != null) {
      try {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // We want to ignore schemas and dtd when parsing web.xml
        builder.setEntityResolver(new EntityResolver() {
          @Override
          public InputSource resolveEntity(String publicId, String systemId) {
            // Ignore entites!
            return new InputSource(new StringReader(""));
          }
        });
        Document document = builder.parse(is);
        XPath xpath = XPathFactory.newInstance().newXPath();
        lookForSessionTimeout(conf, document, xpath);
        lookForSessionConf(conf, document, xpath);
        checkDistributable(conf, document, xpath);
      } catch (SAXException | IOException | XPathExpressionException | ParserConfigurationException e) {
        throw new IllegalStateException("An exception occured while parsing web.xml. "
            + "Using default configuration: " + conf, e);
      }
    }
  }

  /**
   * Checks if application was marked as distributable. For Servlet 3.1 spec see
   * http://download.oracle.com/otndocs/jcp/servlet-3_1-fr-spec/index.html for
   * details.
   *
   * @param conf
   * @param document
   * @param xpath
   * @throws XPathExpressionException
   */
  private static void checkDistributable(SessionConfiguration conf, Document document, XPath xpath)
      throws XPathExpressionException {
    if (xpath.evaluate("/web-app/distributable", document, XPathConstants.NODE) != null) {
      conf.setDistributable(true);
    }
  }

  /**
   * Extract session timeout from web.xml. For Servlet 3.1 see
   * http://download.oracle.com/otndocs/jcp/servlet-3_1-fr-spec/index.html for
   * details.
   *
   * @param conf
   * @param document
   * @param xpath
   * @param logger
   * @throws XPathExpressionException
   */
  private static void lookForSessionTimeout(SessionConfiguration conf, Document document, XPath xpath)
      throws XPathExpressionException {
    String timeoutAsString = xpath.evaluate("/web-app/session-config/session-timeout/text()", document);
    if (isNonEmpty(timeoutAsString)) {
      conf.setMaxInactiveInterval(Integer.parseInt(timeoutAsString));
    }
  }

  /**
   * Extract session configuration from web.xml. Following arguments are
   * understood: tracking-mode, secure cookie and http-only cookie. For Servlet
   * 3.1 see
   * http://download.oracle.com/otndocs/jcp/servlet-3_1-fr-spec/index.html for
   * details.
   *
   * @param sessionConfiguration
   * @param document
   * @param xpath
   * @throws XPathExpressionException
   */
  private static void lookForSessionConf(SessionConfiguration sessionConfiguration, Document document, XPath xpath)
      throws XPathExpressionException {
    String httpOnlyString = xpath.evaluate("/web-app/session-config/cookie-config/http-only/text()", document);
    if (isNonEmpty(httpOnlyString)) {
      sessionConfiguration.setAttribute(CookieSessionTracking.COOKIE_HTTP_ONLY_PARAMETER, httpOnlyString);
    }
    String secure = xpath.evaluate("/web-app/session-config/cookie-config/secure/text()", document);
    if (isNonEmpty(secure)) {
      sessionConfiguration.setAttribute(CookieSessionTracking.SECURE_COOKIE_PARAMETER, secure);
    }
    String trackingMode = xpath.evaluate("/web-app/session-config/tracking-mode/text()", document);
    if (isNonEmpty(trackingMode)) {
      sessionConfiguration.setSessionTracking(sessionTracking(trackingMode));
    }
    String path = xpath.evaluate("/web-app/session-config/cookie-config/path/text()", document);
    if (isNonEmpty(path)) {
      sessionConfiguration.setAttribute(CookieSessionTracking.COOKIE_PATH_PARAMETER, path);
    }
  }

  /**
   * Maps web.xml tracking modes to internal tracking modes. Only COOKIE and URL
   * are supported.
   *
   * @param trackingMode
   *          tracking mode retrieved from web.xml
   * @return
   */
  private static String sessionTracking(String trackingMode) {
    if ("COOKIE".equalsIgnoreCase(trackingMode)) {
      return SessionPropagation.COOKIE.name();
    }
    if ("URL".equalsIgnoreCase(trackingMode)) {
      return SessionPropagation.URL.name();
    }
    logger.warn("Unsupported session tracking mode {}. Will be using default one.", trackingMode);
    return SessionPropagation.DEFAULT.name();
  }

  // Return true if string is not empty and not null.
  private static boolean isNonEmpty(String value) {
    return value != null && !value.isEmpty();
  }
}
