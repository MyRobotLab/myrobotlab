package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.junit.Test;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

public class OpenCVFilterFaceDetectDNNTest {

  boolean debug = false; 
  
  @Test
  public void testFaceFilterDNN() throws IOException {
    
    OpenCVFilterFaceDetectDNN filter = new OpenCVFilterFaceDetectDNN("filter");
    String filename = "src/test/resources/OpenCV/multipleFaces.jpg";
    //String filename = "pose.jpg";
    //String filename = "pose2.jpg";
    //String filename = "pose3.jpg";
    //String filename = "pose4.jpg";
   
    IplImage image = cvLoadImage(filename);
    assertNotNull(image);
    if (debug)
      filter.show(image, "Image");
    IplImage result =  filter.process(image);
    if (debug) {
      filter.show(result, "Resulting image");
      System.out.println("Press the any key...");
      System.in.read();
    }
  }
  
 
    
}
