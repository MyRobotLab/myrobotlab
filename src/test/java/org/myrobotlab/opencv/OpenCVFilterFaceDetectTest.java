package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Assert;

public class OpenCVFilterFaceDetectTest extends AbstractOpenCVFilterTest {
  
  @Before
  public void before() {
    debug = false;
  }
  
  @Override
  public OpenCVFilter createFilter() {
    // create our test filter.
    OpenCVFilterFaceDetect filter = new OpenCVFilterFaceDetect("facedetect");
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    // TODO Auto-generated method stub
    // let's use lena.png.
    IplImage input = defaultImage();
    assertNotNull("bad input image", input);
    return input;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // it should have found 1 face
    OpenCVFilterFaceDetect f = (OpenCVFilterFaceDetect)filter;
    // expected vs actual
    Assert.assertEquals("could not find the 1 face", 1, f.bb.size());
    
  }
  
}
