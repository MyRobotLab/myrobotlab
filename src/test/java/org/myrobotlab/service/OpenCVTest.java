package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.slf4j.Logger;

public class OpenCVTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(OpenCVTest.class);

  static OpenCV cv = null;
  static SwingGui swing = null;
  
  static final String FACE_JPEG_TEST_FILE = "src/test/resources/OpenCV/multipleFaces.jpg"; 

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
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
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
    cv.capture(FACE_JPEG_TEST_FILE);
    OpenCVData data = cv.getOpenCVData();
    assertTrue(data != null);
    cv.stopCapture();
    
  }

  @Test
  public final void testAllGrabberTypes() throws InterruptedException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, org.bytedeco.javacv.FrameGrabber.Exception {

    Set<String> grabbers = OpenCV.getGrabberTypes();

    // "unset" grabber type
    cv.setFrameGrabberType(null);
    assertEquals(null, cv.getGrabberType());

    // verify it will switch to something which
    // might capture
    // cv.capture("src/test/resources/OpenCV/monkeyFace.mp4");

    /*
    for (String g : grabbers) {
      cv.setFrameGrabberType(g);
      assertEquals(g, cv.getGrabberType());
      cv.getGrabber();

    }
    */
    
    cv.capture(FACE_JPEG_TEST_FILE);

    cv.setFrameGrabberType("FFmpeg");
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
    cv.capture(FACE_JPEG_TEST_FILE);

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.warn("trying {}", fn);
      if (fn.equalsIgnoreCase("BoundingBoxToFile")) {
        log.error("here");
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

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.warn("trying {}", fn);
      if (fn.equalsIgnoreCase("BoundingBoxToFile")) {
        log.error("here");
      }
      cv.addFilter(fn);
      sleep(1000);
      cv.removeFilters();
    }

    // cv .addFilter("yolo");

    Map<String, List<Classification>> c = cv.getClassifications();

    assertTrue(c.containsKey("person"));
    assertEquals(5, c.get("person").size());
    // cv.stopCapture();
    cv.removeFilters();
    // cv.getFaces();

    cv.pauseCapture();
    sleep(100);
    int frameIndex = cv.getFrameIndex();
    sleep(100);
    assertEquals(frameIndex, cv.getFrameIndex());
    cv.resumeCapture();
    sleep(100);
    assertTrue(cv.isCapturing());
    assertTrue(frameIndex < cv.getFrameIndex());
    cv.stopCapture();
    assertTrue(!cv.isCapturing());
    sleep(100);
    frameIndex = cv.getFrameIndex();
    sleep(100);
    assertEquals(frameIndex, cv.getFrameIndex());

    cv.setCameraIndex(3);
    assertEquals(3, cv.getCameraIndex());
    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    // TODO: sorry for changing the unit test. this thread sleep is needed now!
    // TODO: remove this thread.sleep call..
    long now = System.currentTimeMillis();
    long delta = System.currentTimeMillis() - now;
    int threshold = 1000;
    cv.capture();
    OpenCVData data = null;
    while (delta < threshold) {
      delta = System.currentTimeMillis() - now;
      data = cv.getOpenCVData();
      if (data != null)
        break;
    }
    assertNotNull(data);
    // adding filter when running - TODO - test addFilter when not running
    // cv.addFilter("FaceDetect");
    // no guarantee filter is applied before retrieval
    // data = cv.getOpenCVData();
    data = cv.getFaceDetect();

  }

  public static void main(String[] args) {
    try {
      // LoggingFactory.init("INFO");
      OpenCVTest test = new OpenCVTest();
      setUpBeforeClass();
      test.testAllGrabberTypes();

      boolean quitNow = false;

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
