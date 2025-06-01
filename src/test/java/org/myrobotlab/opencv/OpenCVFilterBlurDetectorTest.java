package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

/**
 * validate the blur detector produces a value..
 *
 */
public class OpenCVFilterBlurDetectorTest extends AbstractOpenCVFilterTest {

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
    // The default image has a pretty blurry background...
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    Double blurriness = filter.data.getBlurriness();
    assertNotNull(blurriness);
    // low scores indicate a very blurry image.
    assertTrue(blurriness < 100.0);
  }

}
