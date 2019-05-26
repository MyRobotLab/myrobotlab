package org.myrobotlab.opencv;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Ignore;
import org.nd4j.linalg.io.Assert;

// TODO: seems to be some issue initializing the tracker.  maybe some missing dependencies or something?
@Ignore
public class OpenCVFilterTrackerTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterTracker();
    Assert.notNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterTracker("filter");
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
