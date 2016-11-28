package com.amadeus.session.servlet2x;

import static com.amadeus.session.Helpers.assertCookiesMatch;
import static com.amadeus.session.Helpers.callWebapp;
import static com.amadeus.session.Helpers.matchLines;
import static com.amadeus.session.Helpers.setSessionCookie;
import static com.amadeus.session.Helpers.url;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

@SuppressWarnings("javadoc")
public abstract class AbstractITSessionWithCookie {

  @RunAsClient
  @Test
  public void testSessionCreated(@ArquillianResource URL baseURL) throws IOException {
    URL url = url(baseURL, "FirstServlet", "testSessionCreated");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: B"));
    url = new URL(baseURL, "OtherServlet");
    connection = (HttpURLConnection) url.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: C"));
  }

  @RunAsClient
  @Test
  public void testSessionPropagated(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "FirstServlet", "testSessionPropagated");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B"));
    assertCookiesMatch(originalCookie, cookies);
  }

  @RunAsClient
  @Test
  public void testSessionPropagatedChangeServlets(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "FirstServlet", "testSessionPropagatedChangeServlets");
    String originalCookie = callWebapp(urlTest);
    URL urlOther = url(baseURL, "OtherServlet", "testSessionPropagatedChangeServlets");
    HttpURLConnection connection = (HttpURLConnection) urlOther.openConnection();
    setSessionCookie(connection, originalCookie);
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B", "New value of attribute: C"));
    assertCookiesMatch(originalCookie, cookies);
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
    final URL url = url(baseURL, "FirstServlet", "testSessionNotPropagated");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    String originalCookie = cookies.get(0);
    connection = (HttpURLConnection) url.openConnection();
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");
    assertNotNull(cookies);
    assertThat(originalCookie, not(equalTo(cookies.get(0))));
  }

  private static final String BAD_COOKIE = "JSESSIONID=80d47147-0103-48f7-9ce4-999999999999; path=/; HttpOnly";

  @RunAsClient
  @Test
  public void testBadCookieSession(@ArquillianResource URL baseURL) throws IOException {
    final URL url = new URL(baseURL, "FirstServlet");
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
}
