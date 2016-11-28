package com.amadeus.session.servlet.it;

import static com.amadeus.session.servlet.it.Helpers.matchLines;
import static com.amadeus.session.servlet.it.Helpers.setSessionCookie;
import static com.amadeus.session.servlet.it.Helpers.url;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("javadoc")
@RunWith(Arquillian.class)
public class ArqITSessionOperations extends AbstractITSession {

  @Deployment(testable = false, name = "session-operations")
  public static WebArchive startFirst() {
    return Helpers.createWebApp("session-operations.war")
        .addClass(SampleServlet.class)
        .addClass(AddAttributeServlet.class)
        .addClass(RemoveAttributeServlet.class)
        .addClass(GetAllAttributesServlet.class);
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("session-operations")
  public void testSessionRemoveAttributes(@ArquillianResource URL baseURL) throws IOException {
    URL url = url(baseURL, "TestServlet", "testSessionRemoveAttributes");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: B"));
    url = url(baseURL, "RemoveAttributeServlet", "testSessionRemoveAttributes");
    System.out.println("COOKIES:" + cookies);
    connection = (HttpURLConnection) url.openConnection();
    setSessionCookie(connection, cookies);

    assertThat(connection, matchLines("Previous value of attribute: B", "New value of attribute: null"));
  }

  @RunAsClient
  @Test
  @OperateOnDeployment("session-operations")
  public void testSessionGetAttributes(@ArquillianResource URL baseURL) throws IOException {
    URL url = url(baseURL, "TestServlet", "testSessionGetAttributes");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: null", "New value of attribute: B"));
    url = url(baseURL, "GetAllAttributes", "testSessionGetAttributes");
    connection = (HttpURLConnection) url.openConnection();
    setSessionCookie(connection, cookies);

    assertThat(connection, matchLines("Attributes in session: [A]"));
    url = url(baseURL, "AddAttributeServlet", "testSessionGetAttributes");
    connection = (HttpURLConnection) url.openConnection();
    setSessionCookie(connection, cookies);
    assertThat(connection, matchLines("Previous value of attribute B: null", "New value of attribute B: D"));
    url = url(baseURL, "GetAllAttributes", "testSessionGetAttributes");
    connection = (HttpURLConnection) url.openConnection();
    setSessionCookie(connection, cookies);
    assertThat(connection, matchLines("Attributes in session: [A, B]"));
  }
}
