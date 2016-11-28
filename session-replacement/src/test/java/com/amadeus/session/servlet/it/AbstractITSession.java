package com.amadeus.session.servlet.it;

import org.junit.Assume;
import org.junit.Before;

@SuppressWarnings("javadoc")
public class AbstractITSession {

  @Before
  public void shouldTestsRun() {
    Assume.assumeTrue(Boolean.valueOf(System.getProperty("arquillian.integration.tests")));
  }
}
