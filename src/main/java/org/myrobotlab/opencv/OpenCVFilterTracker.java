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
 * http://docs.opencv.org/modules/imgproc/doc/feature_detection.html
 * http://stackoverflow.com/questions/19270458/cvcalcopticalflowpyrlk-not-working-as-expected
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.opencv.opencv_core.AbstractCvScalar;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_tracking.TrackerCSRT;
import org.bytedeco.opencv.opencv_tracking.TrackerKCF;
import org.bytedeco.opencv.opencv_video.Tracker;
import org.bytedeco.opencv.opencv_video.TrackerGOTURN;
import org.bytedeco.opencv.opencv_video.TrackerMIL;
import org.bytedeco.opencv.opencv_video.TrackerNano;
import org.bytedeco.opencv.opencv_video.TrackerVit;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.slf4j.Logger;

/**
 * Bounding-box object tracker. CSRT/KCF/MIL/GOTURN plus OpenCV 4.x DNN
 * trackers Nano and ViT (ONNX from the zoo, downloaded on demand).
 *
 * @author kwatters
 *
 */
public class OpenCVFilterTracker extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  private final static Logger log = LoggerFactory.getLogger(OpenCVFilterTracker.class);

  static {
    Loader.load(opencv_video.class);
  }

  public static final String[] TRACKER_TYPES = { "CSRT", "KCF", "MIL", "GOTURN", "Nano", "Vit" };

  // The current tracker and it's associated boundingBox
  private Tracker tracker;
  // private Rect2d boundingBox;
  private Rect boundingBox;

  // configure these to set the initial box size.
  // public int boxWidth = 224;
  // public int boxHeight = 224;

  // TODO: is there a way to dynamically adjust what this should be?! That'd be
  // cool..
  public int boxWidth = 25;
  public int boxHeight = 25;

  // TODO: i'm not sure there is really a performance difference here..
  public boolean blackAndWhite = false;
  // CSRT, KCF, MIL, GOTURN, Nano, Vit
  public String trackerType = "CSRT";

  public boolean autoDownloadModel = true;

  public String modelStatus = "";

  public String loadedModelPath = "";

  // The current mat that is being processed.
  private Mat mat = null;

  // To hold x,y,w,h
  int[] points = new int[4];

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterTracker() {
    super();
  }

  public OpenCVFilterTracker(String name) {
    super(name);
  }

  private IplImage makeGrayScale(IplImage image) {
    IplImage imageBW = AbstractIplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    return imageBW;
  }

  @Override
  public IplImage process(IplImage image) {
    // TODO: I suspect this would be faster if we cut color first.
    // cvCutColor()

    if (blackAndWhite) {
      IplImage imageBw = makeGrayScale(image);
      // frame = converter.toFrame(imageBw);
      mat = converter.toMat(imageBw);
    } else {
      // frame = converter.toFrame(image);
      mat = converter.toMat(image);
    }
    if (boundingBox != null && tracker != null) {
      // log.info("Yes ! Bounding box : {} {} {} {} " , boundingBox.x(),
      // boundingBox.y(), boundingBox.width()
      // ,boundingBox.height());
      synchronized (tracker) {
        tracker.update(mat, boundingBox);
      }
      // boundingBox.x()
      int x0 = (boundingBox.x());
      int y0 = (boundingBox.y());
      int x1 = x0 + (boundingBox.width());
      int y1 = y0 + (boundingBox.height());
      // log.info("Drawing {} {} -- {} {}", x0,y0,x1,y1);
      cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), AbstractCvScalar.RED, 1, 1, 0);

      ArrayList<Point2df> pointsToPublish = new ArrayList<Point2df>();
      float xC = boundingBox.x() + boundingBox.width() / 2;
      float yC = boundingBox.y() + boundingBox.height() / 2;
      Point2df center = new Point2df(xC, yC);
      pointsToPublish.add(center);
      data.put("TrackingPoints", pointsToPublish);

    }

    return image;
  }

  @Override
  public void release() {
    // TODO Auto-generated method stub
    super.release();
    converter.close();
  }

  private Tracker createTracker(String trackerType) {
    String type = trackerType == null ? "CSRT" : trackerType.trim();
    try {
      if (type.equalsIgnoreCase("CSRT")) {
        return TrackerCSRT.create();
      } else if (type.equalsIgnoreCase("GOTURN")) {
        return TrackerGOTURN.create();
      } else if (type.equalsIgnoreCase("KCF")) {
        return TrackerKCF.create();
      } else if (type.equalsIgnoreCase("MIL")) {
        return TrackerMIL.create();
      } else if (type.equalsIgnoreCase("Nano") || type.equalsIgnoreCase("TrackerNano")) {
        Tracker nano = createNanoTracker();
        if (nano != null) {
          return nano;
        }
        log.warn("Nano tracker models unavailable, falling back to CSRT");
        modelStatus = "Nano models missing, using CSRT";
        return TrackerCSRT.create();
      } else if (type.equalsIgnoreCase("Vit") || type.equalsIgnoreCase("ViT") || type.equalsIgnoreCase("TrackerVit")) {
        Tracker vit = createVitTracker();
        if (vit != null) {
          return vit;
        }
        log.warn("ViT tracker model unavailable, falling back to CSRT");
        modelStatus = "ViT model missing, using CSRT";
        return TrackerCSRT.create();
      }
    } catch (Exception e) {
      log.warn("createTracker {} failed, using CSRT", type, e);
      modelStatus = type + " failed: " + e.getMessage();
    }
    log.warn("Unknown Tracker Algorithm {} defaulting to CSRT", trackerType);
    return TrackerCSRT.create();
  }

  private Tracker createNanoTracker() throws Exception {
    java.io.File backbone = OpenCvZooModels.resolve(OpenCvZooModels.NANOTRACK_BACKBONE, null, autoDownloadModel);
    java.io.File head = OpenCvZooModels.resolve(OpenCvZooModels.NANOTRACK_HEAD, null, autoDownloadModel);
    if (backbone == null || head == null) {
      return null;
    }
    TrackerNano.Params params = new TrackerNano.Params();
    BytePointer bb = new BytePointer(backbone.getAbsolutePath());
    BytePointer hh = new BytePointer(head.getAbsolutePath());
    params.backbone(bb);
    params.neckhead(hh);
    TrackerNano nano = TrackerNano.create(params);
    loadedModelPath = backbone.getAbsolutePath();
    modelStatus = "Nano loaded";
    log.info("TrackerNano loaded backbone={} head={}", backbone, head);
    return nano;
  }

  private Tracker createVitTracker() throws Exception {
    java.io.File netFile = OpenCvZooModels.resolve(OpenCvZooModels.VITTRACK, null, autoDownloadModel);
    if (netFile == null) {
      return null;
    }
    TrackerVit.Params params = new TrackerVit.Params();
    BytePointer netPath = new BytePointer(netFile.getAbsolutePath());
    params.net(netPath);
    TrackerVit vit = TrackerVit.create(params);
    loadedModelPath = netFile.getAbsolutePath();
    modelStatus = "ViT loaded";
    log.info("TrackerVit loaded {}", netFile);
    return vit;
  }

  public String installTrackerModels() {
    try {
      String type = trackerType == null ? "" : trackerType.trim();
      if (type.equalsIgnoreCase("Nano") || type.equalsIgnoreCase("TrackerNano")) {
        java.io.File backbone = OpenCvZooModels.resolve(OpenCvZooModels.NANOTRACK_BACKBONE, null, true);
        java.io.File head = OpenCvZooModels.resolve(OpenCvZooModels.NANOTRACK_HEAD, null, true);
        if (backbone == null || head == null) {
          modelStatus = "Nano download failed";
          return null;
        }
        loadedModelPath = backbone.getAbsolutePath();
        modelStatus = "Nano cached";
        broadcastFilterState();
        return loadedModelPath;
      }
      if (type.equalsIgnoreCase("Vit") || type.equalsIgnoreCase("ViT") || type.equalsIgnoreCase("TrackerVit")) {
        java.io.File netFile = OpenCvZooModels.resolve(OpenCvZooModels.VITTRACK, null, true);
        if (netFile == null) {
          modelStatus = "ViT download failed";
          return null;
        }
        loadedModelPath = netFile.getAbsolutePath();
        modelStatus = "ViT cached";
        broadcastFilterState();
        return loadedModelPath;
      }
      modelStatus = type + " has no ONNX weights";
      broadcastFilterState();
      return null;
    } catch (Exception e) {
      modelStatus = "download failed: " + e.getMessage();
      log.warn("tracker model install failed", e);
      return null;
    }
  }

  public void samplePoint(Float x, Float y) {
    samplePoint((int) (x * width), (int) (y * height));
  }

  @Override
  public void samplePoint(Integer x, Integer y) {
    // TODO: implement a state machine where you select the first corner. then
    // you select the second corner
    // that would define the size of the bounding box also.
    boundingBox = new Rect(x - boxWidth / 2, y - boxHeight / 2, boxWidth, boxHeight);
    log.info("Create bounding box for tracking x:{} y:{} w:{} h:{}", boundingBox.x(), boundingBox.y(), boundingBox.width(), boundingBox.height());
    // TODO: start tracking multiple points ?
    // the tracker will initialize on the next frame.. (I know , I know. it'd be
    // better to have the current frame and do the
    // initialization here.)
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
    tracker = createTracker(trackerType);
    log.info("Created tracker {}", trackerType);
    // TODO: I'm worried about thread safety with the "mat" object.
    if (mat != null) {
      // TODO: what happens if we're already initalized?
      synchronized (tracker) {
        tracker.init(mat, boundingBox);
      }
      log.info("Initialized tracker");
    } else {
      log.warn("Sample point called on a null mat.");
    }
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  public int getBoxWidth() {
    return boxWidth;
  }

  public void setBoxWidth(int boxWidth) {
    this.boxWidth = boxWidth;
  }

  public int getBoxHeight() {
    return boxHeight;
  }

  public void setBoxHeight(int boxHeight) {
    this.boxHeight = boxHeight;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
