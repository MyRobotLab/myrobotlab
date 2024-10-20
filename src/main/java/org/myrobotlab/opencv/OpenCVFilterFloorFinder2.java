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
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvClearMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateMemStorage;
//import static org.bytedeco.opencv.global.opencv_core.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_core.cvGetSeqElem;
import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_core.cvScalar;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.opencv.global.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.opencv.global.opencv_imgproc.cvBoundingRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFloodFill;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFont;
import static org.bytedeco.opencv.helper.opencv_core.CV_RGB;
import static org.bytedeco.opencv.helper.opencv_imgproc.cvFindContours;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.CvContour;
//import org.bytedeco.opencv.opencv_core.CvFont;
import org.bytedeco.opencv.opencv_core.CvMemStorage;
import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.CvSeq;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;
//import static org.bytedeco.javacpp.opencv_core.cvFont;
//import static org.bytedeco.javacpp.opencv_core.cvPutText;

public class OpenCVFilterFloorFinder2 extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFloorFinder2.class);

  transient CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  boolean useMinArea = true;

  public boolean publishBoundingBox = true;
  public boolean publishPolygon = true;

  boolean useMaxArea = false;
  int minArea = 150;
  int maxArea = -1;

  int thresholdValue = 0;
  boolean addPolygon = false;

  boolean isMinArea;
  boolean isMaxArea;

  transient IplImage grey = null;
  transient IplImage dst = null;
  transient CvSeq contourPointer = new CvSeq();
  transient CvMemStorage storage = null;

  transient CvPoint startPoint = cvPoint(180, 120);
  transient CvScalar fillColor = cvScalar(255.0, 0.0, 0.0, 1.0);
  transient CvScalar lo_diff = CV_RGB(20.0, 20.0, 20.0);// cvScalar(20, 0.0,
  // 0.5, 1.0);
  transient CvScalar up_diff = CV_RGB(20.0, 20.0, 20.0);

  // floor finder related
  CvPoint origin = null;
  public List<Point2df> edgePoints;

  public OpenCVFilterFloorFinder2() {
    super();
  }

  public OpenCVFilterFloorFinder2(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    if (storage == null) {
      storage = cvCreateMemStorage(0);
    }

    grey = cvCreateImage(image.cvSize(), 8, 1);
    // display = cvCreateImage(cvGetSize(frame), 8, 3);

    startPoint = cvPoint(image.width() / 2, image.height() - 4);
  }

  @Override
  public IplImage process(IplImage image) {

    // FIXME 3 channel search ???
    if (image.nChannels() == 3) {
      cvCvtColor(image, grey, CV_BGR2GRAY);
      // get HSV !
    } else {
      grey = image.clone();
    }

    // ============ floor finder begin ================
    fillColor = cvScalar(255.0, 255.0, 255.0, 1.0);

    lo_diff = CV_RGB(1, 12, 13);// cvScalar(20, 0.0, 0.5, 1.0);
    up_diff = CV_RGB(1, 12, 13);

    cvFloodFill(image, startPoint, fillColor, lo_diff, up_diff, null, 4, null);

    fillColor = cvScalar(0.0, 255.0, 0.0, 1.0);
    // ============ floor finder end ================

    cvFindContours(grey, storage, contourPointer, Loader.sizeof(CvContour.class), 0, CV_CHAIN_APPROX_SIMPLE);
    CvSeq contours = contourPointer;
    boxes = new ArrayList<Rectangle>();

    while (contours != null && !contours.isNull()) {
      if (contours.elem_size() > 0) { // TODO - limit here for
        // "TOOOO MANY !!!!"

        CvRect rect = cvBoundingRect(contours, 0);

        minArea = 600;

        // find all the avg color of each polygon
        // cxcore.cvZero(polyMask);
        // cvDrawContours(polyMask, points, CvScalar.WHITE,
        // CvScalar.BLACK, -1, cxcore.CV_FILLED, CV_AA);

        // publish polygons
        // CvScalar avg = cxcore.cvAvg(image, polyMask); - good idea -
        // but not implemented

        // log.error("{}", rect);
        // size filter
        if (useMinArea) {
          isMinArea = (rect.width() * rect.height() > minArea) ? true : false;
          // log.error("{} {}", isMinArea, rect.width() *
          // rect.height());
        } else {
          useMinArea = true;
        }

        if (useMaxArea) {
          isMaxArea = (rect.width() * rect.height() < maxArea) ? true : false;
        } else {
          isMaxArea = true;
        }

        if (isMinArea && isMaxArea) {

          Rectangle box = new Rectangle();

          box.x = rect.x();
          box.y = rect.y();
          box.width = rect.width();
          box.height = rect.height();

          boxes.add(box);

          // log.debug("box {}", box);

          if (origin == null) {
            origin = new CvPoint();
            origin.x(width / 2);
            origin.y(10 /* height?? */);
          }

          if (publishPolygon) {
            // CvSeq points = cvApproxPoly(contour,
            // Loader.sizeof(CvContour.class), cvStorage, CV_POLY_APPROX_DP,
            // cvContourPerimeter(contour) * 0.02, 1);
            // CvSeq result = cvApproxPoly(contours,
            // Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
            // cvContourPerimeter(contours)*0.02, 0)
            CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours) * 0.02, 1);
            for (int i = 0; i < result.total(); i++) {
              CvPoint point = new CvPoint(cvGetSeqElem(result, i));
              // log.debug("point {}", point);
            }
          }
          // Polygon polygon = new Polygon();
          // iterate through points - points.total() build awt Polygon
          // polygons.add(polygon);

          // polygons.add(new Polygon(rect, null,
          // (cvCheckContourConvexity(points) == 1) ? true : false,
          // cvPoint(rect.x() + rect.width() / 2, rect.y() +
          // rect.height() / 2), points.total()));

          /*
           * WRONG FIXME - post processing should be done in Java on the
           * buffered image !!!!S cvPutText(display, " " + points.total() + " "
           * + (rect.x() + rect.width() / 2) + "," + (rect.y() + rect.height() /
           * 2) + " " + rect.width() + "x" + rect.height() + "=" + (rect.width()
           * * rect.height()) + " " + " " + cvCheckContourConvexity(points),
           * cvPoint(rect.x() + rect.width() / 2, rect.y()), font,
           * CvScalar.WHITE);
           */
        }

      }
      contours = contours.h_next();
    }

    // FIXME - sources could use this too
    data.put("Contours", boxes);
    cvClearMemStorage(storage);
    return image;
  }

  List<Rectangle> boxes = new ArrayList<>();

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    for (Rectangle box : boxes) {

      int x = (int) box.x;
      int y = (int) box.y;
      int w = x + (int) box.width;
      int h = y + (int) box.height;
      graphics.drawRect(x, y, w, h);
    }

    graphics.drawString(String.format("cnt %d", boxes.size()), 10, 10);
    return image;
  }

}
