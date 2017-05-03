package com.amadeus.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestBase64MaskingHelper {

	@Test
	public void testEncode() {
		byte[] bytes = {-44, 91, -72, -64, -42, 112, 91, 10, 91, 34, -11, -11, -60, 67, -90, -85, 54, 117, 84, 8, -98, -123, 30, -13, 102, -33, 12, 34, 62, -9};
		String output = new String(Base64MaskingHelper.encode(bytes));	
		assertNotNull(output);
		assertEquals(40, output.length());
		assertEquals("1Fu4wNZwWwpbIvX1xEOmqzZ1VAiehR7zZt8MIj73", output);
		
		byte[] timeArray = {0, 0, 1, 91, -51, -118, -51, 9};
		output = new String(Base64MaskingHelper.encode(timeArray));
		assertNotNull(output);
		assertEquals(12, output.length()); //output is always an even multiple of 4
		assertEquals("AAABW82KzQk_", output);
	}
}
