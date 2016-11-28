package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.assertFirstLineEquals;
import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@SuppressWarnings("javadoc")
public class ArqITSessionWithCookieTwoApps extends AbstractITSession {

  private static final String SECOND_WEBAPP = "second";
  private static final String FIRST_WEBAPP = "first";

  @Deployment(testable = false, name = FIRST_WEBAPP)
  public static WebArchive startFirst() {
    return Helpers.createWebApp("first.war").addClass(OtherServlet.class)
        .addClass(SampleServlet.class);
  }

  @Deployment(testable = false, name = SECOND_WEBAPP)
  public static WebArchive startSecond() {
    return Helpers.createWebApp("second.war").addClass(SecondServlet.class);
  }

  @RunAsClient
  @Test
  @OperateOnDeployment(FIRST_WEBAPP)
  public void testSessionNotPropagatedToOtherWebapp(@ArquillianResource @OperateOnDeployment(FIRST_WEBAPP) URL firstURL,
      @ArquillianResource @OperateOnDeployment(SECOND_WEBAPP) URL secondURL) throws IOException {
    URL urlTestFirst = url(firstURL, "TestServlet", "testSessionNotPropagatedToOtherWebapp");
    URL urlTestSecond = url(secondURL, "SecondServlet", "testSessionNotPropagatedToOtherWebapp");
    String originalCookie = callWebapp(urlTestFirst);
    System.out.println("First app cookies: " + originalCookie);
    String secondCookie = callSecondWebApp(urlTestSecond, originalCookie);
    // Call again first webapp with cookie from first webapp, it works
    HttpURLConnection connection = (HttpURLConnection) urlTestFirst.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();

    assertFirstLineEquals(connection, "Previous value of attribute: B");

    // Call again first webapp with cookie from second webapp, it creates new
    // session
    connection = (HttpURLConnection) urlTestFirst.openConnection();
    setSessionCookie(connection, secondCookie);
    connection.connect();
    String newCookie = connection.getHeaderFields().get("Set-Cookie").get(0);
    Assert.assertFalse(originalCookie.equals(newCookie));
    assertFirstLineEquals(connection, "Previous value of attribute: null");
  }


  private String callSecondWebApp(URL urlTestSecond, String originalCookie) throws IOException {
    HttpURLConnection connection2 = (HttpURLConnection) urlTestSecond.openConnection();
    setSessionCookie(connection2, originalCookie);

    connection2.connect();
    List<String> cookies2 = connection2.getHeaderFields().get("Set-Cookie");
    System.out.println("Second app cookies: " + cookies2);
    String secondCookie = cookies2.get(0);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection2.getInputStream()))) {
      String line = reader.readLine();
      assertEquals("Previous value of attribute: null", line);
      line = reader.readLine();
      assertEquals("New value of attribute: A", line);
    }
    assertNotNull(cookies2);
    assertEquals(1, cookies2.size());
    assertTrue(cookies2.get(0).startsWith("JSESSIONID"));
    assertFalse(originalCookie.equals(cookies2.get(0)));
    return secondCookie;
  }
}
