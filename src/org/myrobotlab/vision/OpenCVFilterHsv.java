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

/*
 *  HSV changes in OpenCV -
 *  https://code.ros.org/trac/opencv/ticket/328 H is only 1-180
 *  H <- H/2 (to fit to 0 to 255)
 *  
 *  CV_HSV2BGR_FULL uses full 0 to 255 range
 */
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.awt.Graphics;
import java.nio.ByteBuffer;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterHsv extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHsv.class.getCanonicalName());

  transient IplImage hsv = null;
  transient IplImage hue = null;
  transient IplImage value = null;
  transient IplImage saturation = null;
  transient IplImage mask = null;

  int x = 0;
  int y = 0;
  int clickCounter = 0;
  int frameCounter = 0;
  Graphics g = null;
  String lastHexValueOfPoint = "";

  transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN);

  public OpenCVFilterHsv() {
    super();
  }

  public OpenCVFilterHsv(String name) {
    super(name);
  }

  @Override
  public IplImage display(IplImage image, VisionData data) {

    ++frameCounter;
    if (x != 0 && clickCounter % 2 == 0) {

      if (frameCounter % 10 == 0) {
        // frameBuffer = hsv.getBufferedImage(); // TODO - ran out of
        // memory here
        ByteBuffer buffer = image.getByteBuffer();
        int index = y * image.widthStep() + x * image.nChannels();
        // Used to read the pixel value - the 0xFF is needed to cast
        // from
        // an unsigned byte to an int.
        int value = buffer.get(index) & 0xFF;
        lastHexValueOfPoint = Integer.toHexString(value & 0x00ffffff);
      }

      cvPutText(image, lastHexValueOfPoint, cvPoint(x, y), font, CvScalar.BLACK);
    }

    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
    hsv = IplImage.createCompatible(image);
  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    // CV_BGR2HSV_FULL - uses full 0-255 vs 0-180
    // CV_HSV2BGR_FULL
    cvCvtColor(image, hsv, CV_RGB2HSV);

    // cvSetImageCOI( hsv, 1);
    // cvCopy(hsv, hue );

    /*
     * http://cgi.cse.unsw.edu.au/~cs4411/wiki/index.php?title=OpenCV_Guide#
     * Calculating_color_histograms //Split out hue component and store in hue
     * cxcore.cvSplit(hsv, hue, null, null, null);
     */

    return hsv;

  }

  public void samplePoint(Integer inX, Integer inY) {
    ++clickCounter;
    x = inX;
    y = inY;

  }

}
