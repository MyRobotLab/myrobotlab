package org.myrobotlab.opencv;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.nd4j.linalg.io.Assert;

/**
 * validate the blur detector produces a value..
 *
 */
public class OpenCVFilterBlurDetectorTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    return new OpenCVFilterBlurDetector("filter");
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // TODO: verify!
    log.info("CVData: {}", filter.data);
    // TODO: more info.
    Assert.notNull(output);
    // waitOnAnyKey();
  }

}
