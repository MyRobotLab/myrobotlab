package org.myrobotlab.opencv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterTrackerTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
   // debug = true;
   // LoggingFactory.init("info");
  }

  @Override
  public OpenCVFilter createFilter() {
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterTracker();
    assertNotNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterTracker("filter");
  }

  @Override
  public List<IplImage> createTestImages() {
    // Default behavior, return a list of one default test image.
    ArrayList<IplImage> images = new ArrayList<IplImage>();
    IplImage img = createTestImage();
    // System.err.println("Width : " + img.width());
    // System.err.println("Height : " + img.height());
    // create a rect.. and crop the images.. with a sliding window
    CvRect rect = new CvRect();
    rect.x(100);
    rect.y(100);
    rect.width(300);
    rect.height(300);
    for (int i = 0; i < 25; i+=5) {
      rect.x(rect.x()+i);
      IplImage cropped = OpenCV.cropImage(img,rect);
      //OpenCVFilter.show(cropped, "Cropped " + i);
      images.add(cropped);
    }
    //waitOnAnyKey();
    numFrames = images.size();
    return images;
  }
  
  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    
    log.info("Frame: {} of {}", frameIndex, numFrames);
    if (frameIndex == 1) {
      log.info("Sampling a point");
      filter.samplePoint(275, 200);
    }
    // validate the last frame
    if (frameIndex == numFrames) {
      ArrayList<Point2df> points = (ArrayList<Point2df>)(filter.data.getObject("opencv.output.TrackingPoints"));
      assertEquals(1, points.size());
      Point2df point = points.get(0);
      // plus or minus five pixels
      assertEquals(point.x, 226, 5);
      assertEquals(point.y, 200, 5);
    }
    // Make sure we found 5 faces.
    //log.info("CVData: {}", filter.data);
    //assertNotNull(output);
    if (debug) {
      waitOnAnyKey();
    }
  }

}
