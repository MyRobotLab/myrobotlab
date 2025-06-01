/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * http://docs.opencv.org/modules/imgproc/doc/feature_detection.html
 * http://stackoverflow.com/questions/19270458/cvcalcopticalflowpyrlk-not-working-as-expected
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.opencv.opencv_core.AbstractCvScalar;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_tracking.TrackerCSRT;
import org.bytedeco.opencv.opencv_tracking.TrackerKCF;
import org.bytedeco.opencv.opencv_video.Tracker;
import org.bytedeco.opencv.opencv_video.TrackerGOTURN;
import org.bytedeco.opencv.opencv_video.TrackerMIL;
/*
import org.bytedeco.opencv.opencv_tracking.Tracker;
import org.bytedeco.opencv.opencv_tracking.TrackerBoosting;
import org.bytedeco.opencv.opencv_tracking.TrackerGOTURN;
import org.bytedeco.opencv.opencv_tracking.TrackerMIL;
import org.bytedeco.opencv.opencv_tracking.TrackerMOSSE;
import org.bytedeco.opencv.opencv_tracking.TrackerMedianFlow;
import org.bytedeco.opencv.opencv_tracking.TrackerTLD;
*/
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.slf4j.Logger;

/**
 * This implements the TLD tracking code from opencv_tracking.
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterTracker extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  private final static Logger log = LoggerFactory.getLogger(OpenCVFilterTracker.class);

  // The current tracker and it's associated boundingBox
  private Tracker tracker;
  // private Rect2d boundingBox;
  private Rect boundingBox;

  // configure these to set the initial box size.
  // public int boxWidth = 224;
  // public int boxHeight = 224;

  // TODO: is there a way to dynamically adjust what this should be?! That'd be
  // cool..
  public int boxWidth = 25;
  public int boxHeight = 25;

  // TODO: i'm not sure there is really a performance difference here..
  public boolean blackAndWhite = false;
  // CSRT,GOTURN,KCF,MIL
  public String trackerType = "CSRT";

  // The current mat that is being processed.
  private Mat mat = null;

  // To hold x,y,w,h
  int[] points = new int[4];

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterTracker() {
    super();
  }

  public OpenCVFilterTracker(String name) {
    super(name);
  }

  private IplImage makeGrayScale(IplImage image) {
    IplImage imageBW = AbstractIplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    return imageBW;
  }

  @Override
  public IplImage process(IplImage image) {
    // TODO: I suspect this would be faster if we cut color first.
    // cvCutColor()

    if (blackAndWhite) {
      IplImage imageBw = makeGrayScale(image);
      // frame = converter.toFrame(imageBw);
      mat = converter.toMat(imageBw);
    } else {
      // frame = converter.toFrame(image);
      mat = converter.toMat(image);
    }
    if (boundingBox != null && tracker != null) {
      // log.info("Yes ! Bounding box : {} {} {} {} " , boundingBox.x(),
      // boundingBox.y(), boundingBox.width()
      // ,boundingBox.height());
      synchronized (tracker) {
        tracker.update(mat, boundingBox);
      }
      // boundingBox.x()
      int x0 = (boundingBox.x());
      int y0 = (boundingBox.y());
      int x1 = x0 + (boundingBox.width());
      int y1 = y0 + (boundingBox.height());
      // log.info("Drawing {} {} -- {} {}", x0,y0,x1,y1);
      cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), AbstractCvScalar.RED, 1, 1, 0);

      ArrayList<Point2df> pointsToPublish = new ArrayList<Point2df>();
      float xC = boundingBox.x() + boundingBox.width() / 2;
      float yC = boundingBox.y() + boundingBox.height() / 2;
      Point2df center = new Point2df(xC, yC);
      pointsToPublish.add(center);
      data.put("TrackingPoints", pointsToPublish);

    }

    return image;
  }

  @Override
  public void release() {
    // TODO Auto-generated method stub
    super.release();
    converter.close();
  }

  private Tracker createTracker(String trackerType) {
    // TODO: add a switch for all the other types of trackers!
    // if (trackerType.equalsIgnoreCase("Boosting")) {
    // return TrackerBoosting.create();
    // } else
    if (trackerType.equalsIgnoreCase("CSRT")) {
      TrackerCSRT tracker = TrackerCSRT.create();
      return tracker;
    } else if (trackerType.equalsIgnoreCase("GOTURN")) {
      return TrackerGOTURN.create();
    } else if (trackerType.equalsIgnoreCase("KCF")) {
      return TrackerKCF.create();
    } else

    // if (trackerType.equalsIgnoreCase("MedianFlow")) {
    // return TrackerMedianFlow.create();
    // } else
    if (trackerType.equalsIgnoreCase("MIL")) {
      return TrackerMIL.create();
    } else
    // if (trackerType.equalsIgnoreCase("MOSSE")) {
    // return TrackerMOSSE.create();
    // } else
    // if (trackerType.equalsIgnoreCase("TLD")) {
    // return TrackerTLD.create();
    // } else
    {
      // TODO: why?
      log.warn("Unknown Tracker Algorithm {} defaulting to CSRT", trackerType);
      // default to TLD..
      TrackerCSRT tracker = TrackerCSRT.create();
      return tracker;
    }

  }

  public void samplePoint(Float x, Float y) {
    samplePoint((int) (x * width), (int) (y * height));
  }

  @Override
  public void samplePoint(Integer x, Integer y) {
    // TODO: implement a state machine where you select the first corner. then
    // you select the second corner
    // that would define the size of the bounding box also.
    boundingBox = new Rect(x - boxWidth / 2, y - boxHeight / 2, boxWidth, boxHeight);
    log.info("Create bounding box for tracking x:{} y:{} w:{} h:{}", boundingBox.x(), boundingBox.y(), boundingBox.width(), boundingBox.height());
    // TODO: start tracking multiple points ?
    // the tracker will initialize on the next frame.. (I know , I know. it'd be
    // better to have the current frame and do the
    // initialization here.)
    if (tracker == null) {
      tracker = createTracker(trackerType);
    }
    log.info("Created tracker");
    // TODO: I'm worried about thread safety with the "mat" object.
    if (mat != null) {
      // TODO: what happens if we're already initalized?
      synchronized (tracker) {
        tracker.init(mat, boundingBox);
      }
      log.info("Initialized tracker");
    } else {
      log.warn("Sample point called on a null mat.");
    }
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  public int getBoxWidth() {
    return boxWidth;
  }

  public void setBoxWidth(int boxWidth) {
    this.boxWidth = boxWidth;
  }

  public int getBoxHeight() {
    return boxHeight;
  }

  public void setBoxHeight(int boxHeight) {
    this.boxHeight = boxHeight;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
