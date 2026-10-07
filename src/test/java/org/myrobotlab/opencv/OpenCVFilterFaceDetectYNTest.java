package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterFaceDetectYNTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterFaceDetectYN filter = new OpenCVFilterFaceDetectYN("filter");
    filter.autoDownloadModel = false;
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    String filename = "src/test/resources/OpenCV/multipleFaces.jpg";
    File f = new File(filename);
    if (f.isFile()) {
      return cvLoadImage(filename);
    }
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    assertNotNull(output);
    OpenCVFilterFaceDetectYN yn = (OpenCVFilterFaceDetectYN) filter;
    if (OpenCvZooModels.isInstalled(OpenCvZooModels.YUNET) && yn.loadedModelPath != null && !yn.loadedModelPath.isEmpty()) {
      assertTrue("YuNet should find at least one face", yn.bb.size() > 0);
    }
  }
}
