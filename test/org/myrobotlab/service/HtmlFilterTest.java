package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HtmlFilterTest {

  @Test
  public void testHtmlFilter() {
    String html = "<div>test...</div>";
    String expected = "test...";
    HtmlFilter myfilter = (HtmlFilter) Runtime.createAndStart("myfilter", "HtmlFilter");
    String actual = myfilter.stripHtml(html);
    assertEquals(expected, actual);
  }

}
