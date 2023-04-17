package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Ignore;

// This test takes a long time..  TODO: consider a smaller / lighterweight version of this test
@Ignore
public class OpenCVFilterDL4JTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    return new OpenCVFilterDL4J("filter");
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // waitOnAnyKey();
  }

}
