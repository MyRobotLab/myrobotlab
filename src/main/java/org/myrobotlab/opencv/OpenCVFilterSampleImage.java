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

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_core.cvScalar;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSampleImage extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSampleImage.class.getCanonicalName());

  transient IplImage buffer = null;

  IplImage fileImage = null;
  Graphics2D graphics = null;

  /*
   * http://www.csgnetwork.com/csgcolorsel4.html
   * 
   * 255, 0, 255, 1 = fusia = HSV 255 255 150 254, 0, 0, 1 = blue = HSV 255 255
   * 120 0, 255, 0, 1 = green = HSV 255 255 60 0, 255, 255, 1 = yellow = HSV 255
   * 255 30 254, 254, 0, 1 = blgrn = HSV 255 255 90 0, 0, 255, 1 = red = HSV 255
   * 255 0 255, 255, 255, 1 = white = HSV 255 0 0 0, 0, 0, 1 = black = HSV 0 0 0
   * 128, 128, 128, 1 = grey = HSV 128 0 0 0, 128, 128, 1 = olive = HSV 128 255
   * 30 0, 0, 128, 1 = maroon = HSV 128 255 0 128, 0, 128, 1 = purple = HSV 128
   * 255 150 128, 0, 0, 1 = blue = HSV 128 255 150 (navy)
   */

  public OpenCVFilterSampleImage() {
    super();
  }

  public OpenCVFilterSampleImage(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  public void addImage(String path) {
    fileImage = loadImage(path);
  }

  @Override
  public IplImage process(IplImage image) {
    // cvCvtColor(image, buffer, CV_BGR2HSV);
    // avg = cxcore.cvAvg(image, null);
    // cvFillPoly( image, pt, arr, 2, random_color(&rng), line_type, 0 );

    // black background
    CvPoint p0 = cvPoint(0, 0);
    CvPoint p1 = cvPoint(image.width(), image.height());

    CvScalar fillColor = cvScalar(0.0, 0.0, 0.0, 1.0);
    cvDrawRect(image, p0, p1, fillColor, 240, 1, 0);

    p0 = cvPoint(110, 80);
    p1 = cvPoint(150, 130);

    fillColor = cvScalar(0.0, 256.0, 0.0, 0.0);
    cvDrawRect(image, cvPoint(160, 120), cvPoint(164, 124), fillColor, 2, 1, 0);

    /*
     * cvDrawRect(image, p0, p1, fillColor, 2, 1, 0); fillColor =
     * cvScalar(130.0, 40.0, 120.0, 1.0); cvDrawRect(image, new
     * CvPoint(158,140), new CvPoint(220,160), fillColor, 2, 1, 0); fillColor =
     * cvScalar(160.0, 140.0, 20.0, 1.0); cvDrawRect(image, new CvPoint(20,200),
     * new CvPoint(40,230), fillColor, 2, 1, 0);
     */
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
