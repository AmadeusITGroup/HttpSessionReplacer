package com.amadeus.session;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

public class Helpers {
  static final String SHADED_JAR = "com.amadeus:session-replacement:jar:shaded:" +
      System.getProperty("session-replacement.version", "0.2-SNAPSHOT");

  public static WebArchive createWebAppFromDependency(String name) {
    return ShrinkWrap.create(WebArchive.class, name).addAsLibraries(resolveJar());
  }

  public static WebArchive createWebAppNoDependency(String name) {
    return ShrinkWrap.create(WebArchive.class, name);
  }

  static File[] resolveJar() {
    PomEquippedResolveStage pom = Maven.configureResolver()
                                      .workOffline()
                                      .withMavenCentralRepo(false)
                                      .withClassPathResolution(true)
                                      .loadPomFromFile("pom.xml");
    return pom.resolve(Helpers.SHADED_JAR).withTransitivity().asFile();
  }

  public static void assertCookiesMatch(String originalCookie, List<String> cookies) {
    assertNotNull(cookies);
    assertThat(cookies, Matchers.hasItem(originalCookie));
  }

  public static void setSessionCookie(HttpURLConnection connection, String originalCookie) {
    connection.addRequestProperty("Cookie", originalCookie.split(";", 2)[0]);
  }

  public static void setSessionCookie(HttpURLConnection connection, List<String> cookies) {
    setSessionCookie(connection, cookies.get(0));
  }

  public static void assertFirstLineEquals(HttpURLConnection connection, String firstLine) throws IOException {
    assertThat(connection, matchLines(firstLine));
  }

  public static Matcher<HttpURLConnection> matchLines(final String...lines) {
    return new TypeSafeMatcher<HttpURLConnection>() {

      private ArrayList<String> actualLines;
      private IOException exception;

      @Override
      public void describeTo(Description description) {
        description.appendValueList("For HTTP response, first lines should be: ", System.lineSeparator(), System.lineSeparator(), Arrays.asList(lines));
      }

      @Override
      public void describeMismatchSafely(final HttpURLConnection connection, final Description description) {
        List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
        description.appendValueList("was ", System.lineSeparator(), System.lineSeparator(), actualLines);
        if (exception != null) {
          description.appendText("Caught exception.").appendText(exception.getMessage());
        }
        if (cookies != null) {
          description.appendValueList("Cookies: ", ",", "", cookies);
        }
     }

      @Override
      protected boolean matchesSafely(HttpURLConnection connection) {
        actualLines = new ArrayList<>();
        exception = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
          for (String line : lines) {
            String actualLine = reader.readLine();
            if (actualLine == null) {
              return false;
            }
            actualLines.add(actualLine);
            if (!line.equals(actualLine)) {
              return false;
            }
          }
          return true;
        } catch (IOException e) {
          exception = e;
          return false;
        }
      }
    };
  }

  public static String callWebapp(URL url) throws IOException {
    HttpURLConnection connection1 = (HttpURLConnection) url.openConnection();
    List<String> cookies = connection1.getHeaderFields().get("Set-Cookie");
    assertNotNull(cookies);
    assertThat(cookies, hasItem(startsWith("JSESSIONID")));
    String originalCookie = cookies.get(0);
    return originalCookie;
  }

  public static URL url(URL baseUrl, String servlet, String testCase) throws IOException {
    URL url = new URL(baseUrl, servlet);
    String asString = url.toString();
    if (asString.indexOf('?') < 0) {
      asString += "?";
    } else {
      asString += "&";
    }
    asString += "testCase=" + testCase;
    return new URL(asString);
  }
}
