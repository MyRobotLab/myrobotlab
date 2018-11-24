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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point;
import org.slf4j.Logger;

public class OpenCVFilterKinectDepth extends OpenCVFilter {

  // useful data for the kinect is 632 X 480 - 8 pixels on the right edge are
  // not good data
  // http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectDepth.class);

  private static final long serialVersionUID = 1L;

  transient IplImage lastDepth = null;
  double maxX = 65535;
  double maxY = 1.0;
  double minX = 0;
  double minY = 0.0;
  
  IplImage returnImage = null;
  
  /**
   * list of samplepoint to return depth
   */
  List<Point> samplePoints = new ArrayList<>();
  
  boolean clearSamplePoints = false;

  /**
   * default return colored 3x256(rgb) depth / false is a lossy 1x(256) grey
   * scale
   */
  boolean useColor = true;

  /**
   * use depth as return image
   */
  boolean useDepth = true;
  
  /**
   * color depth info
   */
  IplImage color;

  public OpenCVFilterKinectDepth(String name) {
    super(name);
  }

  // FIXME - use Parrallel
  public IplImage color(IplImage depth) throws InterruptedException {

    // 256 grey channels is not enough for kinect
    // color (3x256 channels) is enough
    if (color == null) {
      color = IplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3);
    }

    ByteBuffer colorBuffer = color.getByteBuffer();
    // it may be deprecated but the "new" function .asByteBuffer() does not
    // return all data
    ByteBuffer depthBuffer = depth.getByteBuffer();

    int depthBytesPerChannel = depth.depth() / 8;

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
      }
    }
    return color;
  }

  
  @Override
  public void imageChanged(IplImage image) {
  }

  public boolean isColor() {
    return useColor;
  }

  public boolean isDepth() {
    return useDepth;
  }
  
  @Override
  public IplImage process(IplImage depth) throws InterruptedException {
    
    lastDepth = depth;
    
    if (clearSamplePoints) {
      samplePoints.clear();
      clearSamplePoints = false;
    }
  
    if (!useDepth) {
      return data.getKinectVideo();
    }
    
    if (useColor) {
      return color(depth);
    }

    return depth;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    ByteBuffer buffer = lastDepth.getByteBuffer();
    for (Point point: samplePoints) {

      int depthBytesPerChannel = lastDepth.depth() / 8;
      int depthIndex = point.y * lastDepth.widthStep() + point.x * lastDepth.nChannels() * depthBytesPerChannel;
      
      String str = String.format("(%d,%d) %d", point.x, point.y, (buffer.get(depthIndex + 1) & 0xFF) << 8 | (buffer.get(depthIndex) & 0xFF));
      graphics.drawString(str, point.x + 3, point.y);
      graphics.drawOval(point.x, point.y, 2, 2);
    }
    return image;
  }

  public IplImage processx(IplImage image) {

    if (returnImage == null) {
      returnImage = IplImage.create(image.width(), image.height(), 16, 3);
    }

    // cvCvtColor(image, returnImage, CV_GRAY2BGR);
    // cvCvtColor(image, returnImage, CV_BGR2HSV);
    // cvCvtColor(image, returnImage, COLOR_BGR2HSV);

    return returnImage;

  }

  public void samplePoint(Integer x, Integer y) {
    samplePoints.add(new Point(x,y));
  }

  public void useColor(boolean b) {
    useColor = b;
  }

  public void useDepth(boolean b) {
    useDepth = b;
  }

  public void clearSamplePoints() {
    clearSamplePoints = true;
  }

}
