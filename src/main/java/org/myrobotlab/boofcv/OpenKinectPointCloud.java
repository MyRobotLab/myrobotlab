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
import java.util.ArrayList;
import java.util.List;

import org.ddogleg.struct.FastQueue;
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

import com.sun.jna.NativeLibrary;

import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point3D_F64;

/**
 * Example demonstrating how to process and display data from the Kinect.
 *
 * @author Peter Abeles and Mats Önnerby
 */
public class OpenKinectPointCloud {

  {
    // be sure to set OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY to the
    // location of your shared library!
    NativeLibrary.addSearchPath("freenect", OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY);
  }

  private PointCloudViewer viewer;

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
  int colors[] = new int[1];

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
    while (starTime + 100000 > System.currentTimeMillis()) {

      if (videoAvailable && depthAvailable) {
        if (firstImage) {
          viewer.getComponent().setPreferredSize(new Dimension(rgb.width, rgb.height));
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

        viewer.clearPoints();
        if (colors.length != points.size()) {
          Log.info("WTF", colors.length, points.size(), cloud.size);
        }
        viewer.addCloud(points, colors);

        if (firstImage) {
          ShowImages.showWindow(viewer.getComponent(), "Point Cloud", true);
          firstImage = false;
        } else {
          viewer.getComponent().repaint();
        }
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

  public static void main(String args[]) {
    OpenKinectPointCloud app = new OpenKinectPointCloud();

    app.process();
  }
}
