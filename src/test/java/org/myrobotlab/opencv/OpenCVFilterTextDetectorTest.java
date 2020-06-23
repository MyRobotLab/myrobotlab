package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Ignore;
import org.nd4j.linalg.io.Assert;

@Ignore
public class OpenCVFilterTextDetectorTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = true;
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
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    Assert.notNull(output);
    waitOnAnyKey();
  }

}
