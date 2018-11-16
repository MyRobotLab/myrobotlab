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

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvZero;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_flann.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_ml.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_shape.*;
import static org.bytedeco.javacpp.opencv_stitching.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_videostab.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;
import org.myrobotlab.service.Runtime;

public class OpenCVFilterAddMask extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAddMask.class);
  Mat mask = null;
  public String maskName;

  transient IplImage dst = null;
  transient IplImage negativeImage = null;

  public OpenCVFilterAddMask(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    dst = null;
  }

  @Override
  public IplImage process(IplImage background) throws InterruptedException {
    if (mask != null) {
      /*
      // Convert Mat to float data type
      mask.convertTo(mask, CV_32FC3);
      background.convertTo(background, CV_32FC3);
      
      // Normalize the alpha mask to keep intensity between 0 and 1
      alpha.convertTo(alpha, CV_32FC3, 1.0/255); // 
   
      // Storage for output image
      Mat ouImage = Mat::zeros(mask.size(), mask.type());
   
      // Multiply the mask with the alpha matte
      multiply(alpha, mask, mask); 
   
      // Multiply the background with ( 1 - alpha )
      multiply(Scalar::all(1.0)-alpha, background, background); 
   
      // Add the masked mask and background.
      add(mask, background, ouImage); 
      */
       
    }
    
    return background;
  }
  
  public void test() {
    if (opencv == null) {
      error("requires a opencv instance - please addFilter this filter to test");
    }
    // setMask(TEST_TRANSPARENT_FILE_PNG);
  }

  public void setMask(String maskName) {
    this.maskName = maskName;
    mask = imread(maskName);
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

  public static void main(String[] args) {
    try {
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      OpenCVFilterAddMask mask = new OpenCVFilterAddMask("mask");
      mask.setMask("src/test/resources/OpenCV/transparent-bubble.jpg");
      cv.addFilter(mask);
      cv.capture();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
