package com.amadeus.session.servlet2x;

import static com.amadeus.session.Helpers.assertCookiesMatch;
import static com.amadeus.session.Helpers.callWebapp;
import static com.amadeus.session.Helpers.matchLines;
import static com.amadeus.session.Helpers.setSessionCookie;
import static com.amadeus.session.Helpers.url;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amadeus.session.SessionConfiguration;

@RunWith(Arquillian.class)
@SuppressWarnings("javadoc")
public class AgentServlet2xITSessionWithCookieSomeFilter extends AbstractITSessionWithCookie {

  @Deployment(testable = false)
  public static WebArchive startFirst() {
    return ShrinkWrap.create(WebArchive.class, "session-cookie-agent.war")
        .addClasses(FirstServlet2x.class, OtherServlet2x.class,
            BaseServlet.class, SomeFilter.class,
            SessionListenerCalled2x.class,
            InvalidateSession2x.class,
            CallCountingSessionListener.class)
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("2.5")
        .getOrCreateContextParam().paramName(SessionConfiguration.DEFAULT_SESSION_TIMEOUT).paramValue("2").up()
        .createFilter().filterName("someFilter").filterClass(SomeFilter.class.getName()).up()
        .createServlet().servletName("firstServlet").servletClass(FirstServlet2x.class.getName()).up()
        .createServlet().servletName("secondServlet").servletClass(OtherServlet2x.class.getName()).up()
        .createServlet().servletName("invalidateServlet").servletClass(InvalidateSession2x.class.getName()).up()
        .createServlet().servletName("sessionListenerServlet").servletClass(SessionListenerCalled2x.class.getName()).up()
        .createListener().listenerClass(CallCountingSessionListener.class.getName()).up()
        .createFilterMapping().filterName("someFilter").urlPattern("/*").up()
        .createServletMapping().servletName("firstServlet").urlPattern("/FirstServlet").up()
        .createServletMapping().servletName("secondServlet").urlPattern("/OtherServlet").up()
        .createServletMapping().servletName("invalidateServlet").urlPattern("/InvalidateServlet").up()
        .createServletMapping().servletName("sessionListenerServlet").urlPattern("/SessionListener").up()
        .exportAsString()));
  }

  @RunAsClient
  @Test
  public void testSessionListenerCalled(@ArquillianResource URL baseURL) throws IOException {
    URL urlTest = url(baseURL, "FirstServlet", "testListenerCalled");
    String originalCookie = callWebapp(urlTest);
    HttpURLConnection connection = (HttpURLConnection) urlTest.openConnection();
    setSessionCookie(connection, originalCookie);
    connection.connect();
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

    assertThat(connection, matchLines("Previous value of attribute: B"));
    assertCookiesMatch(originalCookie, cookies);
    URL urlQueryListener = url(baseURL, "SessionListener", "testListenerCalled");
    HttpURLConnection listenerConnection = (HttpURLConnection) urlQueryListener.openConnection();
    assertThat(listenerConnection, matchLines("Create called: 2", "Destroy called: 1"));
    URL urlInvalidate = url(baseURL, "InvalidateServlet", "testListenerCalled");
    HttpURLConnection invalidateConnection = (HttpURLConnection) urlInvalidate.openConnection();
    setSessionCookie(invalidateConnection, originalCookie);
    assertThat(invalidateConnection, matchLines("Invalidated", "newSessionIdAfterInvalidate"));
    listenerConnection = (HttpURLConnection) urlQueryListener.openConnection();
    assertThat(listenerConnection, matchLines("Create called: 3", "Destroy called: 2"));
  }
}
