package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.assertFirstLineEquals;
import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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

import com.amadeus.session.SessionConfiguration;

@SuppressWarnings("javadoc")
@RunWith(Arquillian.class)
public class ArqITSessionBinding extends AbstractITSession {
  @Deployment(testable = false, name="binding-param")
  public static WebArchive startInitParam() {
    return Helpers.createWebApp("binding-param.war").addClass(BindingServlet.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
            .getOrCreateContextParam().paramName(SessionConfiguration.DEFAULT_SESSION_TIMEOUT).paramValue("2").up().exportAsString()));
  }

  
  @RunAsClient
  @Test
  @OperateOnDeployment("binding-param")
  public void testSessionDoesNotExpire(@ArquillianResource URL baseURL) throws IOException, InterruptedException {
    URL urlTest = url(baseURL, "BindingServlet", "testSessionDoesNotExpire");
    String originalCookie = callWebapp(urlTest);
    await(1000);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    assertFirstLineEquals(connection, "Binding calls: 1:0");
    await(2100);
    connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    assertFirstLineEquals(connection, "Binding calls: 1:1");
  }

  // Wait for specified amount of milliseconds
  private static void await(int millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
