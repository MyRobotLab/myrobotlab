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

/*
 *  HSV changes in OpenCV -
 *  https://code.ros.org/trac/opencv/ticket/328 H is only 1-180
 *  H <- H/2 (to fit to 0 to 255)
 *  
 *  CV_HSV2BGR_FULL uses full 0 to 255 range
 */
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_RGB2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterHsv extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHsv.class.getCanonicalName());

  transient IplImage hsv = null;
  transient IplImage hue = null;
  transient IplImage value = null;
  transient IplImage saturation = null;
  transient IplImage mask = null;

  int x = 0;
  int y = 0;
  int clickCounter = 0;
  Graphics g = null;
  String lastHexValueOfPoint = "";

  transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);

  public OpenCVFilterHsv() {
    super();
  }

  public OpenCVFilterHsv(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    hsv = IplImage.createCompatible(image);
  }

  @Override
  public IplImage process(IplImage image) {
    cvCvtColor(image, hsv, CV_RGB2HSV);
    return hsv;

  }

  public void samplePoint(Integer inX, Integer inY) {
    ++clickCounter;
    x = inX;
    y = inY;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    if (x != 0 && clickCounter % 2 == 0) {
      int clr = image.getRGB(x, y);
      lastHexValueOfPoint = Integer.toHexString(clr);
      graphics.drawString(lastHexValueOfPoint, x, y);
    }

    return image;
  }

}
