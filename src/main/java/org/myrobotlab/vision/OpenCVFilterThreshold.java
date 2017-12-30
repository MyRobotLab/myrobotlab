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

package org.myrobotlab.vision;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterThreshold extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterThreshold.class);
  transient IplImage gray = null;

  public float lowThreshold = 0.0f;
  public float highThreshold = 256.0f;

  public OpenCVFilterThreshold() {
    super();
  }

  public OpenCVFilterThreshold(String name) {
    super(name);
  }

  /*
   * @Override public void loadDefaultConfiguration() { cfg.set("lowThreshold",
   * 130.0f); cfg.set("highThreshold", 255.0f); }
   */
  /*
   * Threshold Applies fixed-level threshold to array elements
   * 
   * void cvThreshold( const CvArr* src, CvArr* dst, double threshold, double
   * maxValue, int thresholdType ); src Source array (single-channel, 8-bit of
   * 32-bit floating point). dst Destination array; must be either the same type
   * as src or 8-bit. threshold Threshold value. maxValue Maximum value to use
   * with CV_THRESH_BINARY, CV_THRESH_BINARY_INV, and CV_THRESH_TRUNC
   * thresholding types. thresholdType Thresholding type (see the discussion)
   * The function cvThreshold applies fixed-level thresholding to single-channel
   * array. The function is typically used to get bi-level (binary) image out of
   * grayscale image or for removing a noise, i.e. filtering out pixels with too
   * small or too large values. There are several types of thresholding the
   * function supports that are determined by thresholdType:
   * 
   * thresholdType=CV_THRESH_BINARY: dst(x,y) = maxValue, if src(x,y)>threshold
   * 0, otherwise
   * 
   * thresholdType=CV_THRESH_BINARY_INV: dst(x,y) = 0, if src(x,y)>threshold
   * maxValue, otherwise
   * 
   * thresholdType=CV_THRESH_TRUNC: dst(x,y) = threshold, if src(x,y)>threshold
   * src(x,y), otherwise
   * 
   * thresholdType=CV_THRESH_TOZERO: dst(x,y) = src(x,y), if (x,y)>threshold 0,
   * otherwise
   * 
   * thresholdType=CV_THRESH_TOZERO_INV: dst(x,y) = 0, if src(x,y)>threshold
   * src(x,y), otherwise
   */

  @Override
  public void imageChanged(IplImage image) {
    gray = cvCreateImage(cvGetSize(image), 8, CV_THRESH_BINARY);
  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    // CV_THRESH_BINARY
    // CV_THRESH_BINARY_INV
    // CV_THRESH_TRUNC
    // CV_THRESH_TOZERO
    // CV_THRESH_TOZERO_INV

    // cxcore.cvSetImageCOI(image, 1);

    // http://www710.univ-lyon1.fr/~bouakaz/OpenCV-0.9.5/docs/ref/OpenCVRef_ImageProcessing.htm
    cvThreshold(image, image, lowThreshold, highThreshold, CV_THRESH_BINARY);

    // must be gray for adaptive
    /*
     * cv.cvCvtColor( image, gray, cv.CV_BGR2GRAY );
     * cv.cvAdaptiveThreshold(gray, gray, 255, cv.CV_ADAPTIVE_THRESH_MEAN_C,
     * CV_THRESH_BINARY, 7,30);
     */
    return image;
  }

}
