/**
 *                    
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

// TODO - have no published OpenCV items - move all to java.awt objects - no native code necessary in viewer

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvInRangeS;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvZero;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvBoundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvCheckContourConvexity;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrDown;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.image.KinectImageNode;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterKinectDepthMask extends OpenCVFilter {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectDepthMask.class.getCanonicalName());

  transient IplImage kinectDepth = null;
  transient IplImage ktemp = null;
  transient IplImage ktemp2 = null;
  transient IplImage black = null;
  transient IplImage itemp = null;
  transient IplImage itemp2 = null;
  transient IplImage gray = null;
  transient IplImage mask = null;

  // Make memory - do not optimize - will only lead to bugs
  // the correct optimization would be NOT TO PUBLISH
  // public ArrayList<KinectImageNode> nodes = new
  // ArrayList<KinectImageNode>();
  public ArrayList<KinectImageNode> nodes = null;

  String imageKey = "kinectDepth";

  int mWidth = 0;
  int mHeight = 0;
  int mX = 0;
  int mY = 0;

  int scale = 2;

  // countours
  CvSeq contourPointer = new CvSeq();

  int minArea = 30;
  int maxArea = 0;
  boolean isMinArea = true;
  boolean isMaxArea = true;

  public boolean drawBoundingBoxes = false;
  public boolean publishNodes = false;

  transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);

  // cvDrawRect has to have 2 points - no cvDrawRect can't draw a cvRect ???
  // http://code.google.com/p/opencvx/ - apparently - I'm not the only one who
  // thinks this is silly
  // http://opencvx.googlecode.com/svn/trunk/cvdrawrectangle.h
  transient CvMemStorage cvStorage = null;

  transient CvPoint p0 = cvPoint(0, 0);
  transient CvPoint p1 = cvPoint(0, 0);

  public OpenCVFilterKinectDepthMask() {
    super();
  }

  public OpenCVFilterKinectDepthMask(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {

    /*
     * 
     * 0 - is about 23 " 30000 - is about 6' There is a blackzone in between -
     * (sign issue?)
     * 
     * CvScalar min = cvScalar( 30000, 0.0, 0.0, 0.0); CvScalar max =
     * cvScalar(100000, 0.0, 0.0, 0.0);
     */
    if (cvStorage == null) {
      cvStorage = cvCreateMemStorage(0);
    }

    // TODO - clean up - remove input parameters? only use storage?
    if (imageKey != null) {
      // TODO: validate what this is doing?
      kinectDepth = data.get(OpenCV.SOURCE_KINECT_DEPTH);
    } else {
      kinectDepth = image;
    }

    // cv Pyramid Down

    if (mask == null) // || image.width() != mask.width()
    {
      mask = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 1);
      ktemp = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 16, 1);
      ktemp2 = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 1);
      black = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 1);
      itemp = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 3);
      itemp2 = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 3);
      gray = cvCreateImage(cvSize(kinectDepth.width() / scale, kinectDepth.height() / scale), 8, 1);
    }
    cvZero(black);
    cvZero(mask);
    cvZero(itemp2);

    cvPyrDown(image, itemp, 7);
    cvPyrDown(kinectDepth, ktemp, 7);

    // cvReshape(arg0, arg1, arg2, arg3);
    // cvConvertScale(ktemp, ktemp2, 0.009, 0);

    CvScalar min = cvScalar(0, 0.0, 0.0, 0.0);
    // CvScalar max = cvScalar(30000, 0.0, 0.0, 0.0);
    CvScalar max = cvScalar(10000, 0.0, 0.0, 0.0);

    cvInRangeS(ktemp, min, max, mask);

    int offsetX = 0;
    int offsetY = 0;
    mWidth = 607 / scale - offsetX;
    mHeight = 460 / scale - offsetY;
    mX = 25 / scale + offsetX;
    mY = 20 / scale + offsetY;

    // shifting mask 32 down and to the left 25 x 25 y
    cvSetImageROI(mask, cvRect(mX, 0, mWidth, mHeight)); // 615-8 = to
    // remove right
    // hand band
    cvSetImageROI(black, cvRect(0, mY, mWidth, mHeight));
    cvCopy(mask, black);
    cvResetImageROI(mask);
    cvResetImageROI(black);
    cvCopy(itemp, itemp2, black);

    invoke("publishDisplay", "input", OpenCV.IplImageToBufferedImage(itemp));
    invoke("publishDisplay", "kinectDepth", OpenCV.IplImageToBufferedImage(ktemp));
    invoke("publishDisplay", "kinectMask", OpenCV.IplImageToBufferedImage(mask));

    // TODO - publish KinectImageNode ArrayList
    // find contours ---- begin ------------------------------------
    CvSeq contour = contourPointer;
    // int cnt = 0;

    // cvFindContours(mask, cvStorage, contourPointer,
    // Loader.sizeof(CvContour.class), 0 ,CV_CHAIN_APPROX_SIMPLE); NOT
    // CORRECTED
    if (itemp2.nChannels() == 3) {
      cvCvtColor(itemp2, gray, CV_BGR2GRAY);
    } else {
      gray = itemp2.clone();
    }

    cvFindContours(gray, cvStorage, contourPointer, Loader.sizeof(CvContour.class), 0, CV_CHAIN_APPROX_SIMPLE);

    // new cvFindContours(gray, storage, contourPointer,
    // Loader.sizeof(CvContour.class), CV_RETR_LIST,
    // CV_CHAIN_APPROX_SIMPLE);
    // old cvFindContours(gray, storage, contourPointer, sizeofCvContour, 0
    // ,CV_CHAIN_APPROX_SIMPLE);

    // log.error("getStructure");

    if (publishNodes) {
      minArea = 1500;
      nodes = new ArrayList<KinectImageNode>();
      while (contour != null && !contour.isNull()) {
        if (contour.elem_size() > 0) { // TODO - limit here for
          // "TOOOO MANY !!!!"

          CvRect rect = cvBoundingRect(contour, 0);

          // size filter
          if (minArea > 0 && (rect.width() * rect.height()) < minArea) {
            isMinArea = false;
          }

          if (maxArea > 0) {
            isMaxArea = false;
          }

          if (isMinArea && isMaxArea) {
            CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), cvStorage, CV_POLY_APPROX_DP, cvContourPerimeter(contour) * 0.02, 1);
            // FIXME - do the work of changing all data types so
            // that the only
            // published material is java.awt object no OpenCV
            // objects
            KinectImageNode node = new KinectImageNode();
            // node.cameraFrame = image.getBufferedImage();
            node.cvCameraFrame = itemp.clone(); // pyramid down
            // version
            node.cvBoundingBox = new CvRect(rect);
            node.boundingBox = new Rectangle(rect.x(), rect.y(), rect.width(), rect.height());

            // convert camera frame
            // FIXME node.cameraFrame = OpenCV.publishFrame("",
            // node.cvCameraFrame.getBufferedImage());

            // cropped
            cvSetImageROI(node.cvCameraFrame, node.cvBoundingBox);
            node.cvCropped = cvCreateImage(cvSize(node.cvBoundingBox.width(), node.cvBoundingBox.height()), 8, 3);
            cvCopy(node.cvCameraFrame, node.cvCropped);
            cvResetImageROI(node.cvCameraFrame);
            // FIXME node.cropped = OpenCV.publishFrame("",
            // node.cvCropped.getBufferedImage());

            log.error("{}", rect);
            log.error("{}", node.cvBoundingBox);
            log.error("{}", node.boundingBox);
            nodes.add(node);

            if (drawBoundingBoxes) {
              cvPutText(itemp2, " " + points.total() + " " + (rect.x() + rect.width() / 2) + "," + (rect.y() + rect.height() / 2) + " " + rect.width() + "x" + rect.height() + "="
                  + (rect.width() * rect.height()) + " " + " " + cvCheckContourConvexity(points), cvPoint(rect.x() + rect.width() / 2, rect.y()), font, CvScalar.WHITE);
              p0.x(rect.x());
              p0.y(rect.y());
              p1.x(rect.x() + rect.width());
              p1.y(rect.y() + rect.height());
              cvDrawRect(itemp2, p0, p1, CvScalar.RED, 1, 8, 0);
            }
          }

          isMinArea = true;
          isMaxArea = true;

          // ++cnt;
        }
        contour = contour.h_next();
      } // while (contour != null && !contour.isNull())
      invoke("publish", (Object) nodes);
    } // if (publishNodes)

    cvClearMemStorage(cvStorage);

    // find contours ---- end --------------------------------------

    return itemp2;

  }

}
