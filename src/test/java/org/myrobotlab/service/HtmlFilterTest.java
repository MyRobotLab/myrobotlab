package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class HtmlFilterTest extends AbstractTest {

  @Test
  public void testHtmlFilter() {
    HtmlFilter filter = (HtmlFilter)Runtime.start("htmlFilter", "HtmlFilter");
    String html = "<div>test...</div>";
    String expected = "test...";
    String actual = HtmlFilter.stripHtml(html);
    assertEquals(expected, actual);
    actual = filter.processText(html);
    assertEquals(expected, actual);
    
    // url strip
    String urls = "blah blah http://www.google.com?search=blah+blah blah";
    String ret = filter.stripUrls(urls);
    assertEquals("blah blah  blah", ret);
    urls = "blah blah http://www.google.com?search=blah+blah&abc=457&3f=77";
    ret = filter.stripUrls(urls);
    assertEquals("blah blah", ret);
    urls = "http://www.google.com?search=blah+blah";
    ret = filter.stripUrls(urls);
    assertEquals("", ret);
    urls = "http://www.google.com?search=blah+blah blah blah";
    ret = filter.stripUrls(urls);
    assertEquals("blah blah", ret);
    urls = "abc http://www.google.com?search=blah+blah blah http://www.google.com?search=blah+blah blah";
    ret = filter.stripUrls(urls);
    assertEquals("abc  blah  blah", ret);       
    
  }

}
