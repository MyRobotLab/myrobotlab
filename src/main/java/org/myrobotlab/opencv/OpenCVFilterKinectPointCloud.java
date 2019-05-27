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
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UShortRawIndexer;
import org.bytedeco.javacv.Parallel;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import boofcv.alg.distort.radtan.RemoveRadialPtoN_F64;
import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point2D_F64;

/**
 * <pre>
 * 
 * &#64;author GroG
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

  public boolean clearPoints = false;

  transient IplImage lastDepth = null;

  IplImage returnImage = null;

  /**
   * list of samplepoint to return depth
   */
  List<Point> samplePoints = new ArrayList<>();

  PointCloud pointCloud = null;

  int colors[] = null;

  SphericalCoordinates transformer = null;

  boolean clearSamplePoints = false;

  Point3df cameraLocation = new Point3df();
  // pitch yaw heading necessary ? - or heading suffice ?
  float cameraHeading = 0;
  float cameraTilt = 0;// degrees ?
  double r, theta, phi;
  Point3df[] depthBuffer;
  float[] colorBuffer;
  IplImage color;
  // double focalLength = h / 2 * Math.tan((43 * 0.0174533)/2);
  // BoofCv
  RemoveRadialPtoN_F64 p2n = null;

  int width, height = 0;

  public OpenCVFilterKinectPointCloud(String name) {
    super(name);

    String baseDir = Util.getResourceDir() + File.separator + "BoofCv";
    String nameCalib = "intrinsic.yaml";

    CameraPinholeRadial param = CalibrationIO.load(new File(baseDir, nameCalib));
    p2n = new RemoveRadialPtoN_F64();
    p2n.setK(param.fx, param.fy, param.skew, param.cx, param.cy).setDistortion(param.radial, param.t1, param.t2);

    // http://myrobotlab.org/content/useful-kinect-info
    // vertical focal length
    theta = 43 * 0.017453;
    phi = 57 * 0.017453;
    r = 1;
  }

  @Override
  public void imageChanged(IplImage image) {
    width = image.width();
    height = image.height();
  }

  @Override
  public IplImage process(IplImage depth) throws InterruptedException {

    if (depth.depth() != 16 && depth.nChannels() != 1) {
      log.error("not valid kinect depth image expecting 1 channel 16 depth got {} channel {} depth", depth.depth(), depth.nChannels());
      return depth;
    }

    if (transformer == null) {
      transformer = new SphericalCoordinates(r, theta, phi);
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

    // buffer = FloatBuffer.allocate(depth.width() * depth.height());
    depthBuffer = new Point3df[width * height];
    if (colorBuffer == null) {
      colorBuffer = new float[width * height * 4]; // RGBA
    }

    /**
     * <pre>
     * for (int i = 0; i < color.width() * color.height(); ++i) {
     *   colorBuffer[i] = 0.5f;
     *   colorBuffer[i + 1] = 0.5f;
     *   colorBuffer[i + 2] = i % 100 * .999f;
     *   colorBuffer[i + 3] = 1f;
     * }
     * </pre>
     */

    Point2D_F64 n = new Point2D_F64();

    Parallel.loop(0, height, new Parallel.Looper() {
      public void loop(int from, int to, int looperID) {
        for (int xv = from; xv < to; xv++) {
          for (int yv = 0; yv < width; yv++) {

            // FIXME - find correct scale for x & y in mm

            double xw, yw, zw = 0;
            float depth = depthIdx.get(xv, yv);

            // zw = D * f / sqrt(xv² + yv² + f²)
            // https://hub.jmonkeyengine.org/t/point-cloud-visualization/25838

            int index = yv * height + xv;
            // colorIdx.put(index, 33);
            // colorIdx.put(index+1, 33);

            /**
             * <pre>
             * BoofCv way p2n.compute(xv,yv,n); Point3D_F64 p = new
             * Point3D_F64(); p.z = depth; p.x = n.x*p.z; p.y = n.y*p.z;
             * 
             * float scaled = depth/1000;
             * 
             * yw = n.y*scaled; xw = n.x*scaled; zw = scaled; // really ?
             */

            zw = -1 * depth / 1000; // we want in 1 meter world unit
            xw = 2 * (xv - 639 / 2) * Math.tan(57 / 2 * 0.0174533) * (zw / 640);
            yw = 2 * (479 - yv - 479 / 2) * Math.tan(43 / 2 * 0.0174533) * (zw / 480);

            // points[index] = new Point3df((float)xw,(float)yw,(float)zw);
            // jmonkey has a flipped y/x
            depthBuffer[index] = new Point3df((float) yw, (float) xw, (float) zw);
            if (color != null) {

            }
          }
        }
      }
    });

    pointCloud = new PointCloud(depthBuffer);
    pointCloud.setColors(colorBuffer);

    // NO MORE PUBLISHING - just put into OpenCVData !!!
    // publishPointCloud(pointCloud);

    put(pointCloud);

    depthIdx.release();
    colorIdx.release();

    return depth;
  }

  public void publishPlane() {
    // spin through sample points
    for (int i = 0; i < samplePoints.size(); ++i) {

    }
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

      jme.attach(cv);
      // jme.subscribe(cv.getName(), "publishPointCloud");
      Service.sleep(3000); // FIXME - fix race condition...

      // kinect data
      // cv.capture("../1543648225287");
      cv.capture("../00000004.png");
      // OpenCVFilterKinectPointCloud floor =
      // (OpenCVFilterKinectPointCloud)cv.addFilter("floor","KinectFloorFinder");
      OpenCVFilterKinectPointCloud cloud = (OpenCVFilterKinectPointCloud) cv.addFilter("cloud", "KinectPointCloud");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

}
