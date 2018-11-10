package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

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

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cv = (OpenCV) Runtime.start("cv", "OpenCV");
    Runtime.setLogLevel("warn");
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
  
  @Test
  public final void testCapture() throws InterruptedException {
    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    // TODO -> cv.getFaces();
    
    if (hasInternet()) {
      // remote fileloading
    }
  }
  

  @Test
  public final void testFileCapture() throws InterruptedException {
    
    // we want "best guess" frame grabbers to auto set depdending on the requested resource
    // which is being "captured"
    
    cv.addFilter("yolo");
    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    Map<String, List<Classification>> c = cv.getClassifications();
    
    assertTrue(c.containsKey("person"));
    assertEquals(5, c.get("person").size());
    // cv.stopCapture();
    cv.removeFilters();
    // cv.getFaces();
    
    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      log.warn("trying {}", fn);
      cv.addFilter(fn);
      sleep(100);
      cv.removeFilters();
    }
    
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
    // TODO: sorry for changing the unit test.  this thread sleep is needed now!
    // TODO: remove this thread.sleep call.. 
    long now = System.currentTimeMillis();
    long delta = System.currentTimeMillis() - now;
    int threshold = 1000;
    OpenCVData data = null;
     while (delta <  threshold) {
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
      boolean quitNow = false;
      
      if (quitNow){
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
