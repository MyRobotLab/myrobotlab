package org.myrobotlab.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */
public class InMoovTest implements PinArrayListener {

	public final static Logger log = LoggerFactory.getLogger(InMoovTest.class);

	static boolean useVirtualHardware = true;
	static String port = "COM7";

	// things to test
	static InMoov i01 = null;

	// virtual hardware
	static VirtualArduino virtual = null;
	static SerialDevice uart = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info("setUpBeforeClass");


		// FIXME - needs a seemless switch
		if (useVirtualHardware) {
			virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
			uart = virtual.getSerial();
			uart.setTimeout(100); // don't want to hang when decoding results...
			virtual.connect(port);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		/**
		 * Arduino's expected state before each test is
		 * 'connected' with no devices, no pins enabled
		 */


		uart.clear();
		uart.setTimeout(100);
	}

	@Test
	public void testReleaseService() {
	}


  @Override
  public boolean isLocal() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onPinArray(PinData[] pindata) {
    // TODO Auto-generated method stub
    
  }
  

  @Test
  public void testMinimalScript() throws ClientProtocolException, IOException {
    Python python = (Python)Runtime.start("python", "Python");
    HttpClient http = (HttpClient)Runtime.start("http", "HttpClient");
    String code = http.get("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/master/home/hairygael/InMoov3.minimal.py");
    python.exec(code);
  }
  

  public static void main(String[] args) {
    try {
      
      boolean runMainOnly = true;
      
      LoggingFactory.init("INFO");
      InMoovTest.setUpBeforeClass();
      
     // Runtime.start("webgui", "WebGui");
     //  Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");

      
      
      if (virtual != null) {
        virtual.connect(port);
      }
      
      
      InMoovTest test = new InMoovTest();
      
      
      test.testMinimalScript();
    
      // test something specific
      // test.testConnectString();
      
      if (runMainOnly){
        return;
      }

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(InMoovTest.class);
      log.info("Result was: {}", result);

      // Runtime.dump();

    } catch (Exception e) {
      Logging.logError(e);
    }
  }


}
