package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterQrCodeTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilter f = new OpenCVFilterQrCode();
    assertNotNull(f.name);
    f.release();
    OpenCVFilterQrCode filter = new OpenCVFilterQrCode("filter");
    filter.mode = OpenCVFilterQrCode.MODE_QR;
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    assertNotNull(output);
  }
}
