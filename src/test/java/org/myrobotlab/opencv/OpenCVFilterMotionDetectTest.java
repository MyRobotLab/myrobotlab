package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterMotionDetectTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    return new OpenCVFilterMotionDetect("filter");
  }

  @Override
  public List<IplImage> createTestImages() {
    // we need two images. (same resolution i guess?
    ArrayList<IplImage> images = new ArrayList<IplImage>();
    images.add(defaultImage());
    images.add(defaultImage());
    return images;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // TODO: verify something useful.
    // waitOnAnyKey();
  }

  @Override
  public IplImage createTestImage() {
    // TODO Auto-generated method stub
    return null;
  }

}
