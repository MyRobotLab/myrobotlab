package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class Arduino2Test extends AbstractTest {

  transient public final static Logger log = LoggerFactory.getLogger(Arduino2Test.class);

  private static final String V_PORT_1 = "test_port_1";
  private static final String V_PORT_2 = "test_port_2";

  @Before
  public void setup() throws IOException {    
  }

  @Test
  public void testArduino() throws IOException {
    Arduino ard = (Arduino) Runtime.start("ard01", "Arduino");
    ard.connect(V_PORT_1);
    Service.sleep(300);
    assertTrue(ard.isConnected());
    assertTrue(ard.getBoardInfo().getVersion() == Msg.MRLCOMM_VERSION);
    ard.disconnect();
    assertFalse(ard.isConnected());
    ard.connect(V_PORT_1, 115200, 8, 1, 0);
    assertTrue(ard.isConnected());
    ard.disconnect();
    assertFalse(ard.isConnected());
    // re-connect to a different serial port
    ard.connect(V_PORT_2);
    assertTrue(ard.isConnected());

    // ard.enablePin(address);
    // assertNotNull(ard.getMetaData());
    //
    // // analog write test
    // ard.analogWrite(12, 1);
    // // digital write test.
    // ard.digitalWrite(2, 0);
    // // what the heck do these do?
    // ard.disablePin(1);
    // // what the heck do these do?
    // ard.disablePin("1");
    // ard.disablePins();
    //

    // int res = ard.read(1);
    // System.out.println("RES FROM READ:" + res);

    // leave it disconnected.
    ard.disconnect();
    assertFalse(ard.isConnected());
  }

  @Test
  public void testArduinoPorts() {
    Arduino ard = (Arduino) Runtime.start("ard01", "Arduino");
    ard.connect(V_PORT_1);
    ard.disconnect();
    ard.connect(V_PORT_2);
    ard.disconnect();

    List<String> ports = ard.getPortNames();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ports.size(); ++i) {
      sb.append("[" + ports.get(i) + "]");
      sb.append(" ");
    }
    log.info("testArduinoPorts.getPortNames {}", sb.toString());
    assertTrue(ports.contains(V_PORT_1));
    assertTrue(ports.contains(V_PORT_2));
  }

  @Test
  public void testBoardInfo() {
    Arduino ard = (Arduino) Runtime.start("ard01", "Arduino");
    ard.connect(V_PORT_1);
    assertNotNull(ard.getBoard());
    ard.setBoard("uno");
    assertEquals(ard.getBoard(), "uno");
    ard.setBoard("mega");
    assertEquals(ard.getBoard(), "mega");
  }

  // TODO: this seems broken.
  // @Test
  // public void testResetArduino() {
  // Arduino ard = (Arduino) Runtime.createAndStart("ard01", "Arduino");
  // // TODO: add some devices and then validate that the device list is empty
  // ard.reset();
  // assertEquals(ard.deviceList.size(), 0);
  // }

  // TODO: this is broken! but not in eclipse!
  // @Test
  public void testSketch() {
    Arduino ard = (Arduino) Runtime.start("ard01", "Arduino");
    Sketch s = ard.getSketch();
    assertNotNull(s.name);
    assertNotNull(s.data);
  }

}
