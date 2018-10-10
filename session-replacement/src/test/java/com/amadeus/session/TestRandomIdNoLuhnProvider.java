package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestRandomIdNoLuhnProvider {

  @Test
  public void testNewId() {
    RandomIdNoLuhnProvider provider = new RandomIdNoLuhnProvider();
    assertEquals(40, provider.newId().length());
    assertThat(provider.newId(), RegexMatchers.matchesPattern("[A-Za-z0-9_\\-]{40}"));
  }

  @Test
  public void testNewIdWith40Characters() {
    RandomIdNoLuhnProvider provider = new RandomIdNoLuhnProvider(40);
    assertEquals(56, provider.newId().length());
    assertThat(provider.newId(), RegexMatchers.matchesPattern("[A-Za-z0-9_\\-]{56}"));
  }

  @Test
  public void testReadId() {
    RandomIdNoLuhnProvider provider = new RandomIdNoLuhnProvider();
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
    RandomIdNoLuhnProvider provider = new RandomIdNoLuhnProvider();
    provider.configure(conf);
    assertEquals(60, provider.newId().length());
    assertEquals("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDEabcde_____1234567890",
        provider.readId("ABCDEABCDEABCDEABCDEABCDEABCDEABCDEABCDEabcde_____1234567890"));
  }

  // TODO add test that there is no luhn sequence in generated id
}
