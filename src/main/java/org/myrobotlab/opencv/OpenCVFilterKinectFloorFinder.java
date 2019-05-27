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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.javacpp.indexer.UShortRawIndexer;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * <pre>
 * 
 * &#64;author GroG
 * 
 * reverse scan from the bottom a series of lines y = height - (i + step) and get depth
 * 
 * find the middle of the widest and deepest path - publish the center point and the width
 * 
 * parameters :
 *    minWidth - minimum width in path - this can be cacluated if the horizontal focal length is known
 *    maxScanHeight - distance from the bottom to scan up - typically only up to the horizon is needed (max would be == height)
 * 
 * publishes :
 *    deepest point - with minimum width
 * 
 * </pre>
 */
public class OpenCVFilterKinectFloorFinder extends OpenCVFilter {

  // useful data for the kinect is 632 X 480 - 8 pixels on the right edge are
  // not good data
  // http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectFloorFinder.class);

  private static final long serialVersionUID = 1L;

  transient IplImage lastDepth = null;

  transient IplImage avgFloor = null;

  List<Rectangle> paths = new ArrayList<>();
  Rectangle minPathArea = null;
  int minPathWidth = 300;
  int minPathHeight = 300;

  IplImage returnImage = null;

  /**
   * list of samplepoint to return depth
   */
  List<Point3df> samplePoints = new ArrayList<>();

  boolean clearSamplePoints = false;

  /**
   * color depth info
   */
  IplImage color;

