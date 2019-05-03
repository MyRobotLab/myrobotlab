/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.myrobotlab.boofcv;

import java.awt.Dimension;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.equation.Equation;
import org.ejml.equation.Sequence;
import org.myrobotlab.image.Util;
import org.openkinect.freenect.Context;
import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.VideoFormat;
import org.openkinect.freenect.VideoHandler;
import org.python.jline.internal.Log;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.color.ColorRgb;
import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Example demonstrating how to process and display data from the Kinect.
 *
 * @author Peter Abeles and Mats Önnerby
 * 
 *         This program currently calculates the Odometry ( how much the camera
 *         has moved as translations and rotations ). Second stage is to
 *         recalculate the second pointcloud to have the same origin as the
 *         first. Third stage is to update the original pointcloud with
 *         information from the new pointcloud to make the world pointcloud
 *         larger Do we need to keep track of the probability for each point in
 *         the pointcloud ? Can we transform the pointcloud to larger objects ?
 *         Like meshes ?
 *         https://www.mathworks.com/help/vision/examples/3-d-point-cloud-registration-and-stitching.html
 * 
 */
public class OpenKinectOdometry {

  private PointCloudViewer viewer;
  private PointCloudViewer viewerFixed;

  private volatile boolean videoAvailable = false;
  private volatile boolean depthAvailable = false;

  boolean firstDepth = true;
  boolean firstVideo = true;
  boolean firstImage = true;

  String baseDir = Util.getResourceDir() + File.separator + "BoofCv";
  String nameCalib = "intrinsic.yaml";

  Planar<GrayU8> rgb = new Planar<>(GrayU8.class, 1, 1, 3);
  GrayU16 depth = new GrayU16(1, 1);

  List<Point3D_F64> points = new ArrayList<Point3D_F64>();
  List<Point3D_F64> pointsFixed = new ArrayList<Point3D_F64>();
  int colors[] = new int[1];

  boolean odometry = true;

  DepthVisualOdometry<GrayU8, GrayU16> visualOdometry;

  Double xTot = 0.0;
  Double yTot = 0.0;
  Double zTot = 0.0;

  // Matrix multiplication initiation
  Sequence transform, invert;
  DMatrixRMaj pointMatIn = new DMatrixRMaj(4, 1);
  DMatrixRMaj pointMatOut = new DMatrixRMaj(4, 1);
  DMatrixRMaj transMat = new DMatrixRMaj(4, 4);
  Equation eq = new Equation();

