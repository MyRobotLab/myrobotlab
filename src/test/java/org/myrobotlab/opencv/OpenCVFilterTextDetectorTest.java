package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

// @Ignore
public class OpenCVFilterTextDetectorTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterTextDetector filter = new OpenCVFilterTextDetector("td");
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    // String filename = "src/test/resources/OpenCV/i_am_a_droid.jpg";
    String filename = "src/test/resources/OpenCV/hiring_humans.jpg";
    return cvLoadImage(filename);
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // assert that we got something semi readable 
    assertEquals("WERE STILL HIRING UMANS Carneqie » Robotics, (7.2l 4", filter.data.getDetectedText());
    // waitOnAnyKey();
  }

}
