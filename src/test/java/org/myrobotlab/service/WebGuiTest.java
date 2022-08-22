package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class WebGuiTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(WebGui.class);

  @Before
  public void setUp() {
    WebGui webgui2 = (WebGui) Runtime.create("webgui2", "WebGui");
    webgui2.autoStartBrowser(false);
    webgui2.setPort(8889);
    webgui2.startService();
    // need to wait for the OS to open the port
    Service.sleep(3);
  }

  @Test
  public void getTest() {

    byte[] bytes = Http.get("http://localhost:8889/api/service/runtime/getUptime");
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("days"));
  }

  @Test
  public void postTest() {
    String postBody = "[\"runtime\"]";
    byte[] bytes = Http.post("http://localhost:8889/api/service/runtime/getFullName", postBody);
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("@"));
  }

}