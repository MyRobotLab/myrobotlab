package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterYoloOnnxTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilter f = new OpenCVFilterYoloOnnx();
    assertNotNull(f.name);
    f.release();
    OpenCVFilterYoloOnnx filter = new OpenCVFilterYoloOnnx("filter");
    filter.autoDownloadModel = false;
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
