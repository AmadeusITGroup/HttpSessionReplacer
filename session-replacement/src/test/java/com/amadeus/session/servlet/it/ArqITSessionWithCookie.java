package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.matchLines;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@SuppressWarnings("javadoc")
public class ArqITSessionWithCookie extends AbstractITSession {

  @Deployment(testable = false)
  public static WebArchive startFirst() {
    return Helpers.createWebApp("session-cookie.war")
        .addClasses(SampleServlet.class, OtherServlet.class, SwitchServlet.class);
  }

  @RunAsClient
  @Test
  public void testSessionCreated(@ArquillianResource URL baseURL) throws IOException {
    URL url = url(baseURL, "TestServlet", "testSessionCreated");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: B"));
    url = new URL(baseURL, "OtherServlet");
    connection = (HttpURLConnection) url.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: C"));
  }

  @RunAsClient
  @Test
  public void testSessionPropagated(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagated");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B"));
    assertThat(cookies, hasItem(originalCookie));
  }

  @RunAsClient
  @Test
  public void testSessionPropagatedChangeServlets(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagatedChangeServlets");
    String originalCookie = callWebapp(urlTest);
    URL urlOther = url(baseURL, "OtherServlet", "testSessionPropagatedChangeServlets");
    HttpURLConnection connection = (HttpURLConnection) urlOther.openConnection();
    setSessionCookie(connection, originalCookie);
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B", "New value of attribute: C"));
    assertThat(cookies, hasItem(originalCookie));
    connection = (HttpURLConnection) urlOther.openConnection();
    setSessionCookie(connection, cookies);
    cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: C", "New value of attribute: C"));
    connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, cookies);
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: C", "New value of attribute: B"));
  }

  @RunAsClient
  @Test
  public void testSessionNotPropagated(@ArquillianResource URL baseURL) throws IOException {
    final URL url = url(baseURL, "TestServlet", "testSessionNotPropagated");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    String originalCookie = cookies.get(0);
    connection = (HttpURLConnection) url.openConnection();
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");
    assertFalse(originalCookie.equals(cookies.get(0)));
  }

  private static final String BAD_COOKIE = "JSESSIONID=80d47147-0103-48f7-9ce4-999999999999; path=/; HttpOnly";

  @RunAsClient
  @Test
  public void testBadCookieSession(@ArquillianResource URL baseURL) throws IOException {
    final URL url = url(baseURL, "TestServlet", "testBadCookieSession");
    sendBadCookie(url, BAD_COOKIE.split(";", 2)[0]);
    sendBadCookie(url, "JSESSIONID=123");
    sendBadCookie(url, "JSESSIONID=null");
  }

  private void sendBadCookie(final URL url, String badCookie) throws IOException {
    List<String> cookies;
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.addRequestProperty("Cookie", badCookie);
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");
    assertNotNull(cookies);
    assertEquals(1, cookies.size());
    assertTrue(cookies.get(0).startsWith("JSESSIONID"));
    assertFalse(cookies.get(0).startsWith(badCookie));
  }

  @RunAsClient
  @Test
  public void testSessionSwitched(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionSwitched");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B"));
    assertThat(cookies, hasItem(originalCookie));

    URL urlOther = url(baseURL, "SwitchServlet", "testSessionSwitched");
    HttpURLConnection connectionOther = (HttpURLConnection) urlOther.openConnection();
    setSessionCookie(connectionOther, originalCookie);
    connectionOther.connect();
    List<String> switchedCookies = connectionOther.getHeaderFields().get("Set-Cookie");

    assertThat(connectionOther, matchLines("Previous value of attribute: B", "New value of attribute: S"));
    assertThat(switchedCookies, not(hasItem(originalCookie)));
  }
}
