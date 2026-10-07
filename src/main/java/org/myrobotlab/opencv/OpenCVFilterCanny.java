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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCanny;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterCanny extends OpenCVFilter {
  /**
   * Catalog metadata for the WebGui filter guide. Static so it can be read
   * without constructing the filter (constructors may start services).
   */
  public static OpenCVFilterInfo catalogInfo() {
    return new OpenCVFilterInfo("Canny",
        "Canny edge detector: finds intensity gradients and thins them into a binary edge map. Widely used as a first step for contours, lines, and shape analysis.",
        "Add the filter and tune aperture size plus low/high hysteresis thresholds. Feed the output into FindContours, HoughLines2, or Mouse. Color input is converted to gray first.",
        OpenCVFilterInfo.DEP_CORE);
  }

  @Override
  public OpenCVFilterInfo getFilterInfo() {
    return catalogInfo();
  }


  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterCanny.class);

  public int apertureSize = 5;
  public double lowThreshold = 0.0;
  public double highThreshold = 50.0;

  transient IplImage gray = null;
  transient IplImage inlines = null;

  public OpenCVFilterCanny(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) {

    if (gray == null) {
      gray = cvCreateImage(image.cvSize(), 8, 1);
    }
    if (inlines == null) {
      inlines = cvCreateImage(image.cvSize(), 8, 1);
    }

    if (image.nChannels() == 3) {
      cvCvtColor(image, gray, CV_BGR2GRAY);
    } else {
      gray = image.clone();
    }

    cvCanny(gray, inlines, lowThreshold, highThreshold, apertureSize);
    return inlines;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
