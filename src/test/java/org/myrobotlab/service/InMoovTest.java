package org.myrobotlab.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Service;
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
public class InMoovTest extends AbstractServiceTest implements PinArrayListener {

  // things to test
  private InMoov i01 = null;

  public final static Logger log = LoggerFactory.getLogger(InMoovTest.class);
  static String leftPort = "VIRTUAL_LEFT_PORT";
  static String rightPort = "VIRTUAL_RIGHT_PORT";

  static SerialDevice uart = null;

  static boolean useVirtualHardware = true;
  // virtual hardware
  static VirtualArduino virtualLeft = null;
  static VirtualArduino virtualRight = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    log.info("INFO");
    // FIXME - needs a seemless switch
    if (useVirtualHardware) {
      virtualLeft = (VirtualArduino) Runtime.start("virtualLeft", "VirtualArduino");
      uart = virtualLeft.getSerial();
      uart.setTimeout(100); // don't want to hang when decoding results...
      virtualLeft.connect(leftPort);
      
      virtualRight = (VirtualArduino) Runtime.start("virtualRight", "VirtualArduino");
      uart = virtualRight.getSerial();
      uart.setTimeout(100); // don't want to hang when decoding results...
      virtualRight.connect(rightPort);
      
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

  // @Test
  public void testMinimalScript() throws ClientProtocolException, IOException {
    // create the inmoov and mute it first before running the minimal script.
    InMoov i01 = (InMoov)Runtime.createAndStart("i01", "InMoov");
    i01.setMute(true);
    // Ok.. now run the test.
    Python python = (Python) Runtime.start("python", "Python");
    HttpClient http = (HttpClient) Runtime.start("http", "HttpClient");
    String code = http.get("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/master/home/hairygael/InMoov3.minimal.py");
    python.exec(code);
    
  }

  @Override
  public Service createService() {
    InMoov i01 = (InMoov)Runtime.start("i01", "InMoov");
    i01.setMute(true);
    return i01;
  }

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    InMoov i01 = (InMoov)service;
    i01.setMute(true);
    
    // i01.startAll(leftPort, rightPort);
    
  }

}
