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

import static org.bytedeco.opencv.global.opencv_imgproc.cvFindContours;
import static org.bytedeco.opencv.global.opencv_core.cvClearMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvGetSeqElem;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.opencv.global.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.opencv.global.opencv_imgproc.cvBoundingRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFont;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;

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

public class OpenCVFilterFindContours extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFindContours.class);

  boolean useMinArea = true;

  public boolean publishPolygon = true;

  boolean useMaxArea = false;
  int minArea = 150;
  int maxArea = -1;

  boolean isMinArea;
  boolean isMaxArea;

  transient IplImage grey = null;
  transient CvSeq contourPointer = new CvSeq();
  transient CvMemStorage storage = null;

  // floor finder related
  CvPoint origin = null;

  public OpenCVFilterFindContours(String name) {
    super(name);
  }

  public OpenCVFilterFindContours() {
    super();
  }

  @Override
  public void imageChanged(IplImage image) {
    if (storage == null) {
      storage = cvCreateMemStorage(0);
    }
    grey = cvCreateImage(image.cvSize(), 8, 1);
  }

  @Override
  public IplImage process(IplImage image) {

    // FIXME 3 channel search ???
    if (image.nChannels() == 3) {
      cvCvtColor(image, grey, CV_BGR2GRAY);
    } else {
      grey = image.clone();
    }

    cvFindContours(grey, storage, contourPointer, Loader.sizeof(CvContour.class), 0, CV_CHAIN_APPROX_SIMPLE);
    CvSeq contours = contourPointer;
    ArrayList<Rectangle> list = new ArrayList<Rectangle>();

    while (contours != null && !contours.isNull()) {
      if (contours.elem_size() > 0) {

        CvRect rect = cvBoundingRect(contours, 0);

        minArea = 600;

        if (useMinArea) {
          isMinArea = (rect.width() * rect.height() > minArea) ? true : false;
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

          list.add(box);

          // log.debug("box {}", box);

          if (origin == null) {
            origin = new CvPoint(width / 2, 10 /* height ?? */);
          }

          if (publishPolygon) {
            CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours) * 0.02, 1);
            for (int i = 0; i < result.total(); i++) {
              CvPoint point = new CvPoint(cvGetSeqElem(result, i));
              // log.debug("point {}", point);
            }
          }
        }
      }
      contours = contours.h_next();
    }

    // FIXME - sources could use this too
    data.putBoundingBoxArray(list);
    cvClearMemStorage(storage);
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    List<Rectangle> boxes = data.getBoundingBoxArray();
    if (boxes != null) {
      for (Rectangle box : boxes) {
        graphics.drawOval((int) box.x, (int) box.y, (int) (box.x + box.width), (int) (box.y + box.height));
      }
      graphics.drawString(String.format("cnt %d", boxes.size()), 10, 10);
    } else {
      graphics.drawString(String.format("cnt %d", 0), 10, 10);
    }
    return image;
  }

}
