package com.amadeus.session;

/**
 * Helper class that encodes bytes into base64 based String.
 *
 * Characters in string are one of following:
 * <code>ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_</code>
 */
public class Base64MaskingHelper {
	
    private static final int FILLER_CHARACTER_INDEX = 63;
	
    private static final int MASK_6_BITS = 0x3F;

    private static final int DIVIDE_BY_64 = 6;

    private static final int MULTIPLY_BY_256 = 8;

    private static final int BYTES_IN_BLOCK = 3;

    private static final int CHARACTERS_IN_BLOCK = 4;

    private static final char[] SESSION_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
	      .toCharArray();
    
    private Base64MaskingHelper() {}
    /**
     * Encode the bytes into a String with a slightly modified Base64-algorithm
     * This code was written by Kevin Kelley <kelley@ruralnet.net> and adapted by
     * Thomas Peuss <jboss@peuss.de>
     *
     * @param data
     *          The bytes you want to encode
     * @return the encoded String
     */
    public static char[] encode(byte[] data) {
        char[] out = new char[getLengthInCharacters(data.length)];
        char[] alphabet = SESSION_ID_ALPHABET;
        //
        // 3 bytes encode to 4 chars. Output is always an even
        // multiple of 4 characters.
        //
        for (int i = 0, index = 0; i < data.length; i++, index += CHARACTERS_IN_BLOCK) {
          boolean quad = false;
          boolean trip = false;

	      int val = byteValue(data[i]);
          val <<= MULTIPLY_BY_256;
          i++; // NOSONAR each loop is actually i+3, and we increment counter inside loop
          if (i < data.length) {
            val |= byteValue(data[i]);
            trip = true;
          }
          val <<= MULTIPLY_BY_256;
          i++; // NOSONAR each loop is actually i+3, and we increment counter inside loop
          if (i < data.length) {
            val |= byteValue(data[i]);
            quad = true;
          }
          out[index + 3] = alphabet[(quad ? (val & MASK_6_BITS) : FILLER_CHARACTER_INDEX)]; // NOSONAR 3 is not magic!
          val >>= DIVIDE_BY_64;
          out[index + 2] = alphabet[(trip ? (val & MASK_6_BITS) : FILLER_CHARACTER_INDEX)]; // NOSONAR 2 is not magic!
          val >>= DIVIDE_BY_64;
          out[index + 1] = alphabet[val & MASK_6_BITS];
          val >>= DIVIDE_BY_64;
          out[index] = alphabet[val & MASK_6_BITS];
        }
        return out;
    }
    
    private static int byteValue(byte data) {
        return 0xFF & data;
    }
    
    static int getLengthInCharacters(int len) {
        return ((len + (BYTES_IN_BLOCK - 1)) / BYTES_IN_BLOCK) * CHARACTERS_IN_BLOCK;
    }
}
