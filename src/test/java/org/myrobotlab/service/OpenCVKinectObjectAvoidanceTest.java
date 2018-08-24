package org.myrobotlab.service;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class OpenCVKinectObjectAvoidanceTest {
  public final static Logger log = LoggerFactory.getLogger(WorkETest.class);

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
    OpenCV cv = (OpenCV)Runtime.start("cv", "OpenCV");
    //cv.setFrameGrabberType(grabberType);
  }
  
  public static void main(String[] args) {
    try {
      LoggingFactory.init("WARN");

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(WorkETest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