  public OpenCVFilterKinectFloorFinder(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  /**
   * <pre>
   * 
   * FIXME - THIS STILL SUFFERS FROM POLAR =&gt; CARTESIAN ERROR !!!
   * 
   * function to try to derive the floor plane as a set of 
   * average x, y, depth
   * 
   * plane formula :
   *    a(x-x0) + b(y-y0) + c(z-z0) = 0
   *    
   *    x0,y0,z0 is a point on the plane
   *    a,b,c : vector perpendicular to the plane
   * 
   * 
   * &#64;param depth
   * 
   * </pre>
   */
  public IplImage learnFloor(IplImage depth) {

    // strategy - get a collection of points (random) or provided by user
    // plane formula
    if (samplePoints.size() < 3) {
      // need some sample points !!! at "least 3" !

      Point3df p1 = samplePoints.get(0);
      Point3df p2 = samplePoints.get(1);
      Point3df p3 = samplePoints.get(2);

      SphericalCoordinates x;

      float a1 = p2.x - p1.x;
      float b1 = p2.y - p1.y;
      float c1 = p2.z - p1.z;
      float a2 = p3.x - p1.x;
      float b2 = p3.y - p1.y;
      float c2 = p3.z - p1.z;
      float a = b1 * c2 - b2 * c1;
      float b = a2 * c1 - a1 * c2;
      float c = a1 * b2 - b1 * a2;
      float d = (-a * p1.x - b * p1.y - c * p1.z);
      log.info("equation of plane is " + a + " x + " + b + " y + " + c + " z + " + d + " = 0.");

      return depth;
    }

    // derive plane from sample points
    // https://www.geeksforgeeks.org/program-to-find-equation-of-a-plane-passing-through-3-points/

    final UShortRawIndexer depthIdx = (UShortRawIndexer) depth.createIndexer();
    // for (int y = h; y > 1; --y) {
    for (int y = depth.height() - 1; y > -1; --y) {
      // scan left to right
      for (int x = 0; x < depth.width() - 1; ++x) {
        try {
          int range = depthIdx.get(y, x);
          // if Math.abs(avgFloor.get(y,x) - range) > maxVariance
          // nonFloorDepth (either wall or hole)
          // scanning left to right and I in or out of floor
          // if (part of floor, makes this 'end marker'
          // compute size of rectangle/path - > minarea addPath
          // else (begin marker)
        } catch (Exception e) {
          log.error("here x");
        }
      }
    }
    depthIdx.release();

    return depth;
  }

  final public String MODE_LEARN = "MODE_LEARN";
  final public String MODE_FIND_PATH = "MODE_FIND_PATH"; // find best path ?
                                                         // other paths ?

  String mode = MODE_LEARN;

  @Override
  public IplImage process(IplImage depth) throws InterruptedException {

    if (mode.equals(MODE_LEARN)) {
      return learnFloor(depth);
    }

    if (depth.depth() != 16 && depth.nChannels() != 1) {
      log.error("not valid kinect depth image expecting 1 channel 16 depth got {} channel {} depth", depth.depth(), depth.nChannels());
      return depth;
    }

    lastDepth = depth;

    if (clearSamplePoints) {
      samplePoints.clear();
      clearSamplePoints = false;
    }

    if (color == null) {
      color = IplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3);
    }

    // f = camera focal length
    // xv = x viewport
    // yv = y viewport

    // w = screen width
    // h = screen height

    // xw = x world coordinate
    // yw = y world coordinate
    // zw = z world coordinate

    int h = depth.height();
    int w = depth.width();

    // scan left to right bottom to top

    final UShortRawIndexer depthIdx = (UShortRawIndexer) depth.createIndexer();
    // for (int y = h; y > 1; --y) {
    for (int y = h - 1; y > -1; --y) {
      // scan left to right
      for (int x = 0; x < w - 1; ++x) {
        try {
          int range = depthIdx.get(y, x);
          // if Math.abs(avgFloor.get(y,x) - range) > maxVariance
          // nonFloorDepth (either wall or hole)
          // scanning left to right and I in or out of floor
          // if (part of floor, makes this 'end marker'
          // compute size of rectangle/path - > minarea addPath
          // else (begin marker)
        } catch (Exception e) {
          log.error("here x");
        }
      }
    }
    depthIdx.release();

    /**
     * <pre>
     *  nice Looper logic for fast analysis
     
     final UShortRawIndexer depthIdx = (UShortRawIndexer) depth.createIndexer();
     final UByteIndexer colorIdx = color.createIndexer();
    
     Parallel.loop(0, depth.height(), new Parallel.Looper() { // "y" - loopers
       public void loop(int from, int to, int looperID) {
         // scan a section of y
         for (int xv = from; xv < to; xv++) {
           // scan left to right
           for (int x = 0; x < depth.width(); x++) {
         
             int depth = depthIdx.get(xv, x);
    
         
           }
         }
       }
     });
    
     depthIdx.release();
     colorIdx.release();
     * 
     * </pre>
     */

    return depth;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (lastDepth == null) {
      return image;
    }
    ByteBuffer buffer = lastDepth.getByteBuffer();
    /*
     * for (Point point : samplePoints) {
     * 
     * int depthBytesPerChannel = lastDepth.depth() / 8; int depthIndex =
     * point.y * lastDepth.widthStep() + point.x * lastDepth.nChannels() *
     * depthBytesPerChannel;
     * 
     * String str = String.format("(%d,%d) %d", point.x, point.y,
     * (buffer.get(depthIndex + 1) & 0xFF) << 8 | (buffer.get(depthIndex) &
     * 0xFF)); graphics.drawString(str, point.x + 3, point.y);
     * graphics.drawOval(point.x, point.y, 2, 2); }
     */
    return image;
  }

  public void samplePoint(Integer x, Integer y) {
    if (lastDepth != null) {
      final UShortRawIndexer depthIdx = (UShortRawIndexer) lastDepth.createIndexer();
      samplePoints.add(new Point3df(x, y, depthIdx.get(y, x)));
    }
  }

  public void clearSamplePoints() {
    clearSamplePoints = true;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");
      Runtime.start("gui", "SwingGui");
      // JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("jme",
      // "JMonkeyEngine");
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");

      // jme.subscribe(cv.getName(), "publishPointCloud");

      // kinect data
      cv.capture("../1543648225286");

      // cv.addFilter("depth", "KinectDepth");
      // cv.addFilter("floor", "KinectFloorFinder");
      cv.addFilter("points", "KinectPointCloud");

      // OpenCVFilterKinectFloorFinder floor =
      // (OpenCVFilterKinectFloorFinder)cv.addFilter("floor","KinectFloorFinder");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

}
