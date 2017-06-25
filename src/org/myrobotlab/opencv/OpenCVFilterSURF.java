/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GRAY2BGR;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.ObjectFinder;
import org.bytedeco.javacv.ObjectFinder.Settings;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSURF extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSURF.class);
  public Settings settings = new Settings();
  public ObjectFinder objectFinder = null;
  transient private IplImage object = null;

  public OpenCVFilterSURF() {
    super();

  }

  public OpenCVFilterSURF(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO: impl something here?
  }

  public void loadObjectImageFilename(String filename) {
    IplImage object = cvLoadImage(filename, CV_LOAD_IMAGE_GRAYSCALE);
    this.setObjectImage(object);
    this.objectFinder = new ObjectFinder(settings);
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {
    // TODO: Expose configuration of the object image to find.
    // TODO: create proper life cycle for the objectFinder obj.
    if (object == null || image == null) {
      return image;
    }

    IplImage objectColor = IplImage.create(object.width(), object.height(), 8, 3);
    cvCvtColor(object, objectColor, CV_GRAY2BGR);

    // object is now black and white
    IplImage imageBW = IplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    // image bw is now black and white

    // a new image to hold the side by side comparison
    int correspondWidth = image.width() + object.width();
    int correspondHeight = Max(image.height(), object.height());

    // Create a new image to hold the object and the image side by side
    IplImage correspond = IplImage.create(correspondWidth, correspondHeight, 8, 1);

    // IplImage correspond = IplImage.create(Max(image.width(), object.width()),
    // object.height() + image.height(), 8, 1);

    // Copy the object to be found into the corresponding image
    cvSetImageROI(correspond, cvRect(0, 0, object.width(), object.height()));
    cvCopy(object, correspond);

    // cvSetImageROI(correspond, cvRect(0, object.height(), correspondWidth,
    // correspondHeight));
    // Copy the image to the corresponding

    cvSetImageROI(correspond, cvRect(object.width(), 0, image.width(), image.height()));

    // cvSetImageROI(correspond, cvRect(0, object.width(), imageBW.width(),
    // imageBW.height()));
    cvCopy(imageBW, correspond);
    //
    cvResetImageROI(correspond);

    // set up the object finder
    // TOOD: potentially re-use this finder object?
    if (settings == null) {
      ObjectFinder.Settings settings = new ObjectFinder.Settings();
      settings.setObjectImage(object);
      settings.setUseFLANN(true);
      settings.setRansacReprojThreshold(5);
      // settings.setHessianThreshold(50);
    }
    ObjectFinder finder = new ObjectFinder(settings);

    // grab a timestamp
    long start = System.currentTimeMillis();

    // if we find it i guess the bounding box is here!
    double[] dst_corners = finder.find(image);
    log.info("Finding time = " + (System.currentTimeMillis() - start) + " ms");

    if (dst_corners != null && dst_corners.length > 0) {
      log.info("DIST CORNER SIZE :" + dst_corners.length);
      // TODO: add a callback
      // TODO: add this info the the CVData
      int pointCount = 0;
      for (int i = 0; i < dst_corners.length;) {
        pointCount += 1;
        // TODO: any idea what the heck?!
        if (i + 3 >= dst_corners.length) {
          log.warn("Unexpected size of point array.");
          break;
        }
        // Points in the object ?
        int x1 = (int) Math.round(dst_corners[i]);
        int y1 = (int) Math.round(dst_corners[i + 1]);

        // points in the video ?
        int x2 = (int) Math.round(dst_corners[i + 2]);
        int y2 = (int) Math.round(dst_corners[i + 3]);

        // int x1 = (int) Math.round(dst_corners[2 * i]);
        // int y1 = (int) Math.round(dst_corners[2 * i + 1]);
        // int x2 = (int) Math.round(dst_corners[2 * j]);
        // int y2 = (int) Math.round(dst_corners[2 * j + 1]);
        // TODO: draw a line on the new corresponding image ?
        // cvLine(correspond, cvPoint(x1, y1 + object.height()), cvPoint(x2, y2
        // + object.height()), CvScalar.WHITE, 1, 8, 0);
        cvLine(correspond, cvPoint(x1, y1), cvPoint(x2 + object.width(), y2), CvScalar.WHITE, 1, 8, 0);
        // increment the loop counter
        i += 4;
      }
      log.info("Found " + pointCount + " correlated points of interest.");
    } else {
      log.info("No Object Found in video.");
    }

    // TODO: what additional info do we get from the obj finder?
    // for (int i = 0; i < finder.ptpairs.size(); i += 2) {
    // CvPoint2D32f pt1 =
    // finder.objectKeypoints[finder.ptpairs.get(i)].pt();
    // CvPoint2D32f pt2 =
    // finder.imageKeypoints[finder.ptpairs.get(i+1)].pt();
    // cvLine(correspond, cvPointFrom32f(pt1),
    // cvPoint(Math.round(pt2.x()), Math.round(pt2.y()+object.height())),
    // CvScalar.WHITE, 1, 8, 0);
    // }
    // CanvasFrame objectFrame = new CanvasFrame("Object");
    // CanvasFrame correspondFrame = new CanvasFrame("Object Correspond");
    // correspondFrame.showImage(correspond);
    // for (int i = 0; i < finder.objectKeypoints.length; i++ ) {
    // CvSURFPoint r = finder.objectKeypoints[i];
    // CvPoint center = cvPointFrom32f(r.pt());
    // int radius = Math.round(r.size()*1.2f/9*2);
    // cvCircle(objectColor, center, radius, CvScalar.RED, 1, 8, 0);
    // }
    // objectFrame.showImage(objectColor);
    // objectFrame.waitKey();
    // objectFrame.dispose();
    // correspondFrame.dispose();

    return correspond;
  }

  private int Max(int a, int b) {
    // TODO Auto-generated method stub
    if (a > b) {
      return a;
    }
    return b;
  }

  /*
   * Set the reference object to find with the surf filter.
   */
  public void setObjectImage(IplImage image) {
    this.object = image;
    settings.setObjectImage(image);
  }

}
