package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterLKOpticalTrackTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    // Loader.load(opencv_java.class);
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    Loader.load(opencv_java.class);
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterLKOpticalTrack();
    assertNotNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterLKOpticalTrack("filter");
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
