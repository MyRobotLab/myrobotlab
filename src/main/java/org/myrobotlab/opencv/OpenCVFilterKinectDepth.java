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

import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_8U;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacv.Parallel;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
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

  boolean firstError = true;

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

  public OpenCVFilterKinectDepth() {
    super();
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

    if (firstError && depth.depth() != 16 && depth.nChannels() != 1) {
      log.error("not valid kinect depth image expecting 1 channel 16 depth got {} channel {} depth", depth.depth(), depth.nChannels());
      firstError = false;
      return depth;
    }

    lastDepth = depth;

    if (clearSamplePoints) {
      samplePoints.clear();
      clearSamplePoints = false;
    }

    if (!useDepth) {
      return data.getKinectVideo();
    }

    if (useColor) {

      // parallel indexers !
      // https://github.com/bytedeco/bytedeco.github.io/blob/master/_posts/2014-12-23-third-release.md

      if (color == null) {
        color = AbstractIplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3);
      }

      final UByteRawIndexer depthIdx = (UByteRawIndexer) depth.createIndexer();
      final UByteIndexer colorIdx = color.createIndexer();

      Parallel.loop(0, depth.height(), new Parallel.Looper() {
        @Override
        public void loop(int from, int to, int looperID) {
          for (int i = from; i < to; i++) {
            for (int j = 0; j < depth.width(); j++) {

              int value = depthIdx.get(i, j);
              double hsv = minY + ((value - minX) * (maxY - minY)) / (maxX - minX);
              Color c = Color.getHSBColor((float) hsv, 0.9f, 0.9f);

              colorIdx.put(i, j, 0, (byte) c.getBlue());
              colorIdx.put(i, j, 1, (byte) c.getRed());
              colorIdx.put(i, j, 2, (byte) c.getGreen());
            }
          }
        }
      });

      depthIdx.release();
      colorIdx.release();

      return color;
    }

    return depth;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (lastDepth == null) {
      return image;
    }
    ByteBuffer buffer = lastDepth.getByteBuffer();
    for (Point point : samplePoints) {

      int depthBytesPerChannel = lastDepth.depth() / 8;
      int depthIndex = point.y * lastDepth.widthStep() + point.x * lastDepth.nChannels() * depthBytesPerChannel;

      String str = String.format("(%d,%d) %d", point.x, point.y, (buffer.get(depthIndex + 1) & 0xFF) << 8 | (buffer.get(depthIndex) & 0xFF));
      graphics.drawString(str, point.x + 3, point.y);
      graphics.drawOval(point.x, point.y, 2, 2);
    }
    return image;
  }

  @Override
  public void samplePoint(Integer x, Integer y) {
    samplePoints.add(new Point(x, y));
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
