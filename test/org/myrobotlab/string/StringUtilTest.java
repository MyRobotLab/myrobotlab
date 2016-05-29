package org.myrobotlab.string;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {

  @Test
  public void testbytesToHex() throws Exception {
    byte[] testBytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    String result = StringUtil.bytesToHex(testBytes);
    assertEquals("000102030405060708090A0B0C0D0E0F", result);
  }

  // TODO: what the heck are the other methods on StringUtil used for ?
  // should we get rid of the string util class?
  // or make it more useful

}
