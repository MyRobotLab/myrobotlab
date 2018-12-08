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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UShortRawIndexer;
import org.bytedeco.javacv.Parallel;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;
/**
 * <pre>
 * 
 * @author GroG
 * 
 * references :
 *  http://blog.elonliu.com/2017/03/18/kinect-coordinate-mapping-summary-and-pitfalls/
 *  https://smeenk.com/kinect-field-of-view-comparison/
 *  https://stackoverflow.com/questions/17832238/kinect-intrinsic-parameters-from-field-of-view/18199938#18199938
 *  https://answers.ros.org/question/195034/coordinates-of-a-specific-pixel-in-depthimage-published-by-kinect/
 * 
 * </pre>
 */
public class OpenCVFilterKinectPointCloud extends OpenCVFilter {

  // useful data for the kinect is 632 X 480 - 8 pixels on the right edge are
  // not good data
  // http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectPointCloud.class);

  private static final long serialVersionUID = 1L;

  transient IplImage lastDepth = null;

  IplImage returnImage = null;

  /**
   * list of samplepoint to return depth
   */
  List<Point> samplePoints = new ArrayList<>();
  
  // does array have a non-negligible smaller footprint ?
  // List<Point3df> pointCloud = new ArrayList<>(640*480);
  // Point3df[] pointCloud = new Point3df[640*480]; // for thread safety ? create it new ?
  // FIXME ! - PointCloud needs to be an object with meta-data
  // Point3df[][] pointCloud = null;
  
  PointCloud pointCloud = null;
  
  SphericalCoordinates transformer = null;

  boolean clearSamplePoints = false;
  
  // FIXME - position (x,y,z) heading(theta) & angle of kinect relative to the floor plane 
  
  Point3df cameraLocation = new Point3df();
  // pitch yaw heading  necessary ? - or heading suffice ?
  float cameraHeading = 0;
  
  float cameraTilt = 0;// degrees ?
  
  double r, theta, phi;

  /**
   * color depth info
   */
  IplImage color;
  
  double buffer[] = null;

  public OpenCVFilterKinectPointCloud(String name) {
    super(name);
    
    // http://myrobotlab.org/content/useful-kinect-info
    // vertical focal length
    theta = 43 * 0.017453;
    phi = 57 * 0.017453;
    r = 1;        
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage depth) throws InterruptedException {

    if (depth.depth() != 16 && depth.nChannels() != 1) {
      log.error("not valid kinect depth image expecting 1 channel 16 depth got {} channel {} depth", depth.depth(), depth.nChannels());
      return depth;
    }
    
    if (transformer == null) {
      transformer = new SphericalCoordinates(r, theta, phi);      
      //buffer = new double [depth.width() * depth.height()]
      // buffer = new double [depth.width()];
      buffer = new double [3];
    }

    lastDepth = depth;

    if (clearSamplePoints) {
      samplePoints.clear();
      clearSamplePoints = false;
    }

    if (color == null) {
      color = IplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3);
    }

    final UShortRawIndexer depthIdx = (UShortRawIndexer) depth.createIndexer();
    final UByteIndexer colorIdx = color.createIndexer();
    
    // f = camera focal length
    // xv = x viewport
    // yv = y viewport
    
    // w = screen width
    // h = screen height
    
    // xw = x world coordinate
    // xy = y world coordinate
    // zy = z world coordinate
    
    int w = 640;
    int h = 480;
    
    // https://www.mathsisfun.com/polar-cartesian-coordinates.html
    
    // double f = w / 2 * Math.tan(57/2);
    double f = h / 2 * Math.tan((43 * 0.0174533)/2);
    Point3df[][] points = new Point3df[w][h];
    
    // pixels to degrees
    
    float verticalRadiansPerPixel   = 0.0015635247916667f; // kinect 43 vertical focal length (43 * 0.0174533)/480
    float horizontalRadiansPerPixel = 0.00155443453125f;   // kinect 57 horizontal focal length (57 * 0.0174533)/640
    
    int tilt = 45;// degrees

    Parallel.loop(0, depth.height(), new Parallel.Looper() {
      public void loop(int from, int to, int looperID) {
        for (int xv = from; xv < to; xv++) {
          for (int yv = 0; yv < depth.width(); yv++) {
            
            // FIXME - find correct scale for x & y in mm
            // (189, 290, 46851)
            double xw,yw,zw = 0;
            int x,y,z = 0;
            int depth = depthIdx.get(xv, yv)/100;
            
            // zw = D * f / sqrt(xv² + yv² + f²)
            zw = depth * f / Math.sqrt(xv*xv + yv*yv + f*f);
            
            // xw = zw * xv / f
            xw = zw * xv / f;
            
            yw = zw * yv / f;
            
            //  https://hub.jmonkeyengine.org/t/point-cloud-visualization/25838
            
            // DO zw !
            
            x = (int)Math.round(xw);
            y = (int)Math.round(yw);
            z = (int)Math.round(zw);
            
//            points[yv][xv] = new Point3df(x,y,z); FIXME - WHAT HAPPENED ?!?!?
//            points[yv][xv] = new Point3df(xv, yv, depth);
//            points[yv][xv] = new Point3df(xv, (float)(Math.cos(yv)-Math.sin(xv)), depth);
//            points[yv][xv] = new Point3df((float)(xv + depth*Math.cos(30)), yv, depth);
//            points[yv][xv] = new Point3df(x,y,z);
//            points[yv][xv] = new Point3df(xv,yv,depth);
            buffer[0] = depth; // radius
            buffer[1] = f * x; // theta
            buffer[1] = f * y; // phi
            double[] cartesian = transformer.toCartesianGradient(buffer);
            points[yv][xv] = new Point3df((float)cartesian[0], (float)cartesian[1], (float)cartesian[2]);
            
            zw = depth * Math.cos(yv * verticalRadiansPerPixel);
            yw = depth * Math.sin(yv * verticalRadiansPerPixel);
            xw = depth * Math.sin(xv * horizontalRadiansPerPixel);

            points[yv][xv] = new Point3df((float)xv,(float)yv,(float)depth);
            
            // points[yv][xv] = new Point3df(xv,yv,depth);
            // z = depth;
            // z = (depth >> 3)/1000;
            // log.info("spherical ({},{},{}) -> world ({},{},{})", xv, yv, depth, x, y, z);
          }
        }
      }
    });
    
    pointCloud = new PointCloud(points);
    publishPointCloud(pointCloud);

    depthIdx.release();
    colorIdx.release();

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

  public void samplePoint(Integer x, Integer y) {
    samplePoints.add(new Point(x, y));
  }

  public void clearSamplePoints() {
    clearSamplePoints = true;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");
      Runtime.start("gui", "SwingGui");
      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("jme", "JMonkeyEngine");
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      
      jme.subscribe(cv.getName(), "publishPointCloud");
      
      // kinect data
      cv.capture("../1543648225287");
      // OpenCVFilterKinectPointCloud floor = (OpenCVFilterKinectPointCloud)cv.addFilter("floor","KinectFloorFinder");
      OpenCVFilterKinectPointCloud cloud = (OpenCVFilterKinectPointCloud)cv.addFilter("cloud","KinectPointCloud");
      
      
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

}
