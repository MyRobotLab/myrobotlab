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
import static org.bytedeco.opencv.global.opencv_imgproc.CV_ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_ADAPTIVE_THRESH_MEAN_C;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvAdaptiveThreshold;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

// FIXME - add ui parameters
public class OpenCVFilterAdaptiveThreshold extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAdaptiveThreshold.class);

  protected String algorithm = "mean";

  protected int blockSize = 11;

  protected int param1 = 2;

  transient IplImage gray = null;

  public OpenCVFilterAdaptiveThreshold(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    gray = cvCreateImage(image.cvSize(), 8, CV_THRESH_BINARY);
  }

  @Override
  public IplImage process(IplImage image) {
    // must be gray for adaptive
    cvCvtColor(image, gray, CV_BGR2GRAY);

    if (algorithm.equals("gaussian")) {
      cvAdaptiveThreshold(gray, gray, 255, CV_ADAPTIVE_THRESH_GAUSSIAN_C, CV_THRESH_BINARY, blockSize, param1);
    } else if (algorithm.equals("mean")) {
      cvAdaptiveThreshold(gray, gray, 255, CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY, blockSize, param1);
    } else {
      log.error("unknown threshold type {}", type);
    }
    return gray;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }
}
