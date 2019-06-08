package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertEquals;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

/** 
 * @author GroG
 *
 */
public class OpenCVFilterFaceDetectDNNTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterFaceDetectDNN filter = new OpenCVFilterFaceDetectDNN("filter");
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    String filename = "src/test/resources/OpenCV/multipleFaces.jpg";
    IplImage image = cvLoadImage(filename);
    return image;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    int numFound = ((OpenCVFilterFaceDetectDNN)filter).bb.size();
    assertEquals(5, numFound);
    
    // waitOnAnyKey();
  }

}
