package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class HtmlFilterTest extends AbstractTest {

  @Test
  public void testHtmlFilter() {
    String html = "<div>test...</div>";
    String expected = "test...";
    String actual = HtmlFilter.stripHtml(html);
    assertEquals(expected, actual);
  }

}
