package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.slf4j.Logger;

public class OpenCVTest {

  public final static Logger log = LoggerFactory.getLogger(OpenCVTest.class);

  static OpenCV opencv = null;
  static SwingGui swing = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
    if (!Runtime.isHeadless()) {
      // swing = (SwingGui) Runtime.start("swing", "SwingGui");
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

  @Test
  public final void testFileCapture() {
    opencv.captureFromImageFile("test/resources/OpenCV/multipleFaces.jpg");
    
    opencv.setCameraIndex(3);
    assertEquals(3, opencv.getCameraIndex());
    
    OpenCVData data = opencv.getOpenCVData();
    assertNotNull(data);

    // adding filter when running - TODO - test addFilter when not running
    // opencv.addFilter("FaceDetect");
    
    // no guarantee filter is applied before retrieval
    // data = opencv.getOpenCVData();
    data = opencv.getFaceDetect();
    
    int expected = 3;
    int actual = 7;
   
    //assertEquals(expected, actual);
    // data.get
    
    
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");
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
