package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HtmlFilterTest {

  @Test
  public void testHtmlFilter() {
    String html = "<div>test...</div>";
    String expected = "test...";
    String actual = HtmlFilter.stripHtml(html);
    assertEquals(expected, actual);
  }

}
