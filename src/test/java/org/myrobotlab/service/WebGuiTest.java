package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class WebGuiTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(WebGui.class);
  
  // FIXME - DO A WEBSOCKET TEST 

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
  
  @Test
  public void getTestWithParameter() {

    byte[] bytes = Http.get("http://localhost:8889/api/service/runtime/isLocal/runtime");
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("true"));
  }


// FIXME - ADD WHEN POST API IS WORKY
// FIXME object non primitive (no string) post

  @Test
  public void postTest() {
    
    // 1st post - simple input - simple return
    String postBody = "[\"runtime\"]";
    byte[] bytes = Http.post("http://localhost:8889/api/service/runtime/getFullName", postBody);
    sleep(200); // FIXME - do a wait(1000, bytes or future)
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("@"));
    
    // second post - simple input - complex return
    postBody = "[\"runtime\"]";
    bytes = Http.post("http://localhost:8889/api/service/runtime/getService", postBody);
    sleep(200);
    assertNotNull(bytes);
    ret = new String(bytes);
    assertTrue(ret.contains("@"));
    
    
    // post non primitive non string object
    MRLListener listener = new MRLListener("getRegistry", "runtime@webguittest", "onRegistry");
    postBody = "[" + CodecUtils.toJson(listener) + "]";    
    // postBody = "[\"runtime\"]";
    bytes = Http.post("http://localhost:8889/api/service/runtime/addListener", postBody);
    sleep(200);
    assertNotNull(bytes);
    
    Runtime runtime = Runtime.getInstance();
    boolean found = false;
    List<MRLListener> check = runtime.getNotifyList("getRegistry");
    for (int i = 0; i < check.size(); ++i) {
      if (check.get(i).equals(listener)) {
        found = true;
      }
    }    
    assertTrue("listener not found !", found);
    
  }

  @Test
  public void servoApiTest() {    
    byte[] bytes = Http.get("http://localhost:8889/api/service/servoApiTest/moveTo/35");
    String ret = new String(bytes);
    assertEquals(ret, "35.0");
    // asynchronous part - msg is put on queue
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
