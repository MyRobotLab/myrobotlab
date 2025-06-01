package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.test.AbstractTest;

public class UltrasonicSensorTest extends AbstractTest {

  static MockGateway gateway = null;
  static UltrasonicSensor ultra = null;
  static Arduino uno = null;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    ultra = (UltrasonicSensor) Runtime.start("ultra", "UltrasonicSensor");
    gateway = (MockGateway) Runtime.start("gateway", "MockGateway");
    gateway.clear();
    uno = (Arduino) Runtime.start("uno", "Arduino");
    uno.setVirtual(true);
    uno.connect("COMX");
    ultra.setTriggerPin(11);
    ultra.setEchoPin(10);
    ultra.attach("uno");
    
    //Runtime.start("webgui", "WebGui");
  }

  @Before
  public void setup() {    
    gateway.clear();
  }

  @Test
  public void testUltrasonicSensor() throws TimeoutException {
    assertTrue(ultra.isAttached());
    Double range = ultra.ping();
    assertNotNull(range);
    ultra.addListener("publishRange", "mocker@mockId");
    ultra.startRanging();
    gateway.waitForMsg("mocker@mockId", "onRange", 100);
    ultra.stopRanging();
    Service.sleep(10);
    gateway.clear();
    assertNull(gateway.getMsg("mocker@mockId", "onRange"));
    
    // 1 hz
    ultra.setRate(1);
    ultra.startRanging();
    Message msg = gateway.waitForMsg("mocker@mockId", "onRange", 1500);
    Double r = (Double)msg.data[0];
    assertNotNull(r);
    ultra.stopRanging();
    Integer count = gateway.size("mocker@mockId", "onRange");
    assertTrue(count == 0);
  }


  @AfterClass
  public static void tearDownAfterClass() {
    Runtime.release("ultra");
    Runtime.release("gateway");
    Runtime.release("uno");
  }

}
