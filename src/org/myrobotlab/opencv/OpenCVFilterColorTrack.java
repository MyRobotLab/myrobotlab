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
import static org.bytedeco.javacpp.opencv_core.cvInRangeS;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterColorTrack extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterColorTrack.class.getCanonicalName());

  transient IplImage hsv = null;
  transient IplImage hue = null;
  transient IplImage thresholded = null;
  transient IplImage thresholded2 = null;
  transient IplImage value = null;
  transient IplImage saturation = null;
  transient IplImage mask = null;

  CvScalar hsv_min = null;
  CvScalar hsv_max = null;
  CvScalar hsv_min2 = null;
  CvScalar hsv_max2 = null;

  transient BufferedImage frameBuffer = null;

  public OpenCVFilterColorTrack() {
    super();
  }

  public OpenCVFilterColorTrack(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  /*
   * 
   * public void samplePoint(MouseEvent event) {
   * 
   * frameBuffer = hsv.getBufferedImage(); int rgb =
   * frameBuffer.getRGB(event.getX(), event.getY()); Color c = new Color(rgb);
   * log.error(event.getX() + "," + event.getY() + " h " + c.getRed() + " s " +
   * c.getGreen() + " v " + c.getBlue()); }
   */

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    // what can you expect? nothing? - if data != null then error?

    if (hsv == null) {
      hsv = cvCreateImage(cvGetSize(image), 8, 3);
      hue = cvCreateImage(cvGetSize(image), 8, 1);
      value = cvCreateImage(cvGetSize(image), 8, 1);
      saturation = cvCreateImage(cvGetSize(image), 8, 1);

      thresholded = cvCreateImage(cvGetSize(image), 8, 1);
      thresholded2 = cvCreateImage(cvGetSize(image), 8, 1);

      mask = cvCreateImage(cvGetSize(image), 8, 1);

      hsv_min = cvScalar(0, 50, 170, 0);
      hsv_max = cvScalar(10, 180, 256, 0);
      hsv_min2 = cvScalar(170, 50, 170, 0);
      hsv_max2 = cvScalar(256, 180, 256, 0);
    }

    // / THIS BECOMES V S H !!!

    /****
     * GREEN LED **** hsv_min = cvScalar(80, 0, 250, 0); hsv_max = cvScalar(90,
     * 40, 256, 0);
     */

    /***
     * LASER - TRY 1 hsv_min = cvScalar(8, 0, 210, 0); hsv_max = cvScalar(18,
     * 255, 255, 0);
     * 
     * 
     * On White 481,217 h 0 s 90 v 214 481,216 h 6 s 57 v 255
     */

    hsv_min = cvScalar(0, 0, 145, 0);
    hsv_max = cvScalar(38, 255, 255, 0);

    if (image == null) {
      log.error("image is null");
    }

    // convert BGR to HSV
    cvCvtColor(image, hsv, CV_BGR2HSV);
    cvInRangeS(hsv, hsv_min, hsv_max, thresholded);
    // cxcore.cvInRangeS(hsv, hsv_min2, hsv_max2,
    // thresholded2);
    // cxcore.cvOr(thresholded, thresholded2, thresholded, null);

    return thresholded;

  }

  public void samplePoint(Integer x, Integer y) {

    frameBuffer = OpenCV.IplImageToBufferedImage(hsv);
    int rgb = frameBuffer.getRGB(x, y);
    Color c = new Color(rgb);
    log.error(x + "," + y + " h " + c.getRed() + " s " + c.getGreen() + " v " + c.getBlue());
  }

}
