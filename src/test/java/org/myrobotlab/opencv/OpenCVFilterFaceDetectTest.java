package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
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
    return input;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // it should have found 1 face
    OpenCVFilterFaceDetect f = (OpenCVFilterFaceDetect)filter;
    // expected vs actual
    Assert.assertEquals(1, f.bb.size());
    
  }
  
}
