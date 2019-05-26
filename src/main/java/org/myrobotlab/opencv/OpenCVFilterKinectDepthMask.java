/**
 *                    
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

// TODO - have no published OpenCV items - move all to java.awt objects - no native code necessary in viewer

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.opencv.global.opencv_core.cvClearMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvInRangeS;
import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_core.cvRect;
import static org.bytedeco.opencv.global.opencv_core.cvResetImageROI;
import static org.bytedeco.opencv.global.opencv_core.cvScalar;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;
import static org.bytedeco.opencv.global.opencv_core.cvSize;
import static org.bytedeco.opencv.global.opencv_core.cvZero;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.opencv.global.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.opencv.global.opencv_imgproc.cvBoundingRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCheckContourConvexity;
import static org.bytedeco.opencv.global.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPyrDown;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.CvContour;
import org.bytedeco.opencv.opencv_core.CvMemStorage;
import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.CvSeq;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.image.KinectImageNode;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterKinectDepthMask extends OpenCVFilter {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectDepthMask.class);

  IplImage mask = null;

  public OpenCVFilterKinectDepthMask(String name) {
    super(name);
  }

  public OpenCVFilterKinectDepthMask() {
    super();
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) {
    // verify we can get a kinect depth
    IplImage kinectDepth = data.getKinectDepth();
    if (kinectDepth == null) {
      return image;
    }

    // create mask based on desired depth
    IplImage mask = getDepthMask(480, 500, kinectDepth, image);
    return mask;

  }

  // consider parallel looping ...
  // http://bytedeco.org/news/2014/12/23/third-release/

  private IplImage getDepthMask(int minDepth, int maxDepth, IplImage depth, IplImage video) {

    if (video == null) {
      // create a 1 channel mask
      video = cvCreateImage(cvSize(depth.width(), depth.height()), 8, 1);
    }

    ByteBuffer maskBuffer = video.getByteBuffer();
    // it may be deprecated but the "new" function .asByteBuffer() does not
    // return all data
    ByteBuffer depthBuffer = depth.getByteBuffer();

    int depthBytesPerChannel = depth.depth() / 8;

    // iterate through the depth bytes bytes and convert to HSV / RGB format
    // map depth gray (0,65535) => 3 x (0,255) HSV :P
    for (int y = 0; y < depth.height(); y++) { // 480
      for (int x = 0; x < depth.width(); x++) { // 640
        int depthIndex = y * depth.widthStep() + x * depth.nChannels() * depthBytesPerChannel;
        int colorIndex = y * video.widthStep() + x * video.nChannels();

        // Used to read the pixel value - the 0xFF is needed to cast from
        // an unsigned byte to an int.
        // int value = depthBuffer.get(depthIndex);// << 8 & 0xFF +
        // buffer.get(depthIndex+1)& 0xFF;
        // this is 16 bit depth - I switched the MSB !!!!
        int value = (depthBuffer.get(depthIndex + 1) & 0xFF) << 8 | (depthBuffer.get(depthIndex) & 0xFF);

        minDepth = 6000;
        maxDepth = 20000;

        if (video.nChannels() == 3) {
          /*
           * do nothing video.put(colorIndex, (byte) c.getBlue());
           * video.put(colorIndex + 1, (byte) c.getRed()); video.put(colorIndex
           * + 2, (byte) c.getGreen());
           */
          if (value > minDepth && value < maxDepth) {
            // maskBuffer.put(colorIndex, (byte) 255); - do nothing
          } else {
            // maskBuffer.put(colorIndex, (byte) 0);
            maskBuffer.put(colorIndex, (byte) 0);
            maskBuffer.put(colorIndex + 1, (byte) 0);
            maskBuffer.put(colorIndex + 2, (byte) 0);
          }

        } else if (video.nChannels() == 1) {

          if (value > minDepth && value < maxDepth) {
            maskBuffer.put(colorIndex, (byte) 255);
          } else {
            maskBuffer.put(colorIndex, (byte) 0);
          }
        }

        // Sets the pixel to a value (greyscale).
        // maskBuffer.put(index, (byte)hsv);

        // Sets the pixel to a value (RGB, stored in BGR order).
        // buffer.put(index, blue);
        // buffer.put(index + 1, green);
        // buffer.put(index + 2, red);
      }
    }

    return video;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
