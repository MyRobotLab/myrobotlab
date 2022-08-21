package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class WebGuiTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(WebGui.class);

  @Before
  public void setUp() {
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
  }

  @Test
  public void getTest() {

    byte[] bytes = Http.get("http://localhost:8888/api/service/runtime/getUptime");
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("days"));
  }

  @Test
  public void postTest() {
    String postBody = "[\"runtime\"]";
    byte[] bytes = Http.post("http://localhost:8888/api/service/runtime/getFullName", postBody);
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("@"));
  }

}