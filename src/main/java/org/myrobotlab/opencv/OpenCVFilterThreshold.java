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
import static org.bytedeco.opencv.global.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvThreshold;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterThreshold extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterThreshold.class);
  transient IplImage gray = null;

  public float lowThreshold = 0.0f;
  public float highThreshold = 256.0f;

  public OpenCVFilterThreshold() {
    super();
  }

  public OpenCVFilterThreshold(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    gray = cvCreateImage(image.cvSize(), 8, CV_THRESH_BINARY);
  }

  @Override
  public IplImage process(IplImage image) {

    // CV_THRESH_BINARY
    // CV_THRESH_BINARY_INV
    // CV_THRESH_TRUNC
    // CV_THRESH_TOZERO
    // CV_THRESH_TOZERO_INV

    // cxcore.cvSetImageCOI(image, 1);

    // http://www710.univ-lyon1.fr/~bouakaz/OpenCV-0.9.5/docs/ref/OpenCVRef_ImageProcessing.htm
    cvThreshold(image, image, lowThreshold, highThreshold, CV_THRESH_BINARY);

    // must be gray for adaptive
    /*
     * cv.cvCvtColor( image, gray, cv.CV_BGR2GRAY );
     * cv.cvAdaptiveThreshold(gray, gray, 255, cv.CV_ADAPTIVE_THRESH_MEAN_C,
     * CV_THRESH_BINARY, 7,30);
     */
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
