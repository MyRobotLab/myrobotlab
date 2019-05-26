package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

@Ignore
public class JMonkeyEngineTest extends AbstractTest {

  static JMonkeyEngine jme = null;

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngineTest.class);
  static SwingGui swing = null;

  static final String TEST_DIR = "src/test/resources/JMonkeyEngine/";
  static final String TEST_FACE_FILE_JPEG = "src/test/resources/JMonkeyEngine/multipleFaces.jpg";
  static final String TEST_INPUT_DIR = "src/test/resources/JMonkeyEngine/kinect-data";
  static final String TEST_TRANSPARENT_FILE_PNG = "src/test/resources/JMonkeyEngine/transparent-bubble.png";

  // TODO - getClassifictions publishClassifications
  // TODO - getFaces publishFaces
  // TODO - chaos monkey filter tester

  public static void main(String[] args) {
    try {
      // // LoggingFactory.init("INFO");

      setUpBeforeClass();

      JMonkeyEngineTest test = new JMonkeyEngineTest();

      test.putTextTest();

      boolean quitNow = true;
      if (quitNow) {
        return;
      }
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    jme = (JMonkeyEngine) Runtime.start("jme", "JMonkeyEngine");
    // Runtime.setLogLevel("info");
    if (!isHeadless()) {
      swing = (SwingGui) Runtime.start("gui", "SwingGui");
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    jme.releaseService();
    if (!isHeadless()) {
      // Runtime.release("gui");
    }
  }

  @Test
  public final void putTextTest() throws Exception {
    log.info("=======JMonkeyEngineTest chaosCaptureTest=======");

    // check after the monkeys have pounded on it - it still works !
    OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
    jme.attach(cv);

    //
    jme.putText("stat: 1\nstat: 2\nstat: 3", 10, 10);
    jme.putText("stat: 5\nstat: 6\nstat: 7", 10, 10);
    jme.putText("IS THIS OVERLAYED", 10, 10, "#FF0000");

    jme.putText("this is new text", 10, 20);
    jme.putText("this is moved new text", 100, 20);
    jme.putText("this is moved new text - replaced", 100, 20);
    // jme.subscribe("cv", "publishPointCloud");

    cv.addFilter("floor", "KinectPointCloud");

    // absolute jme movements
    /**
     * <pre>
     * jme.updatePosition("i01.head.jaw", 70.0);
     * jme.updatePosition("i01.head.jaw", 80.0);
     * jme.updatePosition("i01.head.jaw", 90.0);
     * jme.updatePosition("i01.head.jaw", 100.0);
     * 
     * jme.updatePosition("i01.head.rothead", 90.0);
     * jme.updatePosition("i01.head.rothead", 70.0);
     * jme.updatePosition("i01.head.rothead", 85.0);
     * jme.updatePosition("i01.head.rothead", 130.0);
     * // head.moveTo(90, 90);
     * </pre>
     */


    InMoov i01 = (InMoov) Runtime.create("i01", "InMoov");// has attach ...
                                                          // runtime does
                                                          // dynamic binding
                                                          // anyway...
    InMoovHead head = i01.startHead("COM98");
    Servo s = (Servo) Runtime.getService("i01.head.rothead");
    Servo jaw = (Servo) Runtime.getService("i01.head.jaw");

    // is this necessary ???
    head.rest(); // <- WRONG should not have to do this .. it should be
                 // assumed :P
    // FIXME - there has to be a "default" speed for virtual servos
    s.setVelocity(40.0);
    s.moveTo(0.0); // goes to 30 for rothead - because "min" <-- WRONG 0 should
                 // be 30 .. but start position should be 90 !!!
    s.moveTo(180.0);
    s.moveTo(90.0);
    s.moveTo(0.0);
    for (double i = 90; i < 180; ++i) {
      /// head.moveTo(i, i);
      s.moveTo(i);
      sleep(100);
    }

    // jme.putText("test", 5, 5, 5);
    // cv.capture("../1543648225286");
    // jme.startServoController("i01.left"); // GAH won't work :(
    // jme.startServoController("i01.right");

    //
    // Runtime.start("i01.left", "Jme3ServoController"); GAH won't work :(
    // Runtime.start("i01.right", "Jme3ServoController");
    // jme.start();

    // jme.onPointCloud(cv.getPointCloud());

  }

  // FIXME - do the following test
  // test all frame grabber types
  // test all filters !
  // test remote file source
  // test mpeg streamer

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }
}
