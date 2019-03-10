package org.myrobotlab.opencv;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

@Ignore
public class OpenCVFilterKinectNavigateTest extends AbstractTest {
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectNavigateTest.class);

  @Before
  public void setUp() throws Exception {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void testClickPoint() {
    OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
    cv.setGrabberType("OpenKinect");
    cv.broadcastState();

    // FIXME - all junit tests should inherit from common parent which handles
    // some of the
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

}