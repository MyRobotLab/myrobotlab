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
  }

}
