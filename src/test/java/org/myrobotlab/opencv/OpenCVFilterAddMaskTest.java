package org.myrobotlab.opencv;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.nd4j.linalg.io.Assert;

public class OpenCVFilterAddMaskTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    return new OpenCVFilterAddMask("filter");
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    Assert.notNull(output);
    // waitOnAnyKey();
  }

}
