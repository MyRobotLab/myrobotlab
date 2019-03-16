package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.test.AbstractTest;
import org.myrobotlab.test.ChaosMonkey;
import org.slf4j.Logger;

// TODO: re-enable this unit test.. but for now it's just too slow ..
// it also opens a swing gui which isn't good.
@Ignore
public class OpenCVTest extends AbstractTest {

  static OpenCV cv = null;

  public final static Logger log = LoggerFactory.getLogger(OpenCVTest.class);
  static SwingGui swing = null;

  static final String TEST_DIR = "src/test/resources/OpenCV/";
  static final String TEST_FACE_FILE_JPEG = "src/test/resources/OpenCV/multipleFaces.jpg";
  static final String TEST_INPUT_DIR = "src/test/resources/OpenCV/kinect-data";
  static final String TEST_TRANSPARENT_FILE_PNG = "src/test/resources/OpenCV/transparent-bubble.png";

  // TODO - getClassifictions publishClassifications
  // TODO - getFaces publishFaces
  // TODO - chaos monkey filter tester

  public static void main(String[] args) {
    try {
      // // LoggingFactory.init("INFO");

      setUpBeforeClass();

      OpenCVTest test = new OpenCVTest();

      test.testGetClassifications();

      boolean quitNow = true;
      if (quitNow) {
        return;
      }

      test.testAllFilterTypes();
      /*
       * cv.capture("https://www.youtube.com/watch?v=I9VA-U69yaY");// red pill
       * // green pill cv.capture(0); cv.stopCapture();
       * cv.setGrabberType("Sarxos"); cv.capture(0);
       * cv.capture("https://www.youtube.com/watch?v=zDO1Q_ox4vk");
       * cv.capture(0);
       * cv.capture("https://www.youtube.com/watch?v=zDO1Q_ox4vk");
       * cv.capture(0);
       */

      test.chaosCaptureTest();

      // test.testAllCaptures();

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(OpenCVTest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cv = (OpenCV) Runtime.start("cv", "OpenCV");
    // if (!isHeadless()) { - no longer needed I believe - SwingGui now handles it
      swing = (SwingGui) Runtime.start("gui", "SwingGui");
    // }
  }

  // FIXME - do the following test
  // test all frame grabber types
  // test all filters !
  // test remote file source
  // test mpeg streamer

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Runtime.release("cv"); // <-- DONT NEED TO DO THIS - Abstract will !

    // FIXME - helper - all threads not in my initial thread set.. tear down
    // TODO - remove all services ??? DL4J others ? Runtime ?
    // clean up all threads
    // clean up all services
    // TODO - these utilities should be in base class !
  }

  @Test
  public final void chaosCaptureTest() throws Exception {
    log.info("=======OpenCVTest chaosCaptureTest=======");
    ChaosMonkey.giveToMonkey(cv, "capture", TEST_FACE_FILE_JPEG);
    ChaosMonkey.giveToMonkey(cv, "capture");
    ChaosMonkey.giveToMonkey(cv, "stopCapture");
    if (hasInternet()) {
      // red pill green pill
      ChaosMonkey.giveToMonkey(cv, "capture", "https://www.youtube.com/watch?v=I9VA-U69yaY");
      ChaosMonkey.giveToMonkey(cv, "capture", "https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
    }
    ChaosMonkey.giveToMonkey(cv, "stopCapture");
    ChaosMonkey.giveToMonkey(cv, "capture", 0); // if hasHardware
    ChaosMonkey.startMonkeys();
    ChaosMonkey.monkeyReport();

    // check after the monkeys have pounded on it - it still works !
    cv.reset();
    cv.capture(TEST_FACE_FILE_JPEG);
    OpenCVData data = cv.getFaceDetect();
    assertNotNull(data);
    List<Rectangle> x = data.getBoundingBoxArray();
    assertTrue(x.size() > 0);
  }

  @Test
  public final void simplteFaceDetect() {
    log.info("=======OpenCVTest simplteFaceDetect=======");
    cv.reset();
    cv.capture(TEST_FACE_FILE_JPEG);
    OpenCVData data = cv.getFaceDetect();
    assertNotNull(data);
    List<Rectangle> listOfFaces = data.getBoundingBoxArray();
    assertTrue(listOfFaces.size() > 0);
  }

  @Test
  public final void testAllCaptures() throws Exception {
    log.info("=======OpenCVTest testAllCaptures=======");

    OpenCVData data = null;

    /**
     * Testing default captures after a reset when the frame grabber type is not
     * explicitly set
     */

    if (hasInternet()) {
      // default internet jpg
      cv.reset();
      cv.capture("https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
      data = cv.getFaceDetect();
      assertNotNull(data);
      assertTrue(data.getBoundingBoxArray().size() > 0);
    }

    // default local mp4
    cv.reset();
    cv.capture("src/test/resources/OpenCV/monkeyFace.mp4");
    data = cv.getFaceDetect();
    assertNotNull(data);
    assertTrue(data.getBoundingBoxArray().size() > 0);

    // default local jpg
    cv.reset();
    cv.capture(TEST_FACE_FILE_JPEG);
    data = cv.getFaceDetect();
    assertNotNull(data);
    assertTrue(data.getBoundingBoxArray().size() > 0);

    // default local directory
    cv.reset();
    cv.capture(TEST_INPUT_DIR);
    assertNotNull(data);

    /**
     * Test ImageFile frame grabber
     */

    if (hasInternet()) {
      cv.reset();
      cv.setGrabberType("ImageFile");
      cv.capture("https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
      data = cv.getFaceDetect();
      assertNotNull(data);
      assertTrue(data.getBoundingBoxArray().size() > 0);
    }

  }

  // TODO test enable disable & enableDisplay

  /**
   * minimally all filters should have the ability to load and run by themselves
   * for a second
   */
  @Test
  public final void testAllFilterTypes() {
    log.info("=======OpenCVTest testAllFilterTypes=======");

    log.info("starting all filters test");
    cv.reset();
    // 19 second blue red pill
    cv.capture("https://www.youtube.com/watch?v=I9VA-U69yaY");

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.info("trying filter {}", fn);
      if (fn.startsWith("DL4J") || fn.startsWith("Tesseract") || fn.startsWith("SimpleBlobDetector") || fn.startsWith("Solr") || fn.startsWith("Split")) {
        log.info("skipping {}", fn);
        continue;
      }
      cv.addFilter(fn);
      sleep(1000);
      cv.removeFilters();
    }
    log.info("done with all filters");
  }

  @Test
  public final void testGetClassifications() {
    log.info("=======OpenCVTest testGetClassifications=======");

    cv.reset();
    cv.setGrabberType("ImageFile");
    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    OpenCVFilter f = cv.addFilter("yolo");
    f.enable();
    Map<String, List<Classification>> classifications = cv.getClassifications();
    assertNotNull(classifications);
    assertTrue(classifications.containsKey("person"));
  }
}
