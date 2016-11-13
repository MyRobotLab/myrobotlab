package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class HtmlFilterTest {

  private HtmlFilter testHtmlFilter = null;

  @Before 
  public void startService() {
    testHtmlFilter = (HtmlFilter) Runtime.createAndStart("myfilter", "HtmlFilter");    
    assertNotNull(testHtmlFilter);
  }
  
  @Test
  public void testHtmlFilter() {
    String html = "<div>test...</div>";
    String expected = "test...";
    String actual = testHtmlFilter.stripHtml(html);
    assertEquals(expected, actual);
  } 

  @Test
  public void testStripAccents() {
    String testText = "C'est français";
    String expected = "C'est francais";
    testHtmlFilter.setStripAccents(true);
    testHtmlFilter.setLowercase(false);
    String actual = testHtmlFilter.stripHtml(testText);
    assertEquals(expected, actual);
  }
  
  @Test
  public void testTrim() {
    String testText = " a ";
    testHtmlFilter.setTrim(false);
    String actual = testHtmlFilter.stripHtml(testText);
    assertEquals(testText, actual);
    String expected = "a";
    testHtmlFilter.setTrim(true);
    actual = testHtmlFilter.stripHtml(testText);
    assertEquals(expected, actual);    
  }
  
  
  @Test
  public void testLowercase() {
    String testText = "A";
    testHtmlFilter.setLowercase(false);
    String actual = testHtmlFilter.stripHtml(testText);
    assertEquals(testText, actual);
    String expected = "a";
    testHtmlFilter.setLowercase(true);
    actual = testHtmlFilter.stripHtml(testText);
    assertEquals(expected, actual);    
  }
  
}
