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

import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_EPS;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FILLED;
import static org.bytedeco.javacpp.opencv_imgproc.cvCircle;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindCornerSubPix;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;
import static org.bytedeco.javacpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.slf4j.Logger;

/**
 *
 * @author GroG
 * 
 *         https://www.codeproject.com/Articles/840823/Object-Feature-Tracking-in-Csharp (excellent artical describing sparse
 *         optical flow)
 *
 */
public class OpenCVFilterOpticalFlow extends OpenCVFilter {

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterOpticalFlow.class);

  private static final long serialVersionUID = 1L;

  /**
   * Size of an average block for computing a derivative covariation matrix over each pixel neighborhood. See
   * cornerEigenValsAndVecs()
   */
  int blockSize = 3;

  // cvGoodFeaturesToTrack

  /**
   * default number of max corners to start with
   */
  int max = 500;

  /**
   * Output vector of detected corners.
   */
  CvPoint2D32f corners = new CvPoint2D32f(max);

  IplImage currentImg = null;

  /**
   * The parameter is ignored.
   */
  IplImage eig_image = null;

  boolean getCornerSubPix = true;

  /**
   * Free parameter of the Harris detector.
   */
  float harrisDetectorK = 0.04f;

  IplImage lastImg = null;

  /**
   * Maximum number of corners to return. If there are more corners than are found, the strongest of them is returned.
   */
  IntPointer maxCorners = new IntPointer(1).put(max);

  /**
   * Minimum possible Euclidean distance between the returned corners.
   */
  float minDistance = 5.0f;

  public ArrayList<Point2df> pointsToPublish = new ArrayList<Point2df>();

  /**
   * Parameter characterizing the minimal accepted quality of image corners. The parameter value is multiplied by the best corner
   * quality measure, which is the minimal eigenvalue (see cornerMinEigenVal() ) or the Harris function response (see cornerHarris()
   * ). The corners with the quality measure less than the product are rejected. For example, if the best corner has the quality
   * measure = 1500, and the qualityLevel=0.01 , then all the corners with the quality measure less than 15 are rejected.
   */
  float qualityLevel = 0.05f;

  /**
   * Optional region of interest. If the image is not empty (it needs to have the type CV_8UC1 and the same size as image ), it
   * specifies the region in which the corners are detected.
   */
  CvArr roi = null;

  /**
   * The parameter is ignored.
   */
  IplImage tmpImage = null;

  /**
   * useHarrisDetector – Parameter indicating whether to use a Harris detector (see cornerHarris()) or cornerMinEigenVal().
   */
  int useHarrisDetector = 0;

  /**
   * Minimum possible Euclidean distance between the returned corners.
   */
  int winSize = 15;

  public OpenCVFilterOpticalFlow() {
    super();
  }

  public OpenCVFilterOpticalFlow(String name) {
    super(name);
  }

  public void clearPoints() {

  }

  @Override
  public void imageChanged(IplImage inImage) {
    currentImg = IplImage.create(imageSize, 8, 1);
    lastImg = IplImage.create(imageSize, 8, 1);
    eig_image = cvCreateImage(imageSize, IPL_DEPTH_32F, 1);
    tmpImage = cvCreateImage(imageSize, IPL_DEPTH_32F, 1);
  }

  @Override
  public IplImage process(IplImage inImage) throws InterruptedException {

    if (channels == 3) {
      cvCvtColor(inImage, currentImg, CV_BGR2GRAY);
    } else {
      currentImg = inImage;
    }

    // TODO - "List" of images ?
    if (lastImg != null) {

      // does this "leak" ? - cannot "re-use"
      corners = new CvPoint2D32f(max);
      maxCorners = new IntPointer(1).put(max);

      cvGoodFeaturesToTrack(currentImg, eig_image, tmpImage, corners, maxCorners, qualityLevel, minDistance, roi, blockSize, useHarrisDetector, harrisDetectorK);

      if (getCornerSubPix) {
        cvFindCornerSubPix(currentImg, corners, maxCorners.get(), cvSize(winSize, winSize), cvSize(-1, -1), cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
      }

      // Call Lucas Kanade algorithm
      BytePointer featuresFound = new BytePointer(max);
      FloatPointer featureErrors = new FloatPointer(max);

      CvSize pyr_sz = cvSize(currentImg.width() + 8, lastImg.height() / 3);

      IplImage pyrA = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
      IplImage pyrB = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);

      CvPoint2D32f cornersB = new CvPoint2D32f(max);
      cvCalcOpticalFlowPyrLK(currentImg, lastImg, pyrA, pyrB, corners, cornersB, maxCorners.get(), cvSize(winSize, winSize), 5, featuresFound, featureErrors,
          cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3), 0);

      // Make an image of the results
      for (int i = 0; i < maxCorners.get(); i++) {
        if (featuresFound.get(i) == 0 || featureErrors.get(i) > 550) {
          System.out.println("Error is " + featureErrors.get(i) + "/n");
          continue;
        }
        corners.position(i);
        cornersB.position(i);
        CvPoint p0 = cvPoint(Math.round(corners.x()), Math.round(corners.y()));
        CvPoint p1 = cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y()));
        cvLine(inImage, p0, p1, CV_RGB(255, 0, 0), 2, 8, 0);
        cvCircle(inImage, p1, 3, CV_RGB(0, 0, 255), CV_FILLED, 8, 0);
        // FIXME - don't
        // tamper with out
        // image - change
        // only display :(
        // cvC
      }

      cvReleaseImage(pyrA);
      cvReleaseImage(pyrB);
    }
    // lastImg = currentImg;
    cvCopy(currentImg, lastImg);
    return inImage; // FIXME - don't tamper with out image - change only display
                    // :(
  }

  public void samplePoint(Float x, Float y) {
  }

  public void samplePoint(Integer x, Integer y) {
  }

  public void setMaxCorners(int max) {
    corners = new CvPoint2D32f(max);
    maxCorners = new IntPointer(1).put(max);
  }

  public void setQuality(float q) {
    qualityLevel = q;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}