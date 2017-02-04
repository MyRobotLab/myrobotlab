/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvCircle;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

import com.sun.jna.ptr.IntByReference;

public class OpenCVFilterGoodFeaturesToTrack extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterGoodFeaturesToTrack.class.getCanonicalName());

  transient IplImage grey = null;
  transient IplImage eig = null;
  transient IplImage temp = null;
  transient IplImage mask = null; // ROI

  public int maxPointCount = 46;
  public int totalIterations = 0;
  public boolean colorAgeOfPoint = true;

  // quality - Multiplier for the maxmin eigenvalue; specifies minimal
  // accepted quality of image corners
  public double qualityLevel = 0.05;
  // minDistance - Limit, specifying minimum possible distance between
  // returned corners; Euclidian distance is used
  public double minDistance = 10.0;
  // blockSize - Size of the averaging block, passed to underlying
  // cvCornerMinEigenVal or cvCornerHarris used by the function
  public int blockSize = 3;
  // If nonzero, Harris operator (cvCornerHarris) is used instead of default
  // cvCornerMinEigenVal.
  public int useHarris = 0;
  // Free parameter of Harris detector; used only if useHarris != 0
  public double k = 0.0;

  public Point2Df oldest = new Point2Df();

  public HashMap<String, Integer> stableIterations;

  int lastMaxPointCount = 0;
  transient IntByReference cornerCount = new IntByReference(maxPointCount);
  transient CvPoint2D32f corners = null;
  int[] count = { maxPointCount };

  // only valid for a "fixed" camera - need a new index to support camera
  // movement
  HashMap<String, Float> values = new HashMap<String, Float>();

  DecimalFormat df = new DecimalFormat("0.###");

  transient CvScalar color = new CvScalar();

  transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);

  public OpenCVFilterGoodFeaturesToTrack() {
    super();
  }

  public OpenCVFilterGoodFeaturesToTrack(String name) {
    super(name);
  }

  @Override
  public IplImage display(IplImage frame, OpenCVData data) {

    float gradient = 1 / oldest.value;
    int x, y;

    for (int i = 0; i < count[0]; ++i) {
      /*
       * since there is no subpixel selection - we don't need to round - we can
       * cast x = Math.round(corners.x()); y = Math.round(corners.y());
       */
      corners.position(i);
      x = (int) corners.x();
      y = (int) corners.y();

      if (colorAgeOfPoint) {
        String key = String.format("%d.%d", x, y);
        if (values.containsKey(key)) {
          float scale = (values.get(String.format("%d.%d", x, y)) * (gradient));
          if (scale == 1.0f) // grey
          {
            color = CvScalar.WHITE;
            // TODO - find what this is color

          } else {
            // WTF - I WANT AN HSV REPRESENTATION
            // color.setVal(3, scale*10);
            Color c = Color.getHSBColor(scale, 1.0f, 0.8f);

            color.red(c.getRed());
            color.blue(c.getBlue());
            color.green(c.getGreen());
            // graphics.setColor(new Color(Color.HSBtoRGB(scale,
            // 0.8f, 0.7f)));
            // CV_HSV2RGB(scale);
          }
          cvCircle(frame, cvPoint(x, y), 1, color, -1, 8, 0);
          cvPutText(frame, String.format("%s", df.format(scale)), cvPoint(x, y), font, color);

        } else {
          log.error(key); // FIXME FIXME FIXME ---- WHY THIS SHOULDN"T
          // HAPPEN BUT IT HAPPENS ALL THE TIME
        }
      }
      corners.position(i);
      // graphics.drawOval(x, y, 3, 1);
    }

    return frame; // TODO - ran out of memory here

  }

  @Override
  public void imageChanged(IplImage image) {
    grey = cvCreateImage(cvGetSize(image), 8, 1);
    eig = cvCreateImage(cvGetSize(grey), 32, 1);
    temp = cvCreateImage(cvGetSize(grey), 32, 1);

    stableIterations = new HashMap<String, Integer>();

  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    if (channels == 3) {
      grey = IplImage.create(imageSize, 8, 1);
      cvCvtColor(image, grey, CV_BGR2GRAY);
    } else {
      grey = image;
    }

    if (lastMaxPointCount != maxPointCount) {
      cornerCount.setValue(maxPointCount);
      count = new int[] { maxPointCount };

      lastMaxPointCount = maxPointCount;
    }

    corners = new CvPoint2D32f(maxPointCount); // FIXME copy dont create ?

    ++totalIterations;

    IntPointer countPointer = new IntPointer(count);
    cvGoodFeaturesToTrack(grey, eig, temp, corners, countPointer, qualityLevel, minDistance, mask, blockSize, useHarris, k);

    // FIXME - another sad data conversion :(
    ArrayList<Point2Df> points = new ArrayList<Point2Df>();
    Float value = null;
    int x, y;
    for (int i = 0; i < count[0]; ++i) {
      corners.position(i);
      x = (int) corners.x();
      y = (int) corners.y();
      // da[i] = da[i] / frame.width();
      // da[i + 1] = da[i + 1] / frame.height();
      // x = Math.round(a)
      String key = String.format("%d.%d", x, y);
      if (values.containsKey(key)) {
        value = values.get(key);
        ++value;
        values.put(key, value);
        // log.warn(value);
        // log.warn(values.get(key));
      } else {
        value = new Float(1f);
        values.put(key, value);
      }

      Point2Df np = null;

      if (useFloatValues) {
        np = new Point2Df((float) x / width, (float) y / height, value);
      } else {
        np = new Point2Df(x, y, value);
      }

      if (np.value > oldest.value) {
        oldest = np;
      }

      points.add(np);

    }

    data.set(points);

    return image;
  }

}
