package org.myrobotlab.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.service.interfaces.ServoController;
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

  @Test
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

  @SuppressWarnings("deprecation")
  @Override
  public void testService() throws Exception {
    
    boolean debug = false;
    if (debug) {
      Runtime.start("gui", "SwingGui");
    }
    
    // Start up all services in the inmoov.
    InMoov i01 = (InMoov)service;
    i01.vision.setOpenCVenabled(false);
    i01.startAll(leftPort, rightPort);
    
    Assert.assertNotNull("Arduinos were null.", i01.arduinos);
    
    // start the programab service i guess?  (seems like this should start in startall!)
    i01.startBrain();
    
    Assert.assertNotNull("Chatbot was null after starting brain.", i01.chatBot);
    
    i01.chatBot.onText("are you a robot?");
    
    
    // we should start the eyelids.. they don't start by default.
    // These are just random pin numbers..  
    int eyeLidLeftPin = 24;
    int eyeLidRightPin = 23;
    ServoController controller = i01.arduinos.get(leftPort);
    i01.startEyelids(controller, eyeLidLeftPin, eyeLidRightPin);
    
    
    long startTime = i01.getLastActivityTime();
    // Enable the inmoov.
    
    // this blows up ..
    // why aren't the servos attached at this point?
    
    i01.enable();
    i01.disable();
    i01.enable();
    
    i01.halfSpeed();
    
    i01.fullSpeed();
    
    // TODO: load from a test folder.
    i01.loadGestures();
    
    // save out the current calibration
    i01.saveCalibration();
    
    // try to load that calibration back.
    i01.loadCalibration();
    
    // poses for the servo mixer i guess?
    i01.savePose("pose1");
    
    // test some movements.
    i01.moveHead(90, 90);
    
    i01.moveArm("left", 90, 90, 90, 90);
    i01.moveHand("left", 90, 90, 90, 90, 90);

    i01.moveEyes(90, 90);
    
    i01.moveEyelids(90, 90);
    
    i01.moveTorso(90, 90, 90);
    
    String gestureName = "test-"+ System.currentTimeMillis();
    i01.saveGesture(gestureName);
    i01.loadGestures();
    i01.execGesture(gestureName);
    
    i01.rest();
    
    
    
    
    // other functions
    int pirPin = 1;
    i01.startPIR(leftPort, pirPin);
    
    // now stop the pir
    i01.stopPIR();
    
    // also.. start openni?
    // i01.startOpenNI();
    long delta = i01.getLastActivityTime() - startTime;
    Assert.assertTrue("Activity time not increased!" , delta > 0);
    
    
    // start Open NI I guesS
    i01.startOpenNI();
    
    // TODO: starting opencv also starts a capture.
    
    
    // move stuff.
    i01.moveHeadBlocking(90,90,90,90,90);
    i01.moveTorsoBlocking(90, 90, 90);

    i01.setArmVelocity("left", -1.0,-1.0,-1.0,-1.0);
    i01.setHandVelocity("left", -1.0,-1.0,-1.0,-1.0,-1.0,-1.0);
    i01.setTorsoVelocity(-1.0, -1.0, -1.0);
    i01.setHeadVelocity(-1.0,-1.0,-1.0,-1.0,-1.0,-1.0);
    double testVelocity = i01.leftArm.omoplate.getVelocity();
    
    Assert.assertEquals("Omoplate velocity not -1", -1.0, testVelocity, 0.001);
    
    i01.cameraOff();
    Assert.assertFalse("Camera should be off!", i01.isCameraOn());
    
    
    // if anyhting is attached, this is true.
    i01.isAttached();
    
    
//    i01.releasePeers();
//    i01.releaseService();
    
   // i01.mouth.speak("test");
    //i01.releaseService();
    System.out.println("any key...  do it!");
    System.out.flush();
   // System.in.read();
    
    
    
    
    
    // i01.startAll(leftPort, rightPort);
    
  }

}
