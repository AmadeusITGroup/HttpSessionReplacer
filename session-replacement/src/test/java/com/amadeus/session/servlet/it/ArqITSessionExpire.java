package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.assertFirstLineEquals;
import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
@RunWith(Arquillian.class)
public class ArqITSessionExpire extends AbstractITSession {

  @Deployment(testable = false, name="no-settings")
  public static WebArchive startNoSettings() {
    return Helpers.createWebApp("no-settings.war").addClass(SampleServlet.class);
  }

  @Deployment(testable = false, name="init-param")
  public static WebArchive startInitParam() {
    return Helpers.createWebApp("init-param.war").addClass(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName(SessionConfiguration.DEFAULT_SESSION_TIMEOUT).paramValue("2").up().exportAsString()));
  }

  @Deployment(testable = false, name="never-expires")
  public static WebArchive startNeverExpires() {

    return Helpers.createWebApp("never-expires.war").addClass(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName(SessionConfiguration.DEFAULT_SESSION_TIMEOUT).paramValue("0").up().exportAsString()));
  }

  @Deployment(testable = false, name="web.xml")
  public static WebArchive start2SecondWebXmlConf() {
    return Helpers.createWebApp("web-xml.war").addClass(SampleServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .createSessionConfig().sessionTimeout(2).up().exportAsString()));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("no-settings")
  public void testDefaultSessionDoesNotExpireIn2Seconds(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testDefaultSessionDoesNotExpireIn2Seconds");
    String originalCookie = callWebapp(urlTest);
    await(1000);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
    await(2100);
    connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");
    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("never-expires")
  public void testWhenSessionNeverExpires(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testWhenSessionNeverExpires");
    String originalCookie = callWebapp(urlTest);
    await(1000);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
    await(2100);
    connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    cookies = connection.getHeaderFields().get("Set-Cookie");
    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("init-param")
  public void testSessionDoesNotExpire(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionDoesNotExpire");
    String originalCookie = callWebapp(urlTest);
    await(1000);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("init-param")
  public void testSessionExpires(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionExpires");
    String originalCookie = callWebapp(urlTest);
    await(2100);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    assertFirstLineEquals(connection, "Previous value of attribute: null");
    Assert.assertFalse(originalCookie.equals(cookies.get(0)));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("web.xml")
  public void testSessionDoesNotExpireWebXml(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionDoesNotExpireWebXml");
    String originalCookie = callWebapp(urlTest);
    await(1000);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertFirstLineEquals(connection, "Previous value of attribute: B");
    assertThat(cookies, hasItem(originalCookie));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("web.xml")
  public void testSessionExpiresWebXml(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionExpiresWebXml");
    String originalCookie = callWebapp(urlTest);
    await(2100);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    assertFirstLineEquals(connection, "Previous value of attribute: null");
    Assert.assertFalse(originalCookie.equals(cookies.get(0)));
  }

  // Wait for specified amount of milliseconds
  private static void await(int millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
