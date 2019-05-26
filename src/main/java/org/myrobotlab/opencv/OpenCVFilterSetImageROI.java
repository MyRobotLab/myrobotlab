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

import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSetImageROI extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  CvRect rect = null;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSetImageROI.class);

  public OpenCVFilterSetImageROI(String name) {
    super(name);
  }

  public OpenCVFilterSetImageROI() {
    super();
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  public void setROI(int x, int y, int width, int height) {
    rect = new CvRect(x, y, width, height);
  }

  @Override
  public IplImage process(IplImage image) {

    if (rect != null) {
      cvSetImageROI(image, rect);
    }
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (rect != null) {
      BufferedImage dest = image.getSubimage(0, 0, rect.width(), rect.height());
      return dest;
    } else {
      return image;
    }
  }

}
