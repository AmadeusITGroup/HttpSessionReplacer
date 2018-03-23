package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@SuppressWarnings("javadoc")
public class ArqITSessionWithSecureCookie extends AbstractITSession {

  private static final String SECURE_COOKIE_WEBAPP = "secure-cookie";

  private static final String NON_SECURE_COOKIE_WEBAPP = "non-secure-cookie";
  
  private static final String SECURE_COOKIE_ON_SECURED_REQUEST_WEBAPP = "secure-cookie-on-secured-request";

  @Deployment(testable = false, name = SECURE_COOKIE_WEBAPP)
  public static WebArchive startSecure() {
    return Helpers.createWebApp("secure-cookie.war")
        .addClasses(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName("com.amadeus.session.cookie.secure").paramValue("true").up()
            .exportAsString()));
  }

  @Deployment(testable = false, name = NON_SECURE_COOKIE_WEBAPP)
  public static WebArchive startNonSecured() {
    return Helpers.createWebApp("non-secure-cookie.war")
        .addClasses(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName("com.amadeus.session.cookie.secure").paramValue("false").up()
            .exportAsString()));
  }

  @Deployment(testable = false, name = SECURE_COOKIE_ON_SECURED_REQUEST_WEBAPP)
  public static WebArchive startSecureOnSecuredRequestOnly() {
    return Helpers.createWebApp("secure-cookie-on-secured-request.war")
        .addClasses(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName("com.amadeus.session.cookie.secure").paramValue("true").up()
            .getOrCreateContextParam().paramName("com.amadeus.session.cookie.secure.on.secured.request").paramValue("true").up()
            .exportAsString()));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment(SECURE_COOKIE_WEBAPP)
  public void testCookieSecured(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagated");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = toLowerCase(connection.getHeaderFields().get("Set-Cookie"));
    assertThat(cookies, hasItem(containsString("secure")));
  }

  private List<String> toLowerCase(List<String> list) {
    ArrayList<String> result = new ArrayList<>(list.size());
    for (String s : list) {
      result.add(s.toLowerCase());
    }
    return result;
  }

  @RunAsClient
  @Test
  @OperateOnDeployment(NON_SECURE_COOKIE_WEBAPP)
  public void testCookieNotSecured(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagated");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = toLowerCase(connection.getHeaderFields().get("Set-Cookie"));
    assertThat(cookies, not(hasItem(containsString(("secure")))));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment(SECURE_COOKIE_ON_SECURED_REQUEST_WEBAPP)
  public void testCookieSecureOnSecuredRequest(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagated");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = toLowerCase(connection.getHeaderFields().get("Set-Cookie"));
    assertThat(cookies, not(hasItem(containsString("secure"))));
  }
}
