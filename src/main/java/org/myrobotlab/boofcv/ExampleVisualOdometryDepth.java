/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

/*
 * Extended by Mats Önnerby to use then Kinect 360 as input instead of the .mpg files
 * used in the bare bones example it's based on 
 *
 */

package org.myrobotlab.boofcv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ejml.data.DMatrixRMaj;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.openkinect.freenect.Resolution;
import org.slf4j.Logger;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.MediaManager;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a
 * depth camera system, e.g. Kinect. Additional information on the scene can be
 * optionally extracted from the algorithm if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryDepth {
  
  transient public final static Logger log = LoggerFactory.getLogger(ExampleVisualOdometryDepth.class);

  Resolution resolution = Resolution.MEDIUM;

  BufferedImage buffRgb;
  BufferedImage buffDepth;

  BufferedImage outRgb;
  ImagePanel guiRgb;

  BufferedImage outDepth;
  ImagePanel guiDepth;

  Double xTot = 0.0;
  Double yTot = 0.0;
  Double zTot = 0.0;

  public void process() {

    MediaManager media = DefaultMediaManager.INSTANCE;

    // String directory = UtilIO.pathExample("kinect/straight");
    String directory = Util.getResourceDir() + File.separator + "BoofCv"+ File.separator;
    log.info("Using directory {}", directory);

    // load camera description and the video sequence
    VisualDepthParameters param = CalibrationIO.load(media.openFile(directory + "visualdepth.yaml"));

    // specify how the image features are going to be tracked
    PkltConfig configKlt = new PkltConfig();
    configKlt.pyramidScaling = new int[] { 1, 2, 4, 8 };
    configKlt.templateRadius = 3;

    PointTrackerTwoPass<GrayU8> tracker = FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1), GrayU8.class, GrayS16.class);

    DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(1e-3);

    // declares the algorithm
    DepthVisualOdometry<GrayU8, GrayU16> visualOdometry = FactoryVisualOdometry.depthDepthPnP(1.5, 120, 2, 200, 50, true, sparseDepth, tracker, GrayU8.class, GrayU16.class);

    // Pass in intrinsic/extrinsic calibration. This can be changed in the
    // future.
    visualOdometry.setCalibration(param.visualParam, new DoNothing2Transform2_F32());

    // Process the video sequence and output the location plus number of inliers
    SimpleImageSequence<GrayU8> videoVisual = media.openVideo(directory + "/" + "rgb.mjpeg", ImageType.single(GrayU8.class));
    SimpleImageSequence<GrayU16> videoDepth = media.openVideo(directory + "/" + "depth.mpng", ImageType.single(GrayU16.class));

    while (videoVisual.hasNext()) {

      GrayU8 visual = videoVisual.next();
      GrayU16 depth = videoDepth.next();

      // Handle the Depth stream
      if (outDepth == null) {
        BufferedImage mode = videoDepth.getGuiImage();
        depth.reshape(mode.getWidth(), mode.getHeight());
        outDepth = new BufferedImage(depth.width, depth.height, BufferedImage.TYPE_INT_RGB);
        guiDepth = ShowImages.showWindow(outDepth, "Depth Image");
      }

      if (!visualOdometry.process(visual, depth)) {
        throw new RuntimeException("VO Failed!");
      }

      VisualizeImageData.disparity(depth, outDepth, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
      guiDepth.repaint();

      // Handle the video stream
      if (outRgb == null) {
        BufferedImage mode = videoDepth.getGuiImage();
        visual.reshape(mode.getWidth(), mode.getHeight());
        outRgb = new BufferedImage(visual.width, visual.height, BufferedImage.TYPE_INT_RGB);
        guiRgb = ShowImages.showWindow(outRgb, "RGB Image");
      }

      ConvertBufferedImage.convertTo(visual, outRgb, true);
      guiRgb.repaint();

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
          System.out.printf("%S", rotation);
        }
        System.out.printf("\n");
      }
      System.out.printf("\n");
      xTot = xTot + T.x;
      yTot = yTot + T.y;
      zTot = zTot + T.z;
      // System.out.printf("xT, yT, Zt, %8.2f %8.2f %8.2f \n ", xTot, yTot,
      // zTot);

    }
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

  public static void main(String args[]) {
    ExampleVisualOdometryDepth app = new ExampleVisualOdometryDepth();
    app.process();
  }
}