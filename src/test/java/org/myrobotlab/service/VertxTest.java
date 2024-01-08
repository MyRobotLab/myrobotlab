package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class VertxTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(Vertx.class);

  public static final int port = 9797;

  public static final boolean ssl = false;

  // FIXME - DO A WEBSOCKET TEST

  @BeforeClass
  public static void beforeClass() {
    Vertx vertx2 = (Vertx) Runtime.create("vertx2", "Vertx");
    vertx2.setAutoStartBrowser(false);
    vertx2.setPort(port);
    vertx2.setSsl(ssl);
    vertx2.startService();

    Runtime.start("servoApiTest", "Servo");
    Runtime.start("pythonApiTest", "Python");
    // need to wait for the OS to open the port
    Service.sleep(3);
  }

  public String getUrl(String path) {
    String protocol = (ssl) ? "https" : "http";
    return String.format("%s://localhost:%d/api/service%s", protocol, port, path);
  }

  @Test
  public void getTest() {

    // FIXME - test for minimal time to complete
    // currently, since the api is defined in the vertx router before static 
    // handlers this is very fast < 1s
    for (int i = 0; i < 1000; ++i) {
      byte[] bytes = Http.get(getUrl("/runtime/getUptime"));
      assertNotNull(bytes);
      String ret = new String(bytes);
      assertTrue(ret.contains("days"));
      System.out.println(String.format("%d", i));
    }
  }

  @Test
  public void getTestWithParameter() throws UnsupportedEncodingException {

    byte[] bytes = Http.get(getUrl("/runtime/isLocal/%22runtime%22"));
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
    byte[] bytes = Http.post(getUrl("/runtime/getFullName"), postBody);
    sleep(200); // FIXME - do a wait(1000, bytes or future)
    assertNotNull(bytes);
    String ret = new String(bytes);
    assertTrue(ret.contains("@"));

    // second post - simple input - complex return
    postBody = "[\"runtime\"]";
    bytes = Http.post(getUrl("/runtime/getService"), postBody);
    sleep(200);
    assertNotNull(bytes);
    ret = new String(bytes);
    assertTrue(ret.contains("@"));

    // second post - simple input (including array of strings) - complex return
    // FIXME uncomment when ready - callbacks are not possible through the rest
    // api
    // org.myrobotlab.framework.TimeoutException: timeout of 3000 for
    // proxyName@remoteId.toString exceeded
    // org.myrobotlab.framework.TimeoutException: timeout of 3000 for
    // proxyName@remoteId.getFullName exceeded
    // postBody = "[\"remoteId\", \"proxyName\",
    // \"py:myService\",[\"org.myrobotlab.framework.interfaces.ServiceInterface\"]]";
    // bytes = Http.post(getUrl("/runtime/register", postBody);
    // sleep(200);
    // assertNotNull(bytes);
    // ret = new String(bytes);
    // assertTrue(ret.contains("remoteId"));

    // post non primitive non string object
    MRLListener listener = new MRLListener("getRegistry", "runtime@vertxttest", "onRegistry");
    postBody = "[" + CodecUtils.toJson(listener) + "]";
    // postBody = "[\"runtime\"]";
    bytes = Http.post(getUrl("/runtime/addListener"), postBody);
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
    byte[] bytes = Http.get(getUrl("/servoApiTest/moveTo/35"));
    String ret = new String(bytes);
    assertEquals(ret, "35.0");
    // asynchronous part - msg is put on queue
    sleep(200);
    Servo servoApiTest = (Servo) Runtime.getService("servoApiTest");
    Double pos = servoApiTest.getCurrentOutputPos();
    assertEquals(35.0, pos.doubleValue(), 0.1);

    // return properties
    bytes = Http.get(getUrl("/servoApiTest"));
    ret = new String(bytes);
    assertTrue(ret.contains("servoApiTest"));

    // return methods
    bytes = Http.get(getUrl("/servoApiTest/"));
    ret = new String(bytes);
    assertTrue(ret.contains("enableAutoDisable"));

  }

  @Test
  public void urlEncodingTest() {
    // exec("print \"hello\"")
    byte[] bytes = Http.get(getUrl("/pythonApiTest/exec/%22print+%5C%22hello%5C%22%22"));
    String ret = new String(bytes);
    assertEquals("true", ret);
  }

  @Test
  public void sendBlockingTest() throws InterruptedException, TimeoutException {
    String retVal = "retVal";
    // Put directly in blocking list because sendBlocking() won't use it for
    // local services
    Runtime.getInstance().getInbox().blockingList.put("runtime.onBlocking", new Object[1]);
    Object[] blockingListRet = Runtime.getInstance().getInbox().blockingList.get("runtime.onBlocking");

    // Delay in a new thread so we can get our wait() call in first
    new Thread(() -> {
      try {
        Thread.sleep(50);
      } catch (InterruptedException ignored) {
      }
      Http.post(getUrl("/runtime/onBlocking"), "[\"" + retVal + "\"]");
    }).start();

    long timeout = 1000;
    synchronized (blockingListRet) {
      long startTs = System.currentTimeMillis();
      blockingListRet.wait(timeout);
      if (System.currentTimeMillis() - startTs >= timeout) {
        throw new TimeoutException("timeout of %d exceeded", timeout);
      }
    }

    assertEquals(retVal, blockingListRet[0]);
  }

}
