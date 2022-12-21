package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
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
    
    Runtime.start("servoApiTest","Servo");
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

// FIXME - ADD WHEN POST API IS WORKY
  @Test
  public void postTest() {
    String postBody = "[\"runtime\"]";
    byte[] bytes = Http.post("http://localhost:8889/api/service/runtime/getFullName", postBody);
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("@"));
  }

  @Test
  public void servoApiTest() {    
    byte[] bytes = Http.get("http://localhost:8889/api/service/servoApiTest/moveTo/35");
    String ret = new String(bytes);
    assertEquals(ret, "35.0");
    sleep(200);
    Servo servoApiTest = (Servo)Runtime.getService("servoApiTest");
    Double pos = servoApiTest.getCurrentOutputPos();
    assertEquals(35.0, pos.doubleValue(), 0.1);
    
    // return properties
    bytes = Http.get("http://localhost:8889/api/service/servoApiTest");
    ret = new String(bytes);
    assertTrue(ret.contains("servoApiTest"));
    
    // return methods
    bytes = Http.get("http://localhost:8889/api/service/servoApiTest/");
    ret = new String(bytes);
    assertTrue(ret.contains("enableAutoDisable"));

  }

  
}