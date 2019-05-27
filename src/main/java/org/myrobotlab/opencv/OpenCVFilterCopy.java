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
 * Enjoy !
 * 
 * */

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvResetImageROI;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterCopy extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterCopy.class.getCanonicalName());

  public OpenCVFilterCopy() {
    super();
  }

  public OpenCVFilterCopy(String name) {
    super(name);
  }

  /*
   * 
   * void getSubImg(IplImage* img, IplImage* subImg, CvRect roiRect) {
   * 
   * cvSetImageROI(img, roiRect); subImg = cvCreateImage(cvGetSize(img),
   * img->depth, img->nChannels); cvCopy(img, subImg, NULL);
   * cvResetImageROI(img); }
   */

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image) {
    IplImage copy = copy(image);
    put("copy", copy);
    return copy;
  }

  /**
   * default copy - location starts in 0,0 - overlap gets cropped
   * 
   * @param src
   * @param dst
   * @return
   */
  public IplImage copy(IplImage src, IplImage dst) {
    CvRect rect = getMinRoi(src, dst);
    IplImage ret = copyWithRoi(src, dst, 0, 0, rect.width(), rect.height());
    return ret;
  }

  public CvRect getMinRoi(IplImage src, IplImage dst) {
    int minWidth = (src.width() < dst.width()) ? src.width() : dst.width();
    int minHeight = (src.height() < dst.height()) ? src.height() : dst.height();
    CvRect rect = new CvRect(0, 0, minWidth, minHeight);
    return rect;
  }

  // FIXME - if dst == null - create src sized copy
  public IplImage copyWithRoi(IplImage src, IplImage dst, int roiX, int roiY, int roiWidth, int roiHeight) {
    CvRect roiRect = new CvRect(roiX, roiY, roiWidth, roiHeight);
    cvSetImageROI(dst, roiRect);

    // cvCopy(src, dst, null);
    cvCopy(src, dst);

    cvResetImageROI(dst);
    return dst;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
