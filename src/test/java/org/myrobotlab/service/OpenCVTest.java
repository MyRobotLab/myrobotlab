package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.myrobotlab.test.ChaosMonkey;
import org.slf4j.Logger;

// TODO: re-enable this unit test.. but for now it's just too slow ..
// it also opens a swing gui which isn't good.

public class OpenCVTest extends AbstractTest {

  static OpenCV cv = null;

  public final static Logger log = LoggerFactory.getLogger(OpenCVTest.class);

  static final String TEST_DIR = "src/test/resources/OpenCV/";
  static final String TEST_LOCAL_FACE_FILE_JPEG = "src/test/resources/OpenCV/multipleFaces.jpg";
  static final String TEST_LOCAL_MP4 = "src/test/resources/OpenCV/monkeyFace.mp4";
  // static final String TEST_LOCAL_MP4 =
  // "src/test/resources/OpenCV/big_buck_bunny.mp4";

  // static final String TEST_YOUTUBE =
  // "https://www.youtube.com/watch?v=I9VA-U69yaY";
  static final String TEST_INPUT_DIR = "src/test/resources/OpenCV/kinect-data";
  static final String TEST_TRANSPARENT_FILE_PNG = "src/test/resources/OpenCV/transparent-bubble.png";
  // static final String TEST_REMOTE_FILE_JPG = TEST_LOCAL_FACE_FILE_JPEG;
  // static final String TEST_REMOTE_FILE_JPG =
  // "https://en.wikipedia.org/wiki/Isaac_Asimov#/media/File:Isaac.Asimov01.jpg";
  static final String TEST_REMOTE_FILE_JPG = "https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg";
  private static final int MAX_TIMEOUT = 1000 * 300; // 5 minutes

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

  @Rule
  public final TestName testName = new TestName();
  
