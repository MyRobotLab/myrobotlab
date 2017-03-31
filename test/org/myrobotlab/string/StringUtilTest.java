package org.myrobotlab.string;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

  
  @Test
  public void testChunkString() {
    int maxLength = 30;
    String text = "This is a test of the emergency broadcast system. It has been done in a foo bar. With Testing of some foo. Slightly more bar. A little bit of foo though. And why shouldn't there be some sort of fooness to the air. foo for all and all for foo."; 
    List<String> result = StringUtil.chunkText(text, maxLength);
    //for (String s : result) {
    //  System.out.println(s);
    //}
    //System.out.println("ORG:"+text);
    //System.out.println("RES:"+StringUtils.join(result, " "));
    assertEquals(text, StringUtils.join(result, " "));
    
  }
}
