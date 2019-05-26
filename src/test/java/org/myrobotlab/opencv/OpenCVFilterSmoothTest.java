package org.myrobotlab.opencv;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.nd4j.linalg.io.Assert;

public class OpenCVFilterSmoothTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
    // Loader.load();
  }

  @Override
  public OpenCVFilter createFilter() {
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterSmooth();
    Assert.notNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterSmooth("filter");
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
