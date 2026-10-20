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
  /**
   * Catalog metadata for the WebGui filter guide. Static so it can be read
   * without constructing the filter (constructors may start services).
   */
  public static OpenCVFilterInfo catalogInfo() {
    return new OpenCVFilterInfo("Tracker",
        "Bounding-box object tracker. CSRT/KCF/MIL/GOTURN are built in; Nano and ViT download ONNX weights. Click the video to set the box.",
        "Start capture, add the filter, click the target. Choose tracker type in the WebGui. Enable auto-download for Nano/ViT. Re-click if tracking is lost.",
        "CSRT/KCF/MIL/GOTURN: none extra. Nano/ViT: ONNX files in data/OpenCV/zoo_models/.");
  }

  @Override
  public OpenCVFilterInfo getFilterInfo() {
    return catalogInfo();
  }


  private static final long serialVersionUID = 1L;
  private final static Logger log = LoggerFactory.getLogger(OpenCVFilterTracker.class);

  private static void loadNatives() {
    Loader.load(opencv_video.class);
  }

  public static final String[] TRACKER_TYPES = { "CSRT", "KCF", "MIL", "GOTURN", "Nano", "Vit" };

  /**
   * All Tracker JNI (create/init/update/close) must run on the video-processor
   * thread. WebGui clicks arrive on the OpenCV inbox thread; closing Nano/ViT
   * there while {@code update()} is in native code crashes the JVM
   * (EXCEPTION_ACCESS_VIOLATION writing a null C++ object).
   */
  transient private final Object trackerLock = new Object();

  // JNI / JavaCV pointers — must be transient so WebGui broadcastState()
  // does not try to Jackson-serialize NativeDeallocator (see FilterWrapper).
  transient private Tracker tracker;
  transient private Rect boundingBox;

  transient private Integer pendingSampleX;
  transient private Integer pendingSampleY;

  // Model-path pointers must outlive TrackerNano/TrackerVit; JavaCV BytePointer
  // locals are otherwise GC'd while native code still holds the char*.
  transient private BytePointer nanoBackbonePtr;
  transient private BytePointer nanoHeadPtr;
  transient private BytePointer vitNetPtr;

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
  transient private Mat mat = null;

  // To hold x,y,w,h
  int[] points = new int[4];

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterTracker() {
    super();
    loadNatives();
  }

  public OpenCVFilterTracker(String name) {
    super(name);
    loadNatives();
  }

  private IplImage makeGrayScale(IplImage image) {
    IplImage imageBW = AbstractIplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    return imageBW;
  }

  @Override
  public IplImage process(IplImage image) {
    loadNatives();
    // TODO: I suspect this would be faster if we cut color first.
    // cvCutColor()

    if (blackAndWhite) {
      IplImage imageBw = makeGrayScale(image);
      mat = converter.toMat(imageBw);
    } else {
      mat = converter.toMat(image);
    }

    boolean skipUpdate = false;
    synchronized (trackerLock) {
      if (pendingSampleX != null && pendingSampleY != null) {
        applySamplePoint(pendingSampleX, pendingSampleY, image);
        pendingSampleX = null;
        pendingSampleY = null;
        skipUpdate = true;
      }
      if (!skipUpdate && boundingBox != null && tracker != null) {
        try {
          tracker.update(mat, boundingBox);
        } catch (Exception e) {
          log.warn("tracker.update failed — dropping tracker", e);
          closeTracker();
        }
      }
    }

    if (boundingBox != null && tracker != null) {
      int x0 = boundingBox.x();
      int y0 = boundingBox.y();
      int x1 = x0 + boundingBox.width();
      int y1 = y0 + boundingBox.height();
      cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), AbstractCvScalar.RED, 1, 1, 0);

      ArrayList<Point2df> pointsToPublish = new ArrayList<Point2df>();
      float xC = boundingBox.x() + boundingBox.width() / 2.0f;
      float yC = boundingBox.y() + boundingBox.height() / 2.0f;
      Point2df center = new Point2df(xC, yC);
      pointsToPublish.add(center);
      data.put("TrackingPoints", pointsToPublish);
    }

    return image;
  }

  @Override
  public void release() {
    synchronized (trackerLock) {
      closeTracker();
      closePointer(nanoBackbonePtr);
      closePointer(nanoHeadPtr);
      closePointer(vitNetPtr);
      nanoBackbonePtr = null;
      nanoHeadPtr = null;
      vitNetPtr = null;
    }
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
    if (nanoBackbonePtr == null) {
      nanoBackbonePtr = new BytePointer(backbone.getAbsolutePath());
    }
    if (nanoHeadPtr == null) {
      nanoHeadPtr = new BytePointer(head.getAbsolutePath());
    }
    TrackerNano.Params params = new TrackerNano.Params();
    params.backbone(nanoBackbonePtr);
    params.neckhead(nanoHeadPtr);
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
    if (vitNetPtr == null) {
      vitNetPtr = new BytePointer(netFile.getAbsolutePath());
    }
    TrackerVit.Params params = new TrackerVit.Params();
    params.net(vitNetPtr);
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
    // Queue only. create/init/close run on the video thread in process().
    synchronized (trackerLock) {
      pendingSampleX = x;
      pendingSampleY = y;
    }
  }

  /**
   * Must be called on the video-processor thread with {@link #trackerLock} held.
   */
  private void applySamplePoint(int x, int y, IplImage image) {
    closeTracker();
    boundingBox = clampBox(x, y, image.width(), image.height());
    log.info("Create bounding box for tracking x:{} y:{} w:{} h:{}", boundingBox.x(), boundingBox.y(), boundingBox.width(),
        boundingBox.height());
    tracker = createTracker(trackerType);
    log.info("Created tracker {}", trackerType);
    if (mat == null || tracker == null) {
      log.warn("Sample point called with null mat or tracker.");
      return;
    }
    try {
      tracker.init(mat, boundingBox);
      log.info("Initialized tracker");
    } catch (Exception e) {
      log.warn("tracker.init failed", e);
      closeTracker();
    }
  }

  private Rect clampBox(int x, int y, int imgW, int imgH) {
    int w = Math.max(8, boxWidth);
    int h = Math.max(8, boxHeight);
    if (imgW > 0) {
      w = Math.min(w, imgW);
    }
    if (imgH > 0) {
      h = Math.min(h, imgH);
    }
    int x0 = x - w / 2;
    int y0 = y - h / 2;
    if (imgW > 0) {
      x0 = Math.max(0, Math.min(x0, imgW - w));
    } else {
      x0 = Math.max(0, x0);
    }
    if (imgH > 0) {
      y0 = Math.max(0, Math.min(y0, imgH - h));
    } else {
      y0 = Math.max(0, y0);
    }
    return new Rect(x0, y0, w, h);
  }

  private void closeTracker() {
    if (tracker == null) {
      return;
    }
    try {
      tracker.close();
    } catch (Exception e) {
      log.debug("tracker.close", e);
    }
    tracker = null;
  }

  private static void closePointer(BytePointer ptr) {
    if (ptr != null) {
      try {
        ptr.close();
      } catch (Exception ignored) {
      }
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
