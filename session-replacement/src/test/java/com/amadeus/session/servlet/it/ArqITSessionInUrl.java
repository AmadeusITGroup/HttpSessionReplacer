package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.matchLines;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
@RunWith(Arquillian.class)
public class ArqITSessionInUrl extends AbstractITSession {

  @Deployment(testable = false)
  public static WebArchive startCreateWar() {
    return Helpers.createWebApp("url-session.war")
                  .addClasses(SampleServlet.class, OtherServlet.class, SwitchServlet.class)
                  .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
                                                        .getOrCreateContextParam()
                                                        .paramName(SessionConfiguration.SESSION_PROPAGATOR_NAME)
                                                        .paramValue("URL").up().exportAsString()));

  }

  @RunAsClient
  @Test
  public void testSessionCreated(@ArquillianResource URL baseURL) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url(baseURL, "TestServlet",
                                                           "testSessionCreated").openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: B"));
    connection = (HttpURLConnection) new URL(baseURL, "OtherServlet").openConnection();

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: C"));
  }

  @RunAsClient
  @Test
  public void testSessionPropagated(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagated");
    String originalSession = callWebapp(urlTest);
    assertNotNull(originalSession);
    URL urlNext = url(baseURL, "TestServlet" + originalSession, "testSessionPropagated");
    HttpURLConnection connection = (HttpURLConnection) urlNext.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: B"));
  }

  @RunAsClient
  @Test
  public void testSessionPropagatedChangeServlets(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionPropagatedChangeServlets");
    String originalSession = callWebapp(urlTest);
    URL urlOther = url(baseURL, "OtherServlet" + originalSession, "testSessionPropagatedChangeServlets");
    HttpURLConnection connection = (HttpURLConnection) urlOther.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: B", "New value of attribute: C"));
    connection = (HttpURLConnection) urlOther.openConnection();

    assertThat(connection, matchLines("Previous value of attribute: C", "New value of attribute: C"));
    urlTest = url(baseURL, "TestServlet" + originalSession, "testSessionPropagatedChangeServlets");
    connection = (HttpURLConnection) urlTest.openConnection();
    connection.connect();

    assertThat(connection, matchLines("Previous value of attribute: C", "New value of attribute: B"));
  }

  @RunAsClient
  @Test
  public void testSessionNotPropagated(@ArquillianResource URL baseURL) throws IOException {
    final URL url = url(baseURL, "TestServlet", "testSessionNotPropagated");
    String originalSession = callWebapp(url);
    String newSession = callWebapp(url);
    assertThat(originalSession, not(equalTo(newSession)));
  }

  private static final String BAD_SESSION = ";JSESSIONID=80d47147-0103-48f7-9ce4-999999999999";

  public void testBadSession(@ArquillianResource URL baseURL) throws IOException {
    final URL url = url(baseURL, "TestServlet", "testBadSession");
    sendBadSession(url, BAD_SESSION);
    sendBadSession(url, ";JSESSIONID=123");
    sendBadSession(url, ";JSESSIONID=null");
  }

  private void sendBadSession(final URL url, String badCookie) throws IOException {
    String value = callWebapp(url);
    assertNotNull(value);
    assertTrue(value.startsWith(";JSESSIONID"));
    assertFalse(value.startsWith(badCookie));
  }

  @RunAsClient
  @Test
  public void testSessionSwitched(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionSwitched");
    String originalSession = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) url(baseURL, "TestServlet" + originalSession,
                                                           "testSessionSwitched").openConnection();
    assertThat(connection, matchLines("Previous value of attribute: B", "New value of attribute: B", "Encoded url: /" + originalSession));

    URL urlOther = url(baseURL, "SwitchServlet" + originalSession, "testSessionSwitched");
    String switchedSession = callWebapp(urlOther);

    assertThat(switchedSession, not(equalTo(originalSession)));
    HttpURLConnection connectionOther = (HttpURLConnection) url(baseURL, "TestServlet" + switchedSession,
                                                                "testSessionSwitched").openConnection();
    assertThat(connectionOther, matchLines("Previous value of attribute: S", "New value of attribute: B"));
  }

  static String callWebapp(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try (Scanner s = new Scanner(connection.getInputStream())) {
      while (s.hasNext()) {
        String line = s.nextLine();
        if (line.startsWith("Encoded url: /")) {
          return line.substring("Encoded url: /".length()).trim();
        }
      }
    }
    return null;
  }
}