  @Before
  public void beforeTest() {
    cv.reset();
  }


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    log.info("========= OpenCVTest - setupbefore class - begin loading libraries =========");
    log.info("========= OpenCVTest - setupbefore class - starting cv =========");
    long ts = System.currentTimeMillis();
    cv = (OpenCV) Runtime.start("cv", "OpenCV");

  }

  // FIXME - do the following test
  // test all frame grabber types
  // test all filters !
  // test remote file source
  // test mpeg streamer

  // @Ignore
  @Test
  public final void chaosCaptureTest() throws Exception {
    log.warn("=======OpenCVTest chaosCaptureTest=======");
    ChaosMonkey.giveToMonkey(cv, "capture", TEST_LOCAL_FACE_FILE_JPEG);
    ChaosMonkey.giveToMonkey(cv, "capture");
    ChaosMonkey.giveToMonkey(cv, "stopCapture");
    if (hasInternet()) {
      // red pill green pill
      ChaosMonkey.giveToMonkey(cv, "capture", TEST_LOCAL_MP4);
      ChaosMonkey.giveToMonkey(cv, "capture", TEST_REMOTE_FILE_JPG);
    }
    ChaosMonkey.giveToMonkey(cv, "stopCapture");
    if (!cv.isVirtual()) {
      // if hasHardware camera index 0 - FIXME should check if camera 0 exists ?
      ChaosMonkey.giveToMonkey(cv, "capture", 0);
    }
    ChaosMonkey.startMonkeys();
    ChaosMonkey.monkeyReport();

    // check after the monkeys have pounded on it - it still works !
    cv.reset();
    cv.removeFilters();

    cv.capture(TEST_LOCAL_FACE_FILE_JPEG);
    List<Classification> data = cv.getFaces(MAX_TIMEOUT);
    assertNotNull(data);
    assertTrue(data.size() > 0);
  }

  @Test
  public final void simpleFaces() {
    log.warn("=======OpenCVTest simpleFaces=======");

    cv.reset();
    cv.capture(TEST_LOCAL_FACE_FILE_JPEG);
    List<Classification> data = cv.getFaces(MAX_TIMEOUT);
    assertNotNull(data);
    assertTrue(data.size() > 0);
  }

  @Test
  public final void testAllCaptures() throws Exception {
    log.warn("=======OpenCVTest testAllCaptures=======");

    List<Classification> data = null;

    /**
     * Testing default captures after a reset when the frame grabber type is not
     * explicitly set
     */

    if (hasInternet()) {
      // default internet jpg
      cv.reset();
      // cv.capture("https://upload.wikimedia.org/wikipedia/commons/c/c0/Douglas_adams_portrait_cropped.jpg");
      cv.capture(TEST_REMOTE_FILE_JPG);
      data = cv.getFaces(MAX_TIMEOUT);
      assertNotNull(data);
      assertTrue(data.size() > 0);
    }

    // default local mp4
    cv.reset();
    cv.capture(TEST_LOCAL_FACE_FILE_JPEG);
    data = cv.getFaces(MAX_TIMEOUT);
    assertNotNull(data);
    assertTrue(data.size() > 0);

    // default local jpg
    cv.reset();
    cv.capture(TEST_LOCAL_FACE_FILE_JPEG);
    data = cv.getFaces(MAX_TIMEOUT);
    assertNotNull(data);
    assertTrue(data.size() > 0);

    // default local directory
    cv.reset();
    cv.capture(TEST_INPUT_DIR);
    assertNotNull(data);


  }
  
  @Test
  public void testHttpCapture() {
    
    /**
     * Test ImageFile frame grabber
     */

    if (hasInternet()) {
      cv.reset();
      cv.setGrabberType("ImageFile");
      cv.capture("https://upload.wikimedia.org/wikipedia/commons/f/fe/Isaac_Asimov%2C_RIT_NandE_Vol13Num29_1981_Sep24_Complete.jpg");
      List<Classification> data = cv.getFaces(MAX_TIMEOUT);
      assertNotNull(data);
      assertTrue(data.size() > 0);
    }
    
  }

  // TODO test enable disable & enableDisplay

  /**
   * minimally all filters should have the ability to load and run by themselves
   * for a second
   */
  @Test
  public final void testAllFilterTypes() {
    log.warn("=======OpenCVTest testAllFilterTypes=======");

    log.info("starting all filters test");
    cv.reset();
    // 19 second blue red pill
    cv.capture(TEST_LOCAL_MP4);

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.warn("trying filter {}", fn);
      if ( fn.startsWith("FaceDetectDNN") || fn.startsWith("FaceRecognizer") || fn.startsWith("DL4J") || fn.startsWith("FaceTraining") || fn.startsWith("Tesseract") || fn.startsWith("SimpleBlobDetector") || fn.startsWith("Solr") || fn.startsWith("Split")) {
        log.warn("skipping {}", fn);
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
    log.warn("=======OpenCVTest testGetClassifications=======");
    Runtime.setAllLocales("en");
    cv.reset();
    // cv.removeFilters();
    log.warn("=======OpenCVTest testGetClassifications - 1=======");
    cv.capture(TEST_LOCAL_FACE_FILE_JPEG);
    // OpenCVFilter f =
    log.warn("=======OpenCVTest testGetClassifications - 2=======");
    cv.addFilter("yolo");
    log.warn("=======OpenCVTest testGetClassifications - 3=======");
    // f.enable();
    log.warn("=======OpenCVTest testGetClassifications - cv.getLocale {} =======", cv.getLocale());
    Map<String, List<Classification>> classifications = cv.getClassifications(MAX_TIMEOUT);
    log.warn("=======OpenCVTest testGetClassifications - 4 {} =======", classifications);
    assertNotNull(classifications);
    log.warn("=======OpenCVTest testGetClassifications - 5 =======");
    assertTrue(classifications.containsKey("person"));
    log.warn("=======OpenCVTest testGetClassifications - 6 =======");
  }
}