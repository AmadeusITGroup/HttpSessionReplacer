package com.amadeus.session.servlet2x;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.runner.RunWith;

import com.amadeus.session.Helpers;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.servlet.SessionFilter;

@RunWith(Arquillian.class)
@SuppressWarnings("javadoc")
public class Servlet2xITSessionWithCookieUsingSessionFilter extends AbstractITSessionWithCookie {

  @Deployment(testable = false)
  public static WebArchive startFirst() {
    return Helpers.createWebAppFromDependency("session-cookie.war")
        .addClasses(FirstServlet2x.class, OtherServlet2x.class, BaseServlet.class)
        .addClass("com.amadeus.session.servlet.SessionFilter")
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("2.5")
        .getOrCreateContextParam().paramName(SessionConfiguration.DEFAULT_SESSION_TIMEOUT).paramValue("2").up()
        .createFilter().filterName("sessionFilter").filterClass(SessionFilter.class.getName()).up()
        .createServlet().servletName("firstServlet").servletClass(FirstServlet2x.class.getName()).up()
        .createServlet().servletName("secondServlet").servletClass(OtherServlet2x.class.getName()).up()
        .createFilterMapping().filterName("sessionFilter").urlPattern("/*").up()
        .createServletMapping().servletName("firstServlet").urlPattern("/FirstServlet").up()
        .createServletMapping().servletName("secondServlet").urlPattern("/OtherServlet").up()
        .exportAsString()));
  }
}
