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

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrDown;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
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

  transient IplImage lastDepthImage = null;

  double minX = 0;
  double maxX = 65535;
  double minY = 0.0;
  double maxY = 1.0;

  int x = 0;
  int y = 0;
  int clickCounter = 0;
  int selectedDepthValue;

  /**
   * use depth as return image
   */
  boolean useDepth = true;

  /**
   * default return colored 3x256(rgb) depth / false is a lossy 1x(256) grey scale
   */
  boolean useColor = true;

  public OpenKinectFrameGrabber getGrabber() {

    OpenKinectFrameGrabber kinect = null;

    if (kinect == null && opencv != null) {
      try {
        FrameGrabber grabber = opencv.getGrabber();
        if (!grabber.getClass().equals(OpenKinectFrameGrabber.class)) {
          log.error("KinectDepth filter requires OpenKinectFrameGrabber, please select Kinect for grabber type");
          return null;
        }
        kinect = (OpenKinectFrameGrabber) grabber;
        return kinect;
      } catch (Throwable e) {
        log.error("could not get framegrabber", e);
      }
    }
    return null;
  }

  public OpenCVFilterKinectDepth(String name) {
    super(name);
  }

  public void useDepth(boolean b) {
    useDepth = b;
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {

    OpenKinectFrameGrabber kinect = getGrabber();
    if (kinect == null) {
      return image;
    }

    IplImage kinectDepth;
    try {
      if (useDepth) {
        kinectDepth = kinect.grabDepth();
        lastDepthImage = kinectDepth;
        data.putKinect(kinectDepth, image);
        
        if (useColor) {
          kinectDepth = color(kinectDepth);
        }
        return kinectDepth;
      }
    } catch (Exception e) {
      log.error("kinect grabber failed", e);
    }

    return image;
  }

  public IplImage color(IplImage depth) throws InterruptedException {

    // 256 grey channels is not enough for kinect
    // color (3x256 channels) is enough
    IplImage color = IplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3); 

    ByteBuffer colorBuffer = color.getByteBuffer();
    // it may be deprecated but the "new" function .asByteBuffer() does not
    // return all data
    ByteBuffer depthBuffer = depth.getByteBuffer();

    int depthBytesPerChannel = lastDepthImage.depth() / 8;

    // iterate through the depth bytes bytes and convert to HSV / RGB format
    // map depth gray (0,65535) => 3 x (0,255) HSV :P
    for (int y = 0; y < depth.height(); y++) { // 480
      for (int x = 0; x < depth.width(); x++) { // 640
        int depthIndex = y * depth.widthStep() + x * depth.nChannels() * depthBytesPerChannel;
        int colorIndex = y * color.widthStep() + x * color.nChannels();

        // Used to read the pixel value - the 0xFF is needed to cast from
        // an unsigned byte to an int.
        // int value = depthBuffer.get(depthIndex);// << 8 & 0xFF +
        // buffer.get(depthIndex+1)& 0xFF;
        // this is 16 bit depth - I switched the MSB !!!!
        int value = (depthBuffer.get(depthIndex + 1) & 0xFF) << 8 | (depthBuffer.get(depthIndex) & 0xFF);
        double hsv = minY + ((value - minX) * (maxY - minY)) / (maxX - minX);

        Color c = Color.getHSBColor((float) hsv, 0.9f, 0.9f);

        if (color.nChannels() == 3) {
          colorBuffer.put(colorIndex, (byte) c.getBlue());
          colorBuffer.put(colorIndex + 1, (byte) c.getRed());
          colorBuffer.put(colorIndex + 2, (byte) c.getGreen());
        } else if (color.nChannels() == 1) {
          colorBuffer.put(colorIndex, (byte) c.getBlue());
        }

        // Sets the pixel to a value (greyscale).
        // colorBuffer.put(index, (byte)hsv);

        // Sets the pixel to a value (RGB, stored in BGR order).
        // buffer.put(index, blue);
        // buffer.put(index + 1, green);
        // buffer.put(index + 2, red);
      }
    }

    return color;

  }

  public void samplePoint(Integer inX, Integer inY) {
    ++clickCounter;
    x = inX;
    y = inY;
    ByteBuffer buffer = lastDepthImage.createBuffer();
    selectedDepthValue = buffer.get(y * lastDepthImage.width() + x) & 0xFF;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

  public boolean isDepth() {
    return useDepth;
  }

  public void useColor(boolean b) {
    useColor = b;
  }
  
  public boolean isColor() {
    return useColor;
  }

}
