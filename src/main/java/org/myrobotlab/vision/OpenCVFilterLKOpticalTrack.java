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

package org.myrobotlab.vision;

import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_EPS;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;
import static org.bytedeco.javacpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.util.ArrayList;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

public class OpenCVFilterLKOpticalTrack extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterLKOpticalTrack.class);

  private static final int maxPointCount = 30;

  // external modifiers
  public boolean addRemovePoint = false;
  public boolean clearPoints = false;
  public boolean needTrackingPoints = false;
  public Point2Df samplePoint = new Point2Df();

  int validCorners = 0;

  // start //////////////////////

  // transient IntPointer count = new IntPointer(0).put(maxPointCount);
  transient IntPointer count = new IntPointer(0).put(0);

  transient IplImage imgA = null;
  transient IplImage imgB = null;

  transient IplImage pyrA = null;
  transient IplImage pyrB = null;

  int win_size = 15;

  // Get the features for tracking
  transient IplImage eig = null;
  transient IplImage tmp = null;

  transient BytePointer features_found = new BytePointer(maxPointCount);
  transient FloatPointer feature_errors = new FloatPointer(maxPointCount);

  transient CvPoint2D32f cornersA = new CvPoint2D32f(maxPointCount);
  transient CvPoint2D32f cornersB = new CvPoint2D32f(maxPointCount);
  transient CvPoint2D32f cornersC = new CvPoint2D32f(maxPointCount);

  transient CvArr mask = null;

  public ArrayList<Point2Df> pointsToPublish = new ArrayList<Point2Df>();

  public OpenCVFilterLKOpticalTrack() {
    super();
  }

  public OpenCVFilterLKOpticalTrack(String name) {
    super(name);
  }

  public void clearPoints() {
    clearPoints = true;
  }

  @Override
  public IplImage display(IplImage frame, VisionData data) {

    // Make an image of the results
    // for (int i = 0; i < count.get(); i++) {
    for (int i = 0; i < count.get(); i++) {
      cornersA.position(i);
      cornersB.position(i);
      cornersC.position(i);

      features_found.position(i);
      feature_errors.position(i);

      if (features_found.get() == 0 || feature_errors.get() > 550) {
        continue;
      }

      // line from previous frame point to current frame point
      CvPoint p0 = cvPoint(Math.round(cornersC.x()), Math.round(cornersC.y()));
      CvPoint p1 = cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y()));
      cvLine(frame, p0, p1, CV_RGB(255, 0, 0), 2, 8, 0);
    }
    // reset internal position
    cornersA.position(0);
    cornersB.position(0);
    cornersC.position(0);
    features_found.position(0);
    feature_errors.position(0);
    return frame;
  }

  @Override
  public void imageChanged(IplImage image) {

    eig = IplImage.create(imageSize, IPL_DEPTH_32F, 1);
    tmp = IplImage.create(imageSize, IPL_DEPTH_32F, 1);

    imgB = IplImage.create(imageSize, 8, 1);
    imgA = IplImage.create(imageSize, 8, 1);

    if (channels == 3) {
      cvCvtColor(image, imgB, CV_BGR2GRAY);
      cvCopy(imgB, imgA);
    }

    cornersA = new CvPoint2D32f(maxPointCount);
    cornersB = new CvPoint2D32f(maxPointCount);
    cornersC = new CvPoint2D32f(maxPointCount);

    // Call Lucas Kanade algorithm
    features_found = new BytePointer(maxPointCount);
    feature_errors = new FloatPointer(maxPointCount);

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    if (channels == 3) {
      cvCvtColor(image, imgB, CV_BGR2GRAY);
    } else {
      imgB = image;
    }

    if (clearPoints) {
      pointsToPublish.clear();
      count.put(0);
      clearPoints = false;
    }

    if (addRemovePoint && count.get() < maxPointCount) {
      cornersA.position(count.get()).x(samplePoint.x);
      cornersA.position(count.get()).y(samplePoint.y);
      count.put(count.get() + 1);
      addRemovePoint = false;
    }

    if (needTrackingPoints) {
      count.put(30);
      cvGoodFeaturesToTrack(imgA, eig, tmp, cornersA, count, 0.05, 5.0, mask, 3, 0, 0.04);
      // cvFindCornerSubPix(imgA, points, count.get(), cvSize(win_size,
      // win_size), cvSize(-1, -1), cvTermCriteria(CV_TERMCRIT_ITER |
      // CV_TERMCRIT_EPS, 20, 0.03));

      needTrackingPoints = false;
    }

    // http://docs.opencv.org/modules/video/doc/motion_analysis_and_object_tracking.html#void
    // calcOpticalFlowPyrLK(InputArray prevImg, InputArray nextImg,
    // InputArray prevPts, InputOutputArray nextPts, OutputArray status,
    // OutputArray err, Size winSize, int maxLevel, TermCriteria criteria,
    // int flags, double minEigThreshold)
    cvCalcOpticalFlowPyrLK(imgA, imgB, pyrA, pyrB, cornersA, cornersB, count.get(), cvSize(win_size, win_size), 5, features_found, feature_errors,
        cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3), 0);

    StringBuffer ff = new StringBuffer();
    StringBuffer fe = new StringBuffer();
    StringBuffer cA = new StringBuffer();
    StringBuffer cB = new StringBuffer();

    validCorners = 0;
    pointsToPublish = new ArrayList<Point2Df>();

    // shift newly calculated corner flows
    for (int i = 0; i < count.get(); i++) {
      features_found.position(i);
      feature_errors.position(i);
      cornersA.position(i);
      cornersB.position(i);
      cornersC.position(i);

      // debugging
      ff.append(features_found.get());
      fe.append(feature_errors.get());
      cA.append(String.format("(%f,%f)", cornersA.x(), cornersA.y()));
      float x = cornersB.x();
      float y = cornersB.y();
      cB.append(String.format("(%f,%f)", x, y));

      // if in eror - don't bother processing
      if (features_found.get() == 0 || feature_errors.get() > 550) {
        // System.out.println("found " + features_found.get() +
        // " error " + feature_errors.get());
        continue;
      }

      if (useFloatValues) {
        pointsToPublish.add(new Point2Df(x / width, y / height));
      } else {
        pointsToPublish.add(new Point2Df(x, y));
      }

      ++validCorners;
      // putting new points in previous buffer
      // PROBABLY WRONG !!!
      // refer to ->
      // http://stackoverflow.com/questions/9344503/equivalent-of-opencv-statement-in-java-using-javacv
      // FloatPointer p = new FloatPointer(cvGetSeqElem(circles, i));

      // we want to save previous for display to show the delta
      cornersC.put(cornersA.x(), cornersA.y());
      // we need to take the latest corners and move them to our next
      // previous
      cornersA.put(cornersB.x(), cornersB.y());
      // cornersA.put(cornersB.get(i), i);
      // cvLine(imgC, p0, p1, CV_RGB(255, 0, 0), 2, 8, 0);
    } // iterated through points

    if (publishData) {
      data.set(pointsToPublish);
    }

    log.info(String.format("MAX_POINT_COUNT %d", maxPointCount));
    log.info(String.format("count %d", count.get()));
    log.info(String.format("features_found %s", ff.toString()));
    log.info(String.format("feature_errors %s", fe.toString()));
    log.info(String.format("cA %s", cA.toString()));
    log.info(String.format("cB %s", cB.toString()));

    log.info(String.format("valid coners %d", validCorners));

    // swap
    // TODO - release what imgA pointed to?
    // cvCopy(imgA, imgB);
    cvCopy(imgB, imgA);

    // reset internal position
    cornersA.position(0);
    cornersB.position(0);
    cornersC.position(0);
    feature_errors.position(0);
    features_found.position(0);

    return image;
  }

  public void samplePoint(Float x, Float y) {
    samplePoint((int) (x * width), (int) (y * height));
  }

  public void samplePoint(Integer x, Integer y) {
    if (count.get() < maxPointCount) {
      samplePoint.x = x;
      samplePoint.y = y;
      addRemovePoint = true;
    } else {
      clearPoints();
    }
  }

}
