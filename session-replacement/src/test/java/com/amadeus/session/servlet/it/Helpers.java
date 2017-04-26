package com.amadeus.session.servlet.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

@SuppressWarnings("javadoc")
public class Helpers {
  static final String JAR_TO_TEST =
      System.getProperty("session-replacement.jar.file", "target/session-replacement-0.3-SNAPSHOT-shaded.jar");

  public static WebArchive createWebApp(String name) {
    return ShrinkWrap.create(WebArchive.class, name).addAsLibraries(new File(JAR_TO_TEST)).addClass(BaseTestServlet.class);
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
      private Exception exception;

      @Override
      public void describeTo(Description description) {
        description.appendValueList("For HTTP response, first lines should be: ", System.lineSeparator(), System.lineSeparator(), Arrays.asList(lines));
      }

      @Override
      public void describeMismatchSafely(final HttpURLConnection connection, final Description description) {
        List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
        description.appendValueList("was ", System.lineSeparator(), System.lineSeparator(), actualLines);
        if (exception != null) {
          description.appendText("Caught exception.").appendText(exception.toString());
        }
        if (cookies != null) {
          description.appendValueList("Cookies: ", ",", "", cookies);
        }
     }

      @Override
      protected boolean matchesSafely(HttpURLConnection connection) {
        actualLines = new ArrayList<>();
        exception = null;
        try (Scanner s = new Scanner(connection.getInputStream())) {
          for (String line : lines) {
            if (!s.hasNextLine()) {
              return false;
            }
            String actualLine = s.nextLine();
            actualLines.add(actualLine);
            if (!line.equals(actualLine)) {
              return false;
            }
          }
          return true;
        } catch (Exception e) {
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
    assertEquals(1, cookies.size());
    assertTrue(cookies.get(0).startsWith("JSESSIONID"));
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
