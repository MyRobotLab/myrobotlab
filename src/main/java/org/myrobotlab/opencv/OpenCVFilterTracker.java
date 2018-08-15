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
 * http://docs.opencv.org/modules/imgproc/doc/feature_detection.html
 * http://stackoverflow.com/questions/19270458/cvcalcopticalflowpyrlk-not-working-as-expected
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_tracking.TrackerBoosting;
import static org.bytedeco.javacpp.opencv_tracking.TrackerCSRT;
import static org.bytedeco.javacpp.opencv_tracking.TrackerGOTURN;
import static org.bytedeco.javacpp.opencv_tracking.TrackerKCF;
import static org.bytedeco.javacpp.opencv_tracking.TrackerMedianFlow;
import static org.bytedeco.javacpp.opencv_tracking.TrackerMIL;
import static org.bytedeco.javacpp.opencv_tracking.TrackerMOSSE;
import static org.bytedeco.javacpp.opencv_tracking.TrackerTLD;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect2d;
import org.bytedeco.javacpp.opencv_tracking.Tracker;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import java.util.ArrayList;
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
  transient private static final OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  transient private static final OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  // The current tracker and it's associated boundingBox
  private Tracker tracker;
  private Rect2d boundingBox;
  
  // configure these to set the initial box size.
  public int boxWidth = 10;
  public int boxHeight = 10;
  // Boosting
  //  CSRT
  //  GOTURN
  //  KCF
  //  MedianFlow
  //  MIL
  //  MOSSE
  //  TLD
  public String trackerType = "TLD";
  
  public OpenCVFilterTracker() {
    super();
  }

  public OpenCVFilterTracker(String name) {
    super(name);
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {
    Frame frame = converterToIpl.convert(image);
    Mat mat = converterToMat.convert(frame);
    if (boundingBox != null && tracker == null) {
      tracker = createTracker(trackerType);
      // log.info("Init tracker");
      tracker.init(mat,boundingBox);
      log.info("Tracker initialized");
    }
    if (boundingBox != null && tracker != null) {
      // log.info("Yes ! Bounding box : {} {} {} {} " , boundingBox.x(), boundingBox.y(), boundingBox.width() ,boundingBox.height());
      tracker.update(mat, boundingBox);
      // boundingBox.x()
      int x0 = (int) (boundingBox.x());
      int y0 = (int) (boundingBox.y());
      int x1 = x0 + (int) (boundingBox.width() );
      int y1 = y0 + (int) (boundingBox.height() );
      // log.info("Drawing {} {} -- {} {}", x0,y0,x1,y1);
      cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), CvScalar.RED, 1, 1, 0);
      if (publishData) {
        ArrayList<Point2Df> pointsToPublish = new ArrayList<Point2Df>();
        float xC = (float) (boundingBox.x() + boundingBox.width()/2);
        float yC = (float) (boundingBox.y() + boundingBox.height()/2);
        Point2Df center = new Point2Df(xC, yC);
        pointsToPublish.add(center);
        data.set(pointsToPublish);
      }
    }
    return image;
  }

  private Tracker createTracker(String trackerType) {
    // TODO: add a switch for all the other types of trackers!
    if (trackerType.equalsIgnoreCase("Boosting")) {
      return TrackerBoosting.create();
    } else if (trackerType.equalsIgnoreCase("CSRT")) {
      return TrackerCSRT.create();
    } else if (trackerType.equalsIgnoreCase("GOTURN")) {
      return TrackerGOTURN.create();
    } else if (trackerType.equalsIgnoreCase("KCF")) {
      return TrackerKCF.create();
    } else if (trackerType.equalsIgnoreCase("MedianFlow")) {
      return TrackerMedianFlow.create();
    } else if (trackerType.equalsIgnoreCase("MIL")) {
      return TrackerMIL.create();
    } else if (trackerType.equalsIgnoreCase("MOSSE")) {
      return TrackerMOSSE.create();
    } else if (trackerType.equalsIgnoreCase("TLD")) {
      return TrackerTLD.create();  
    } else {
      log.warn("Unknown Tracker Algorithm {} defaulting to TLD", trackerType);
      // default to TLD..
      return TrackerTLD.create();
    }
    
  }

  public void samplePoint(Float x, Float y) {
    samplePoint((int) (x * width), (int) (y * height));
  }

  public void samplePoint(Integer x, Integer y) {
    boundingBox = new Rect2d(x-boxWidth, y-boxHeight, boxWidth*2, boxHeight*2);
    log.info("Create bounding box for tracking x:{} y:{} w:{} h:{}" , boundingBox.x() , boundingBox.y(), boundingBox.width(), boundingBox.height());
    //TODO:  the tracker will initialize on the next frame..  (I know , I know. it'd be better to have the current frame and do the initialization here.
    tracker = null;
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

}
