package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterKinectNavigate;
import org.slf4j.Logger;

public class OpenCVFilterKinectNavigateTest {
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectNavigateTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
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
  public void testClickPoint() {
    OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
    cv.setGrabberType("OpenKinect");
    cv.broadcastState();

    // FIXME - all junit tests should inherit from common parent which handles some of the
    // common features like virtualization and headless

    boolean virtual = true;

    if (virtual) {
      // FIXME - this should just be a single switch - like setting the image
      // file
      // or a 2 parameter method setting the source
      cv.setInputFileName("src/test/resources/OpenCV/white-black-center-640x480.png");
      cv.setGrabberType("ImageFile");
      cv.setInputSource("file");
    } else {
      Runtime.start("gui", "SwingGui");
    }

    OpenCVFilterKinectNavigate filter = new OpenCVFilterKinectNavigate("kinect-nav");
    cv.addFilter(filter);
    cv.capture();
    Service.sleep(2000);
    cv.stopCapture();
    cv.removeFilters();
    // cv.setGrabberType("OpenCV");
    // cv.capture();
    // cv.setGrabberType(grabberType);
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("WARN");

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(OpenCVFilterKinectNavigateTest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
