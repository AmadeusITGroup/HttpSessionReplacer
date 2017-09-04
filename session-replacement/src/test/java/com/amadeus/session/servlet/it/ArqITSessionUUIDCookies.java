package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.assertFirstLineEquals;
import static com.amadeus.session.servlet.it.Helpers.callWebapp;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

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
public class ArqITSessionUUIDCookies extends AbstractITSession {

  @Deployment(testable = false, name = "uuid")
  public static WebArchive startFirst() {
    return Helpers.createWebApp("uuid.war").addClass(SampleServlet.class)
    .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0")
    .getOrCreateContextParam().paramName(SessionConfiguration.SESSION_ID_PROVIDER).paramValue("uuid").up()
    .exportAsString()));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("uuid")
  public void testSessionIdIsUuid(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "TestServlet", "testSessionIdIsUuid");
    String originalCookie = callWebapp(urlTest);
    String justCookie = originalCookie.split(";", 2)[0];
    assertThat(justCookie, containsString("-"));
    assertThat(justCookie, startsWith("JSESSIONID="));
    try {
      UUID.fromString(justCookie.substring("JSESSIONID=".length()));
    } catch (Exception e) {
      fail("Invalid format of UUID " + justCookie);
    }
    HttpURLConnection connection = (HttpURLConnection)urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    assertFirstLineEquals(connection, "Previous value of attribute: B");
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    assertThat(cookies, hasItem(originalCookie));
  }
}
