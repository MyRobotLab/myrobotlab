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

import static org.bytedeco.opencv.global.opencv_core.CV_TERMCRIT_EPS;
import static org.bytedeco.opencv.global.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cornerSubPix;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.goodFeaturesToTrack;
import static org.bytedeco.opencv.global.opencv_video.calcOpticalFlowPyrLK;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.myrobotlab.cv.TrackingPoint;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point;
import org.slf4j.Logger;

/**
 * 
 * TODO - now TrackingPoints can be "id" so the entire lifecycle of the point
 * can be followed. This should allow for more intelligent tracking strategies
 * in the future
 * 
 * <pre>
 * Future API ============
 * setRoi(x,y, width, height)
 * addPoint(rect) - in Roi goodfeature
 * addPoint(x, y)
 * resetPoint(id, x, y)
 * removePoint(id)
 * removeLostPoints()
 * clearPoints
 * 
 * </pre>
 * 
 * @author GroG
 *
 */
public class OpenCVFilterLKOpticalTrack extends OpenCVFilter {

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterLKOpticalTrack.class);
  private static final long serialVersionUID = 1L;
  public boolean addRemovePoint2dfPoint = false;
  protected boolean getSubPixels = false;
  public boolean clearPoints = false;
  protected int maxPointCnt = 50;
  // protected double quality = 0.05;
  protected double quality = 5; // percent quality
  protected int blockSize = 3;
  protected double minDistance = 5.0;
  public boolean needTrackingPoints = false;
  protected List<Point> pointsToPublish = new ArrayList<>();
  protected boolean printCount = true;
  protected Point samplePoint = null;
  protected Map<Integer, TrackingPoint> trackingPoints = new HashMap<>();
  /**
   * name or id of the point to track in current index of points
   */
  protected Map<String, Integer> nameToIndex = new HashMap<>();
  protected int winSize = 15;
  protected long currentPntCnt;
  // TODO: can i just create a new Mat instead of having to convert an IplImage
  // to a mat first?!
  transient Mat zeroPoints = null;
  transient Mat cornersA = null;
  transient Mat cornersB = null;
  transient Mat featureErrors = null;
  transient Mat featuresFound = null;
  transient IplImage grayImgA = null;
  transient IplImage grayImgB = null;
  transient Mat matA = null;
  transient Mat matB = null;
  // 0-based maximal pyramid level number; if set to 0, pyramids are not used
  // (single level), if set to 1, two levels are used, and so on; if pyramids
  // are passed to input then algorithm will use as many levels as pyramids have
  // but no more than maxLevel.
  protected int maxLevel = 5;
  transient private CloseableFrameConverter converter1 = new CloseableFrameConverter();
  transient private CloseableFrameConverter converter2 = new CloseableFrameConverter();
  transient private CloseableFrameConverter converter3 = new CloseableFrameConverter();
  transient private CloseableFrameConverter converter4 = new CloseableFrameConverter();

  public OpenCVFilterLKOpticalTrack() {
    this(null);
  }

  public OpenCVFilterLKOpticalTrack(String name) {
    super(name);

    zeroPoints = converter1.toMat(AbstractIplImage.create(new CvSize().width(0).height(0), 32, 2));
    cornersA = zeroPoints;
  }

  public String addPoint(int x, int y) {
    return addPoint(null, x, y);
  }

  public String addPoint(String id, int x, int y) {

    maxPointCnt++;

    if (id == null) {
      id = String.format("%d", maxPointCnt);
    }

    cornersA = resize(cornersA, 1);

    FloatIndexer idx = cornersA.createIndexer();

    idx.put(0, idx.size(0) - 1, 0, x);
    idx.put(0, idx.size(0) - 1, 1, y);
    idx.release();

    return id;
  }

  public Mat resize(Mat toResize, int amount) {

    FloatIndexer idx = toResize.createIndexer();
    CvSize sz = new CvSize();
    sz.width(1).height((int) idx.size(0) + amount);
    Mat tmp = converter2.toMat(AbstractIplImage.create(sz, 32, 2));
    FloatIndexer newIdx = tmp.createIndexer();
    // copy contents
    for (int i = 0; i < idx.size(0); i++) {
      newIdx.put(0, i, 0, idx.get(0, i, 0));
      newIdx.put(0, i, 1, idx.get(0, i, 1));
      log.info("here");
    }
    toResize.release();
    newIdx.release();
    idx.release();

    return tmp;
  }

  @Override
  public void imageChanged(IplImage image) {

    grayImgA = AbstractIplImage.create(image.cvSize(), 8, 1);
    grayImgB = AbstractIplImage.create(image.cvSize(), 8, 1);

    if (channels == 3) {
      cvCvtColor(image, grayImgB, CV_BGR2GRAY);
      cvCopy(grayImgB, grayImgA);
    }
  }

  public void printCorners(Mat a) {
    FloatIndexer idx = a.createIndexer();
    StringBuilder sb = new StringBuilder();

    // copy contents
    for (int i = 0; i < idx.size(0); i++) {
      sb.append(String.format("(%d,%d)", (int) idx.get(0, i, 0), (int) idx.get(0, i, 1)));
    }
    idx.release();
    log.info(sb.toString());
  }

  public void printDirections(List<TrackingPoint> dir) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < dir.size(); ++i) {
      TrackingPoint d = dir.get(i);
      sb.append(String.format("%03d,%03d->%03d,%03d|", d.p0.x, d.p0.y, d.p1.x, d.p1.y));
    }
    log.info("{}", sb);
  }

  @Override
  public IplImage process(IplImage image) {
    if (channels == 3) {
      cvCvtColor(image, grayImgB, CV_BGR2GRAY);
    } else {
      grayImgB = image;
    }

    // load 1st prev image - must have at least 2 images
    if (matA == null) {
      matA = converter3.toMat(grayImgA);
      return image;
    }

    // current image
    matB = converter4.toMat(grayImgB);

    if (samplePoint != null) {
      addPoint(samplePoint.x, samplePoint.y);
      samplePoint = null;
    }

    if (needTrackingPoints) {
      cornersA.release();
      cornersA = new Mat();
      goodFeaturesToTrack(matA, cornersA, maxPointCnt, quality / 100, minDistance, null, blockSize, false, 0.04);
      needTrackingPoints = false;

      if (getSubPixels) {
        cornerSubPix(matA, cornersA, new Size(winSize, winSize), new Size(-1, -1), new TermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
      }
    }

    if (clearPoints) {
      cornersA.release();
      cornersA = zeroPoints;
      clearPoints = false;
      trackingPoints.clear();
      pointsToPublish.clear();
    }

    if (cornersA.address() == 0) {
      // No corners! null matrix!!
      return image;
    }

    FloatIndexer cornersAidx = cornersA.createIndexer();
    if (cornersAidx.size(0) == 0) {
      // no requested tracking points
      return image;
    }

    featuresFound = new Mat();
    featureErrors = new Mat();
    cornersB = new Mat();

    // FIXME - is featuresFound input, output, or both ???
    // calcOpticalFlowPyrLK(imgA, imgB, cornersA, cornersB, featuresFound,
    // featureErrors);

    if (currentPntCnt != cornersAidx.size(0)) {
      printCorners(cornersA);
      currentPntCnt = cornersAidx.size(0);
    }

    // OPTFLOW_USE_INITIAL_FLOW = 4
    // OPTFLOW_LK_GET_MIN_EIGENVALS = 8

    calcOpticalFlowPyrLK(matA, matB, cornersA, cornersB, featuresFound, featureErrors, new Size(winSize, winSize), maxLevel,
        new TermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3), 0, 1e-4);

    // create publishing containers
    pointsToPublish = new ArrayList<>();
    trackingPoints.clear();

    FloatIndexer cornersBidx = cornersB.createIndexer();
    UByteIndexer featuresFoundIdx = featuresFound.createIndexer();
    FloatIndexer featureErrorsIdx = featureErrors.createIndexer();

    for (int i = 0; i < cornersAidx.size(0); i++) {
      // FIXME - send errors too
      if (featuresFoundIdx.get(i) == 0 || featureErrorsIdx.get(i) > 550) {
        // FIXME - pruning and MOST IMPORTANTLY identifying points (id) !!! with
        // printed error index
        // System.out.println("Error is " + feature_errors_idx.get(i) + "/n");
        // continue;

      }
      TrackingPoint direction = new TrackingPoint(i, Math.round(cornersAidx.get(0, i, 0)), Math.round(cornersAidx.get(0, i, 1)), Math.round(cornersBidx.get(0, i, 0)),
          Math.round(cornersBidx.get(0, i, 1)));

      direction.found = featuresFoundIdx.get(i);
      direction.error = featureErrorsIdx.get(i);
      pointsToPublish.add(direction.p1);
      nameToIndex.put(direction.id, i);
      trackingPoints.put(i, direction);
    }

    // Set the tracking points in the cv data output
    if (!trackingPoints.isEmpty()) {
      data.put("points", trackingPoints);
    }
    // FIXME !!! - close all resources
    // releasing previous frame
    matA.release();
    cornersA.release();
    // cornersB.release();
    featuresFound.release();
    featureErrors.release();
    featuresFoundIdx.release();
    featureErrorsIdx.release();
    cornersAidx.release();
    cornersBidx.release();

    // shift to next image
    // prev = current
    // imgA = imgB;
    // imgA = imgB.clone();
    cvCopy(grayImgB, grayImgA);
    matA = new Mat(grayImgA);
    cornersA = cornersB;

    matB.release();

    return image;
  }

  @Override
  public void release() {
    super.release();
    converter1.close();
    converter2.close();
    converter3.close();
    converter4.close();
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    // FIXME - TODO - calculate avg direction !!!
    int notFound = 0;
    int errorCount = 0;

    // for (int i = 0; i < trackingPoints.size(); i++) {
    for (TrackingPoint point : trackingPoints.values()) {
      // TrackingPoint d = trackingPoints.get(i);
      int x0 = point.p0.x;
      int y0 = point.p0.y;
      int x1 = point.p1.x;
      int y1 = point.p1.y;

      graphics.setColor(Color.GREEN);
      graphics.drawLine(x0, y0, x1, y1);
      graphics.setColor(Color.RED);
      graphics.drawLine(x1, y1, x1, y1);
      graphics.fillArc(x1, y1, 3, 3, 0, 360);
      // graphics.drawString(String.format("%s %d %03f", point.id, point.found,
      // point.error), x1 + 10, y1 - 10);
      graphics.drawString(String.format("%s %d,%d %.02f", point.id, point.p1.x, point.p1.y, point.error), x1 + 10, y1 - 10);
      if (point.found == 0) {
        ++notFound;
      }
      if (point.error > 550) {
        ++errorCount;
      }
    }

    if (printCount) {
      graphics.drawString(String.format("points: %d errors: %d not found: %d", trackingPoints.size(), errorCount, notFound), 10, 10);
    }
    return image;
  }

  @Override
  public void samplePoint(Integer x, Integer y) {
    samplePoint = new Point(x, y);
  }

}
