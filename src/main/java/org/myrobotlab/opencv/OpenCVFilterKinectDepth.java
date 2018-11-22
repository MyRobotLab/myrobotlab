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

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrDown;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.OpenKinectFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterKinectDepth extends OpenCVFilter {

  // useful data for the kinect is 632 X 480 - 8 pixels on the right edge are
  // not good data
  // http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectDepth.class);

  int filter = 7;
  boolean createMask = false;

  transient IplImage dst = null;
  transient IplImage src = null;
  transient IplImage mask = null;

  transient IplImage lastDepthImage = null;

  int x = 0;
  int y = 0;
  int clickCounter = 0;

  boolean displayCamera = false;

  OpenKinectFrameGrabber kinect;
  int cameraIndex = 0;

  public OpenCVFilterKinectDepth() {
    super();
    if (opencv != null) {
      cameraIndex = opencv.getCameraIndex();
    }
    kinect = new OpenKinectFrameGrabber(cameraIndex);
  }

  public OpenCVFilterKinectDepth(String name) {
    super(name);
    kinect = new OpenKinectFrameGrabber(cameraIndex);
  }

  public void setDisplayCamera(boolean b) {
    displayCamera = b;
  }

  public void createMask() {
    createMask = true;
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {

    // INFO - This filter has 2 sources !!!
    // IplImage kinectDepth = data.getKinectDepth();
    IplImage kinectDepth;
    try {
      kinectDepth = kinect.grabDepth();
      lastDepthImage = kinectDepth;
    } catch (Exception e) {
      log.error("getting grabber failed", e);
      return null;
    }

    boolean processDepth = false;
    if (kinectDepth != null && processDepth) {

      // allowing publish & fork
      if (dst == null || dst.width() != image.width() || dst.nChannels() != image.nChannels()) {
        dst = cvCreateImage(cvSize(kinectDepth.width() / 2, kinectDepth.height() / 2), kinectDepth.depth(), kinectDepth.nChannels());
      }

      cvPyrDown(kinectDepth, dst, filter); // <-- why pyramid down ? should be
                                           // left to the user
      invoke("publishDisplay", "kinectDepth", toBufferedImage(dst));// <-- if requested from the service - it will publish from this filter 
    }
    // end fork

    if (displayCamera) {
      log.info("OpenCVFilterKinect image");
      return image;
    }
    
    data.putKinect(kinectDepth, image);

    return kinectDepth;
  }

  public void samplePoint(Integer inX, Integer inY) {
    ++clickCounter;
    x = inX;
    y = inY;
    ByteBuffer buffer = lastDepthImage.createBuffer();
    int value = buffer.get(y * lastDepthImage.width() + x) & 0xFF;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
