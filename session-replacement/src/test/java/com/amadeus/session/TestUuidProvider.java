package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestUuidProvider {

  @Test
  public void testNewId() {
    UuidProvider provider = new UuidProvider();
    assertEquals(36, provider.newId().length());
    assertThat(provider.newId(), RegexMatchers.matchesPattern("[a-f0-9\\-]{36}"));
  }

  @Test
  public void testReadId() {
    UuidProvider provider = new UuidProvider();
    assertNull(provider.readId("ABCDEFG"));
    assertNull(provider.readId(""));
    assertNull(provider.readId(null));
    UUID uuid = UUID.randomUUID();
    assertEquals(uuid.toString(), provider.readId(uuid.toString()));
  }
}