  public void process() {

    Context kinect = Freenect.createContext();

    if (kinect.numDevices() < 0)
      throw new RuntimeException("No kinect found!");

    Device device = kinect.openDevice(0);

    device.setDepthFormat(DepthFormat.REGISTERED);
    device.setVideoFormat(VideoFormat.RGB);

    CameraPinholeRadial param = CalibrationIO.load(new File(baseDir, nameCalib));

    FastQueue<Point3D_F64> cloud = new FastQueue<>(Point3D_F64.class, true);
    FastQueueArray_I32 cloudColor = new FastQueueArray_I32(3);

    if (odometry) {
      initOdometry(param);
    }

    viewer = VisualizeData.createPointCloudViewer();
    viewer.setCameraHFov(PerspectiveOps.computeHFov(param));
    viewer.setTranslationStep(15);

    device.startDepth(new DepthHandler() {
      @Override
      public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
        processDepth(mode, frame, timestamp);
      }
    });
    device.startVideo(new VideoHandler() {
      @Override
      public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
        processRgb(mode, frame, timestamp);
      }
    });

    long starTime = System.currentTimeMillis();
    while (starTime + 200000 > System.currentTimeMillis()) {

      if (videoAvailable && depthAvailable) {
        if (firstImage) {
          viewer.getComponent().setPreferredSize(new Dimension(rgb.width, rgb.height));
          if (odometry) {
            viewerFixed.getComponent().setPreferredSize(new Dimension(rgb.width, rgb.height));
          }
        }

        VisualDepthOps.depthTo3D(param, rgb, depth, cloud, cloudColor);

        if (colors.length != cloud.size()) {
          colors = new int[cloud.size()];
          points = new ArrayList<Point3D_F64>(cloud.size());
        }

        points.clear();
        for (int i = 0; i < cloud.size; i++) {
          Point3D_F64 p = cloud.get(i);
          int[] color = cloudColor.get(i);
          int c = (color[0] << 16) | (color[1] << 8) | color[2];
          points.add(p);
          colors[i] = c;
        }

        Log.info("Points size() 1 ", points.size(), "\n");
        viewer.clearPoints();
        viewer.addCloud(points, colors);

        if (firstImage) {
          ShowImages.showWindow(viewer.getComponent(), "Point Cloud", true);
          if (odometry) {
            ShowImages.showWindow(viewerFixed.getComponent(), "Point Cloud Fixed", true);
          }
          firstImage = false;
        }

        // Odometry
        if (odometry) {
          processOdometry();
        }

        viewer.getComponent().repaint();
        videoAvailable = false;
        depthAvailable = false;
      }
      sleep(10);
    }
    System.out.println("100 Seconds elapsed");

    device.stopDepth();
    device.stopVideo();
    device.close();
  }

  private void initOdometry(CameraPinholeRadial param) {
    // Initiate and precompile Matrix operations
    eq.alias(pointMatIn, "in", transMat, "R", pointMatOut, "out");
    eq.alias(new DMatrixRMaj(1, 1), "in");
    eq.alias(new DMatrixRMaj(1, 1), "R");
    eq.alias(new DMatrixRMaj(1, 1), "out");
    transform = eq.compile("out = R*in");
    invert = eq.compile("R = inv(R)");
    eq.alias(pointMatIn, "in", transMat, "R", pointMatOut, "out");

    MediaManager media = DefaultMediaManager.INSTANCE;
    // String directory = UtilIO.pathExample("kinect/straight");
    String directory = Util.getResourceDir() + File.separator + "BoofCv"+File.separator;
    Log.info("Using directory ", directory);

    // load camera description and the video sequence
    VisualDepthParameters depthParam = CalibrationIO.load(media.openFile(directory + "visualdepth.yaml"));

    // specify how the image features are going to be tracked
    PkltConfig configKlt = new PkltConfig();
    configKlt.pyramidScaling = new int[] { 1, 2, 4, 8 };
    configKlt.templateRadius = 3;

    PointTrackerTwoPass<GrayU8> tracker = FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1), GrayU8.class, GrayS16.class);

    DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(1e-3);

    // declares the algorithm
    visualOdometry = FactoryVisualOdometry.depthDepthPnP(1.5, 120, 2, 200, 50, true, sparseDepth, tracker, GrayU8.class, GrayU16.class);

    // Pass in intrinsic/extrinsic calibration. This can be changed in the
    // future.
    visualOdometry.setCalibration(depthParam.visualParam, new DoNothing2Transform2_F32());

    viewerFixed = VisualizeData.createPointCloudViewer();
    viewerFixed.setCameraHFov(PerspectiveOps.computeHFov(param));
    viewerFixed.setTranslationStep(15);
  }

  private void processOdometry() {

    GrayU8 gray = new GrayU8(rgb.width, rgb.height);
    ColorRgb.rgbToGray_Weighted(rgb, gray);
    if (!visualOdometry.process(gray, depth)) {
      throw new RuntimeException("VO Failed!");
    }

    Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
    Vector3D_F64 T = leftToWorld.getT();
    DMatrixRMaj R = leftToWorld.getR();
    System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));

    int cols = R.getNumCols();
    int rows = R.getNumRows();
    System.out.printf("Rotation matrix \n");
    for (int x = 0; x < cols; x++) {
      for (int y = 0; y < rows; y++) {
        Double rotation = R.get(x, y);
        System.out.printf("%8.2f", rotation);
      }
      System.out.printf("\n");
    }
    System.out.printf("\n");
    pointsFixed.clear();
    // Rotate and transform

    // Load the (4*4) Transformation matrix from Rotations (3*3) and
    // Translations (3*1)
    // From the Odometry
    transMat.set(0, 0, R.get(0, 0));
    transMat.set(1, 0, R.get(1, 0));
    transMat.set(2, 0, R.get(2, 0));
    transMat.set(3, 0, T.getX());
    transMat.set(0, 1, R.get(0, 0));
    transMat.set(1, 1, R.get(1, 1));
    transMat.set(2, 1, R.get(2, 1));
    transMat.set(3, 1, T.getY());
    transMat.set(0, 2, R.get(0, 2));
    transMat.set(1, 2, R.get(1, 2));
    transMat.set(2, 2, R.get(2, 2));
    transMat.set(3, 2, T.getZ());
    transMat.set(0, 3, 0.0);
    transMat.set(1, 3, 0.0);
    transMat.set(2, 3, 0.0);
    transMat.set(3, 3, 1.0);

    // Load the (4*4) Transformation matrix from Rotations (3*3) and
    // Translations (3*1)
    // No rotation or transformation (unit matrix)
    // 1 0 0 0
    // 0 1 0 0
    // 0 0 1 0
    // 0 0 0 1
    transMat.set(0, 0, 1.0);
    transMat.set(1, 0, 0.0);
    transMat.set(2, 0, 0.0);
    transMat.set(3, 0, 0.0); // Translate X
    transMat.set(0, 1, 0.0);
    transMat.set(1, 1, 1.0);
    transMat.set(2, 1, 0.0);
    transMat.set(3, 1, 0.0); // Translate Y
    transMat.set(0, 2, 0.0);
    transMat.set(1, 2, 0.0);
    transMat.set(2, 2, 1.0);
    transMat.set(3, 2, 0.0); // Translate Z
    transMat.set(0, 3, 0.0);
    transMat.set(1, 3, 0.0);
    transMat.set(2, 3, 0.0);
    transMat.set(3, 3, 1.0);

    Log.info("Points size() 2", points.size(), "\n");
    for (int i = 0; i < points.size(); i++) {
      // Transform from cartesian to homogenous coordinates ( 4 dimensions )
      Point3D_F64 p = points.get(i);
      pointMatIn.set(0, 0, p.getX());
      pointMatIn.set(1, 0, p.getY());
      pointMatIn.set(2, 0, p.getZ());
      pointMatIn.set(3, 0, 1.0);
      // This statement executes the Matrix multiplication defined in transform
      // Log.info("Points in ", pointMatIn.get(0, 0), " ", pointMatIn.get(1, 0),
      // " ", pointMatIn.get(2, 0), " ", pointMatIn.get(3,
      // 0));
      transform.perform();
      // Log.info("Points out ", pointMatOut.get(0, 0), " ", pointMatOut.get(1,
      // 0), " ", pointMatOut.get(2, 0), " ",
      // pointMatOut.get(3, 0));
      Point3D_F64 pFixed = new Point3D_F64(pointMatOut.get(0, 0) / pointMatOut.get(3, 0), pointMatOut.get(1, 0) / pointMatOut.get(3, 0),
          pointMatOut.get(2, 0) / pointMatOut.get(3, 0));
      pointsFixed.add(pFixed);
    }

    viewerFixed.clearPoints();
    viewerFixed.addCloud(pointsFixed, colors);
    viewerFixed.getComponent().repaint();

  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }

  }

  protected void processDepth(FrameMode mode, ByteBuffer frame, int timestamp) {

    // System.out.println("Got depth! "+timestamp);
    if (firstDepth) {
      depth.reshape(mode.getWidth(), mode.getHeight());
      firstDepth = false;
    }

    // Skip frames until the previous depth map has been shown
    if (!depthAvailable) {
      // Convert the frame to a depth map))
      UtilOpenKinect.bufferDepthToU16(frame, depth);
      // Log.info("frame.capacity", frame.capacity());
      // Log.info("depthAvailable", depth.width, depth.height);
      depthAvailable = true;
    }
  }

  protected void processRgb(FrameMode mode, ByteBuffer frame, int timestamp) {

    if (mode.getVideoFormat() != VideoFormat.RGB) {
      System.out.println("Bad rgb format!");
    }

    // System.out.println("Got rgb! "+timestamp);
    if (firstVideo) {
      rgb.reshape(mode.getWidth(), mode.getHeight());
      viewer.getComponent().setPreferredSize(new Dimension(rgb.width, rgb.height));
      firstVideo = false;
    }

    // Skip frames until the previous video has been shown
    if (!videoAvailable) {
      UtilOpenKinect.bufferRgbToMsU8(frame, rgb);
      // Log.info("videoAvailable");
      videoAvailable = true;
    }

  }

  /**
   * If the algorithm implements AccessPointTracks3D, then count the number of
   * inlier features and return a string.
   */
  public static String inlierPercent(VisualOdometry alg) {
    if (!(alg instanceof AccessPointTracks3D))
      return "";

    AccessPointTracks3D access = (AccessPointTracks3D) alg;

    int count = 0;
    int N = access.getAllTracks().size();
    for (int i = 0; i < N; i++) {
      if (access.isInlier(i))
        count++;
    }

    return String.format("%%%5.3f", 100.0 * count / N);
  }

  public static String toAbsolutePath(String maybeRelative) {
    Path path = Paths.get(maybeRelative);
    Path effectivePath = path;
    if (!path.isAbsolute()) {
      Path base = Paths.get("");
      effectivePath = base.resolve(path).toAbsolutePath();
    }
    return effectivePath.normalize().toString();
  }

  public static void main(String args[]) {
    OpenKinectOdometry app = new OpenKinectOdometry();

    app.process();
  }
}
