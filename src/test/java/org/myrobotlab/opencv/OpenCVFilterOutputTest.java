package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterOutputTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
    // Loader.load(opencv_java.class);
    // Loader.load();
    // Service.sleep(10000);
  }

  @Override
  public OpenCVFilter createFilter() {
    // Loader.load(opencv_java.class);
    // Loader.load();
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterOutput();
    assertNotNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterOutput("filter");
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
