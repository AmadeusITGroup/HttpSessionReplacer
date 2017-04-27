package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestRandomIdProvider {

  @Test
  public void testNewId() {
    RandomIdProvider provider = new RandomIdProvider();
    assertEquals(40, provider.newId().length());
    assertThat(provider.newId(), RegexMatchers.matchesPattern("[A-Za-z0-9_\\-]{40}"));
  }

  @Test
  public void testNewIdWith40Characters() {
    RandomIdProvider provider = new RandomIdProvider(40);
    assertEquals(56, provider.newId().length());
    assertThat(provider.newId(), RegexMatchers.matchesPattern("[A-Za-z0-9_\\-]{56}"));
  }

  @Test
  public void testReadId() {
    RandomIdProvider provider = new RandomIdProvider();
    assertNull(provider.readId("ABCDEFG"));
    assertNull(provider.readId(""));
    assertNull(provider.readId(null));
    assertEquals("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDE",
        provider.readId("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDE"));
  }

  @Test
  public void testConfigure() {
    SessionConfiguration conf = mock(SessionConfiguration.class);
    when(conf.getAttribute(eq(SessionConfiguration.SESSION_ID_LENGTH), any(String.class))).thenReturn("43");
    RandomIdProvider provider = new RandomIdProvider();
    provider.configure(conf);
    assertEquals(60, provider.newId().length());
    assertEquals("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDEabcde_____1234567890",
        provider.readId("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDEabcde_____1234567890"));
  }
}
