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
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrDown;

import java.awt.Graphics;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterKinectDepth extends OpenCVFilter {

  // useful data for the kinect is 632 X 480 - 8 pixels on the right edge are
  // not good data
  // http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectDepth.class.getCanonicalName());

  int filter = 7;
  boolean createMask = false;

  transient IplImage dst = null;
  transient IplImage src = null;
  transient IplImage mask = null;

  int x = 0;
  int y = 0;
  int clickCounter = 0;

  boolean displayCamera = false;

  public OpenCVFilterKinectDepth() {
    super();
  }

  public OpenCVFilterKinectDepth(String name) {
    super(name);
  }
  
  public void setDisplayCamera(boolean b) {
    displayCamera = b;
  }

  public void createMask() {
    createMask = true;
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {

    // INFO - This filter has 2 sources !!!
    IplImage kinectDepth = data.get(OpenCV.SOURCE_KINECT_DEPTH);

    
    boolean processDepth = false;
    if (kinectDepth != null && processDepth) {

      // allowing publish & fork
      if (dst == null || dst.width() != image.width() || dst.nChannels() != image.nChannels()) {
        dst = cvCreateImage(cvSize(kinectDepth.width() / 2, kinectDepth.height() / 2), kinectDepth.depth(), kinectDepth.nChannels());
      }

      cvPyrDown(kinectDepth, dst, filter);
      invoke("publishDisplay", "kinectDepth", OpenCV.IplImageToBufferedImage(dst));
    }
    // end fork
    
    if (displayCamera) {
      return image;
    }

    return kinectDepth;

    /*
     * // check for depth ! 1 ch 16 depth - if not format error & return if
     * (image.nChannels() != 1 || image.depth() != 16) { log.error(
     * "image is not a kinect depth image"); return image; }
     * 
     * if (dst == null) { //dst = cvCreateImage(cvSize(image.width(),
     * image.height()), image.depth(),image.nChannels()); //dst =
     * cvCreateImage(cvSize(image.width(), image.height()), 8, 1); src =
     * cvCreateImage(cvSize(image.width(), image.height()), 8, 1); dst =
     * cvCreateImage(cvSize(image.width(), image.height()), 8, 1); }
     * 
     * cvConvertScale(image, src, 1, 0); //cvThreshold(dst, dst, 30, 255,
     * CV_THRESH_BINARY);
     * 
     * CvScalar min = cvScalar(30000, 0.0, 0.0, 0.0); CvScalar max =
     * cvScalar(150000, 0.0, 0.0, 0.0);
     * 
     * cvInRangeS(image, min, max, dst);
     * 
     * createMask = true; if (createMask) { if (mask == null) { mask =
     * cvCreateImage(cvSize(image.width(), image.height()), 8, 1); } cvCopy(dst,
     * mask, null); myService.setMask(this.getName(), mask); createMask = false;
     * } //cvCvtColor /* ByteBuffer source = image.getByteBuffer(); int z =
     * source.capacity(); ByteBuffer destination = dst.getByteBuffer(); z =
     * destination.capacity();
     * 
     * int depth = 0;
     * 
     * Byte b = 0xE; int max = 0;
     * 
     * for (int i=0; i<image.width()*image.height(); i++) {
     * 
     * depth = source.get(i) & 0xFF; depth <<= 8; depth = source.get(i+1) &
     * 0xFF; if (depth > max) max = depth;
     * 
     * if (depth > 100 && depth < 400) { destination.put(i, b); } }
     */

    // return dst;
  }

  public void samplePoint(Integer inX, Integer inY) {
    ++clickCounter;
    x = inX;
    y = inY;

  }

}
