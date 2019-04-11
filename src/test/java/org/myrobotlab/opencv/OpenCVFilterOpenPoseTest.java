package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.springframework.util.Assert;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;


import java.io.IOException;
import java.text.ParseException;

public class OpenCVFilterOpenPoseTest extends AbstractOpenCVFilterTest {

  @Override
  public OpenCVFilter createFilter() {
    debug = true;
//    try {
//      Runtime.install("OpenCV");
//    } catch (ParseException | IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
    OpenCVFilterOpenPose filter = new OpenCVFilterOpenPose("filter");
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    LoggingFactory.init("INFO");
    
    try {
      Runtime.install("OpenCV");
    } catch (ParseException|IOException e) {
      log.warn("failed to install opencv.", e);
      return null;
    }
    
    //String imgFilename = "src/test/resources/OpenCV/multipleFaces.jpg";
    //String imgFilename = "src/test/resources/OpenCV/bettie.jpg";
    String imgFilename = "src/test/resources/OpenCV/standing-man.jpg";
   // String imgFilename = "src/test/resources/OpenCV/standing-woman-1.jpg";
    IplImage img = cvLoadImage(imgFilename);
    return img;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // TODO: some validation.
    Assert.notNull(filter.data);
    System.out.println(filter.data);
    if (debug)
      waitOnAnyKey();
    
  }

}
