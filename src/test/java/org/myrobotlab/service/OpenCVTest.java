package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.slf4j.Logger;

public class OpenCVTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(OpenCVTest.class);

  static OpenCV cv = null;
  static SwingGui swing = null;

  static final String TEST_DIR = "src/test/resources/OpenCV/";
  static final String TEST_FACE_FILE_JPEG = "src/test/resources/OpenCV/multipleFaces.jpg";
  static final String TEST_TRANSPARENT_FILE_PNG = "src/test/resources/OpenCV/transparent-bubble.png";

  // TODO - getClassifictions publishClassifications
  // TODO - getFaces publishFaces
  // TODO - chaos monkey filter tester

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cv = (OpenCV) Runtime.start("cv", "OpenCV");
    Runtime.setLogLevel("info");
    if (!isHeadless()) {
      swing = (SwingGui) Runtime.start("swing", "SwingGui");
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    // TODO - remove all services ??? DL4J others ? Runtime ?
    // clean up all threads
    // clean up all services
    // TODO - these utilities should be in base class !
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
    // cuz ffmpeg is the most durable
    cv.setGrabberType("FFmpeg");
  }

  // FIXME - do the following test
  // test all frame grabber types
  // test all filters !
  // test remote file source
  // test mpeg streamer

  public class ChaosButtonPusher extends Thread {
    public boolean running = false;
    Random ran = new Random();

    public void run() {
      running = true;
      while (running) {
        int x = ran.nextInt(2);
        if (x == 0) {
          log.info("capture");
          cv.capture();
        } else {
          log.info("stop capture");
          cv.stopCapture();
        }
        try {
          sleep(50);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  @Test
  public final void testChaosButtons() {
    ChaosButtonPusher b0 = new ChaosButtonPusher();
    b0.start();

    ChaosButtonPusher b1 = new ChaosButtonPusher();
    b1.start();

    ChaosButtonPusher b2 = new ChaosButtonPusher();
    b2.start();

    // 5 seconds of pure chaos monkey !
    sleep(5000);

    b0.running = false;
    b1.running = false;
    b2.running = false;

    sleep(1000);

    // can we still work ?
    cv.capture(TEST_FACE_FILE_JPEG);
    OpenCVData data = cv.getOpenCVData();
    assertTrue(data != null);
    cv.stopCapture();

  }
  
  @Test
  public final void chaosCaptureTest() throws Exception {
    giveToMonkey(cv, "capture", TEST_FACE_FILE_JPEG);
    giveToMonkey(cv, "stopCapture");
    giveToMonkey(cv, "capture", "https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
    giveToMonkey(cv, "stopCapture");
    // giveToMonkey(cv, "capture", 0); // if hasHardware
    startMonkeys();
    monkeyReport();
    cv.reset();
  }


  @Test
  public final void testAllCaptures() throws InterruptedException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, org.bytedeco.javacv.FrameGrabber.Exception {

    OpenCVData data = null;
   
    if (hasInternet()) {
     
      cv.reset();
      // default is FFmpeg - but this too should be able to get the jpg
      cv.setGrabberType("ImageFile");
      cv.capture("https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
      data = cv.getFaceDetect();
      assertNotNull(data);
      
      
      // if you have a internet this should auto-load the ffmpeg grabber and start capturing 
      // mr. adam's picture and return a face
      cv.reset();
      cv.capture("https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
      // wait up to 10 seconds for bad internet connection
      data = cv.getFaceDetect(10000);
      assertNotNull(data);
    }
    
    cv.reset();
    cv.capture(TEST_FACE_FILE_JPEG);
    data = cv.getFaceDetect();
    assertNotNull(data);
    
    // verify it will switch to something which
    // might capture
    // cv.capture("src/test/resources/OpenCV/monkeyFace.mp4");

    /*
     * for (String g : grabbers) { cv.setGrabberType(g); assertEquals(g,
     * cv.getGrabberType()); cv.getGrabber();
     * 
     * }
     */

    cv.capture(TEST_FACE_FILE_JPEG);

    cv.setGrabberType("FFmpeg");
    assertEquals("FFmpeg", cv.getGrabberType());

    // grabber types - if i set a grabber it should stay that way -
    // setGrabber(null) ???

    // cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    // TODO -> cv.getFaces();

    // file types

    if (hasInternet()) {
      // remote fileloading
    }
  }

  @Test
  public final void testAllFilterTypes() {
    // cv.capture(TEST_FACE_FILE_JPEG);
    cv.stopCapture();
    // cv.capture("multipleFaces.jpg"); did not work :(

    cv.capture("https://www.youtube.com/watch?v=zDO1Q_ox4vk");

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.info("trying {}", fn);
      if (fn.startsWith("DL4J")) {
        continue;
      }
      cv.addFilter(fn);
      sleep(1000);
      cv.removeFilters();
    }

  }

  @Test
  public final void testFileCapture() throws InterruptedException {

    // we want "best guess" frame grabbers to auto set depdending on the
    // requested resource
    // which is being "captured"

    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");

    /**
     * <pre>
     *  good test
       cv = Runtime.start("cv", "OpenCV")
       cv.setGrabberType("OpenCV")
       yoloFilter=cv.addFilter("Yolo")
       yoloFilter.disable()
       cv.capture()
       yoloFilter.enable()
       cv.setDisplayFilter("Yolo")
       yoloFilter.disable()
       yoloFilter.enable()
       yoloFilter.disable()
       yoloFilter.enable()
     * </pre>
     */

    /*
     * for (String fn : OpenCV.POSSIBLE_FILTERS) { log.warn("trying {}", fn);
     * 
     * if (fn.equals("DL4J")) { break; } cv.addFilter(fn); sleep(1000);
     * cv.removeFilters(); }
     */

    // cv .addFilter("yolo");
    /*
     * Map<String, List<Classification>> c = cv.getClassifications();
     * 
     * assertTrue(c.containsKey("person")); assertEquals(5,
     * c.get("person").size()); // cv.stopCapture(); cv.removeFilters(); //
     * cv.getFaces();
     * 
     * cv.pauseCapture(); sleep(100); int frameIndex = cv.getFrameIndex();
     * sleep(100); assertEquals(frameIndex, cv.getFrameIndex());
     * cv.resumeCapture(); sleep(100); assertTrue(cv.isCapturing());
     * assertTrue(frameIndex < cv.getFrameIndex()); cv.stopCapture();
     * assertTrue(!cv.isCapturing()); sleep(100); frameIndex =
     * cv.getFrameIndex(); sleep(100); assertEquals(frameIndex,
     * cv.getFrameIndex());
     * 
     * cv.setCameraIndex(3); assertEquals(3, cv.getCameraIndex());
     * cv.capture("src/test/resources/OpenCV/multipleFaces.jpg"); // TODO: sorry
     * for changing the unit test. this thread sleep is needed now! // TODO:
     * remove this thread.sleep call.. long now = System.currentTimeMillis();
     * long delta = System.currentTimeMillis() - now; int threshold = 1000;
     * cv.capture(); OpenCVData data = null; while (delta < threshold) { delta =
     * System.currentTimeMillis() - now; data = cv.getOpenCVData(); if (data !=
     * null) break; } assertNotNull(data); // adding filter when running - TODO
     * - test addFilter when not running // cv.addFilter("FaceDetect"); // no
     * guarantee filter is applied before retrieval // data =
     * cv.getOpenCVData(); data = cv.getFaceDetect();
     */
  }

  public static void main(String[] args) {
    try {
      // LoggingFactory.init("INFO");
      OpenCVTest test = new OpenCVTest();
      setUpBeforeClass();
      test.chaosCaptureTest();
      
      // test.testAllCaptures();

      boolean quitNow = true;

      if (quitNow) {
        return;
      }

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(OpenCVTest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
