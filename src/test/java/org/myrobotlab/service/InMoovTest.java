package org.myrobotlab.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */
public class InMoovTest extends AbstractTest implements PinArrayListener {

  // things to test
  static InMoov i01 = null;

  public final static Logger log = LoggerFactory.getLogger(InMoovTest.class);
  static String port = "COM7";

  static SerialDevice uart = null;

  static boolean useVirtualHardware = true;
  // virtual hardware
  static VirtualArduino virtual = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // LoggingFactory.init("WARN");
    log.info("setUpBeforeClass");
    // FIXME - needs a seemless switch
    if (useVirtualHardware) {
      virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      uart = virtual.getSerial();
      uart.setTimeout(100); // don't want to hang when decoding results...
      virtual.connect(port);
    }
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isLocal() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void onPinArray(PinData[] pindata) {
    // TODO Auto-generated method stub

  }

  @Before
  public void setUp() throws Exception {
    /**
     * Arduino's expected state before each test is 'connected' with no devices,
     * no pins enabled
     */
    uart.clear();
    uart.setTimeout(100);
  }

  @Test
  public void testMinimalScript() throws ClientProtocolException, IOException {
    Python python = (Python) Runtime.start("python", "Python");
    HttpClient http = (HttpClient) Runtime.start("http", "HttpClient");
    String code = http.get("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/master/home/hairygael/InMoov3.minimal.py");
    python.exec(code);
  }

}
