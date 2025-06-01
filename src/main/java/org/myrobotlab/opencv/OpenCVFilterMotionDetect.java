package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.absdiff;
import static org.bytedeco.opencv.global.opencv_core.convertScaleAbs;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_imgproc.CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;
import static org.bytedeco.opencv.global.opencv_imgproc.RETR_EXTERNAL;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.accumulateWeighted;
import static org.bytedeco.opencv.global.opencv_imgproc.boundingRect;
import static org.bytedeco.opencv.global.opencv_imgproc.contourArea;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.dilate;
import static org.bytedeco.opencv.global.opencv_imgproc.findContours;
import static org.bytedeco.opencv.global.opencv_imgproc.morphologyDefaultBorderValue;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

/**
 * OpenCV Fitler for motion detection based on
 * https://www.pyimagesearch.com/2015/06/01/home-surveillance-and-motion-detection-with-the-raspberry-pi-python-and-opencv/
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterMotionDetect extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  private int minMotionFrames = 8;
  private double deltaThresh = 5;
  // TODO: need to resize the image to make this consistent
  public int minArea = 5000;
  private transient Mat avgMat = null;
  private String text = "Unoccupied";
  private ArrayList<Rect> rects = null;
  // how many subsequent frames detect motion
  private int motionCounter = 0;
  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterMotionDetect() {
    super();
  }

  public OpenCVFilterMotionDetect(String name) {
    super(name);
  }

  public OpenCVFilterMotionDetect(String filterName, String sourceKey) {
    super(filterName, sourceKey);
  }

  @Override
  public void imageChanged(IplImage image) {
    // NoOp
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // TODO: resize to a fixed width so the area is meaningful ?
    // # resize the frame, convert it to grayscale, and blur it
    // frame = imutils.resize(frame, width=500)
    // gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    IplImage gray = cvCreateImage(image.cvSize(), 8, 1);
    cvCvtColor(image, gray, CV_BGR2GRAY);

    Mat grayMat = converter.toMat(gray);
    // gray = cv2.GaussianBlur(gray, (21, 21), 0)
    GaussianBlur(grayMat, grayMat, new Size(21, 21), 0.0);
    // # if the average frame is None, initialize it
    // if avg is None:
    if (avgMat == null) {
      // print("[INFO] starting background model...")
      // avg = gray.copy().astype("float")
      // rawCapture.truncate(0)
      // continue
      avgMat = new Mat();
      grayMat.convertTo(avgMat, CV_32F);
      // This was our initialization method..
      log.info("init motion detection frame.");
      return image;
      // avgMat = grayMat.clone();
      // TODO: potentially continue here because we need to initialize the
      // frame?
      // maybe not.. maybe just create the avgMat and move on.
    }
    // # accumulate the weighted average between the current frame and
    // # previous frames, then compute the difference between the current
    // # frame and running average
    // cv2.accumulateWeighted(gray, avg, 0.5)
    accumulateWeighted(grayMat, avgMat, 0.5);
    // frameDelta = cv2.absdiff(gray, cv2.convertScaleAbs(avg))
    Mat absScaleMat = new Mat();
    convertScaleAbs(avgMat, absScaleMat);
    Mat frameDelta = new Mat();
    absdiff(grayMat, absScaleMat, frameDelta);
    // # threshold the delta image, dilate the thresholded image to fill
    // # in holes, then find contours on thresholded image
    // thresh = cv2.threshold(frameDelta, conf["delta_thresh"], 255,
    // cv2.THRESH_BINARY)[1]
    Mat threshMat = new Mat();
    threshold(frameDelta, threshMat, deltaThresh, 255, THRESH_BINARY);
    // thresh = cv2.dilate(thresh, None, iterations=2)
    Mat dilateMat = new Mat();
    Scalar defaultBorder = morphologyDefaultBorderValue();
    dilate(threshMat, dilateMat, new Mat(), new Point(-1, -1), 2, BORDER_CONSTANT, defaultBorder);
    // cnts = cv2.findContours(thresh.copy(), cv2.RETR_EXTERNAL,
    // cv2.CHAIN_APPROX_SIMPLE)
    MatVector cntsVec = new MatVector();
    findContours(threshMat, cntsVec, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    // cnts = imutils.grab_contours(cnts)
    boolean sawMovement = false;
    rects = new ArrayList<Rect>();
    for (Mat c : cntsVec.get()) {
      // # loop over the contours
      // for c in cnts:
      // # if the contour is too small, ignore it
      double contArea = contourArea(c);
      // if cv2.contourArea(c) < conf["min_area"]:
      // continue
      if (contArea < minArea) {
        continue;
      }
      // # compute the bounding box for the contour, draw it on the frame,
      // # and update the text
      // TODO: can i draw the actual contour instead of a rect?
      // (x, y, w, h) = cv2.boundingRect(c)
      Rect rect = boundingRect(c);
      rects.add(rect);
      sawMovement = true;
    }
    if (sawMovement) {
      text = "Occupied";
      motionCounter++;
    } else {
      // no motion detected in the current frame, reset the counter
      // TODO: publish stuffs.
      text = "Unoccupied";
      motionCounter = 0;
    }
    if (motionCounter >= minMotionFrames) {
      // TODO: add some additional stuff to the motion being detected
      invoke("publishMotionDetected", rects);
      // once we've published the detected motion, let's clear our motion
      // counter
      motionCounter = 0;
    }
    // return the original image.
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    // Draw the rects where motion was detected and the motion count
    if (rects != null) {
      for (Rect rect : rects) {
        int x = rect.x();
        int y = rect.y();
        int width = rect.width();
        int height = rect.height();
        graphics.setColor(Color.GREEN);
        graphics.drawRect(x, y, width, height);
      }
    }
    String label = text + " " + motionCounter;
    graphics.drawString(label, 20, 60);
    return image;
  }

  @Override
  public void release() {
    super.release();
    converter.close();
  }

}
