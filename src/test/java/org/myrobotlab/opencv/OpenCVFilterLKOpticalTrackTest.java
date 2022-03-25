package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

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
import org.myrobotlab.logging.LoggingFactory;

public class OpenCVFilterLKOpticalTrackTest extends AbstractOpenCVFilterTest {

  int frameIndex = 0;
  @Before
  public void setup() {
    // LoggingFactory.init("info");
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
    return new OpenCVFilterLKOpticalTrack("filter");
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
    log.info("Frame: {} CVData: {}", frameIndex, filter.data);
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
    // System.out.println(filter.data);
    if (debug) {
      waitOnAnyKey();
    }
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

}
