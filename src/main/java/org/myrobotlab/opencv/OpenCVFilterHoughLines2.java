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

import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvGetSeqElem;
import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_HOUGH_PROBABILISTIC;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCanny;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawLine;
import static org.bytedeco.opencv.global.opencv_imgproc.cvHoughLines2;
import static org.bytedeco.opencv.helper.opencv_core.CV_RGB;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.AbstractCvMemStorage;
import org.bytedeco.opencv.opencv_core.CvMemStorage;
import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.CvSeq;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterHoughLines2 extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHoughLines2.class.getCanonicalName());

  transient IplImage gray = null;

  double lowThreshold = 0.0;
  double highThreshold = 50.0;
  int apertureSize = 5;
  transient Pointer storage = null;
  transient IplImage inlines = null;

  transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);

  transient CvPoint p0 = cvPoint(0, 0);

  transient CvPoint p1 = cvPoint(0, 0);

  public OpenCVFilterHoughLines2(String name) {
    super(name);
  }

  public OpenCVFilterHoughLines2() {
    super();
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) {

    if (image == null) {
      log.error("image is null");
    }

    if (gray == null) {
      gray = cvCreateImage(image.cvSize(), 8, 1);
    }

    if (storage == null) {
      storage = AbstractCvMemStorage.create();
    }

    if (inlines == null) {
      inlines = cvCreateImage(image.cvSize(), 8, 1);
    }

    if (image.nChannels() > 1) {
      cvCvtColor(image, gray, CV_BGR2GRAY);
    } else {
      gray = image.clone();
    }

    // TODO - use named inputs and outputs
    lowThreshold = 5.0;
    highThreshold = 400.0;
    apertureSize = 3;
    cvCanny(gray, inlines, lowThreshold, highThreshold, apertureSize);

    // http://www.aishack.in/2010/04/hough-transform-in-opencv/ -
    // explanation of hough transform parameters

    // CV_HOUGH_MULTI_SCALE || CV_HOUGH_STANDARD
    CvSeq lines = cvHoughLines2(inlines, storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 10);// ,
    // 40,
    // 10);

    // Pointer p = null;
    // cvHoughLines2(inlines, p, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 10,
    // 40, 10);

    for (int i = 0; i < lines.total(); i++) {

      Pointer line = cvGetSeqElem(lines, i);
      CvPoint pt1 = new CvPoint(line);
      pt1.position(0);
      CvPoint pt2 = new CvPoint(line);
      pt2.position(1);

      log.info("Line spotted: ");
      log.info("\t pt1: " + pt1);
      log.info("\t pt2: " + pt2);
      // cvLine(image, pt1, pt2, CV_RGB(255, 0, 0), 3, CV_AA, 0); // draw
      // the segment on the image
      cvDrawLine(image, p0, p1, CV_RGB(255, 255, 255), 2, 8, 0);

      try {
        // close these resources?!
        pt1.close();
        pt2.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

    // cxcore.cvPutText(image, "x", cvPoint(10, 14), font, CvScalar.WHITE);

    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
