package org.myrobotlab.opencv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvSize;
import static org.bytedeco.opencv.global.opencv_core.cvGetSize;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import org.junit.Before;
import org.myrobotlab.cv.TrackingPoint;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point;

public class OpenCVFilterLKOpticalTrackTest extends AbstractOpenCVFilterTest {

  int frameIndex = 0;
  int numFrames = 0;
  @Before
  public void setup() {
    LoggingFactory.init("info");
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    Loader.load(opencv_java.class);
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterLKOpticalTrack();
    assertNotNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterLKOpticalTrack("lkfilter");
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
      IplImage cropped = cropImage(img,rect);
      //OpenCVFilter.show(cropped, "Cropped " + i);
      images.add(cropped);
    }
    //waitOnAnyKey();
    numFrames = images.size();
    return images;
  }

  private IplImage cropImage(IplImage img, CvRect rect) {
    CvSize sz = new CvSize();
    sz.width(rect.width()).height(rect.height());
    cvSetImageROI(img, rect);
    IplImage cropped = cvCreateImage(sz, img.depth(), img.nChannels());
    // Copy original image (only ROI) to the cropped image
    cvCopy(img, cropped);
    return cropped;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {

    frameIndex++;
    // log.info("Frame: {} CVData: {}", frameIndex, filter.data);
    if (frameIndex == 1) {
      log.info("Sampling a point");
      filter.samplePoint(275, 200);
    }
    if (frameIndex == 2) {
      log.info("Sampling a second point");
      filter.samplePoint(200, 100);
    }
    if (frameIndex == 3) {
      log.info("Sampling a second point");
      filter.samplePoint(235, 150);
    }
    assertNotNull(output);
    
    if (frameIndex == numFrames) {
      // verify stuff!
      // System.err.println("OPENCVDATA: " + filter.data); 
      Map<Integer, TrackingPoint> data = (Map<Integer, TrackingPoint>)filter.data.getObject(CVSERVICENAME + "." + filter.name + ".points");
      
      // there should be 3 tracking points.
      assertEquals(data.size(), 3);
      // assert all 3 points were found
      assertEquals(data.get(0).found.intValue(), 1);
      assertEquals(data.get(1).found.intValue(), 1);
      assertEquals(data.get(2).found.intValue(), 1);
      //verify that the first point was tracked moving left by 20 pixels
      Point p0 = data.get(0).p0;
      Point p1 = data.get(0).p1;
      // we should have tracked it from x = 245 to 225   and y = 200
      assertEquals(p0.x, 245);
      assertEquals(p0.y, 200);
      assertEquals(p1.x, 225);
      assertEquals(p1.y, 200);
    } 
    
    if (debug) {
      waitOnAnyKey();
    }
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

}
