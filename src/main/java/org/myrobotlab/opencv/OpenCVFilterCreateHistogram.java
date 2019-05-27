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

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgproc.cvCalcHist;
import static org.bytedeco.opencv.helper.opencv_imgproc.cvCreateHist;
import static org.bytedeco.opencv.global.opencv_core.CV_HIST_ARRAY;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvSplit;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.CvHistogram;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.IplImageArray;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterCreateHistogram extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterCreateHistogram.class);

  int numberOfBins = 255;
  IplImage channel0 = null;
  IplImage channel1 = null;
  IplImage channel2 = null;

  /**
   * creates a histogram in seperate channels TODO - display different channels
   * TODO -
   * 
   * @param name
   */
  public OpenCVFilterCreateHistogram(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  private IplImageArray splitChannels(IplImage hsvImage) {
    CvSize size = hsvImage.cvSize();
    int depth = hsvImage.depth();
    channel0 = cvCreateImage(size, depth, 1);
    channel1 = cvCreateImage(size, depth, 1);
    channel2 = cvCreateImage(size, depth, 1);
    cvSplit(hsvImage, channel0, channel1, channel2, null);
    return new IplImageArray(channel0, channel1, channel2);
  }

  @Override
  public IplImage process(IplImage image) {

    IplImage hsvImage = cvCreateImage(image.cvSize(), image.depth(), image.nChannels());
    cvCvtColor(image, hsvImage, CV_BGR2HSV);
    // Split the 3 channels into 3 images
    IplImageArray hsvChannels = splitChannels(hsvImage);
    // bins and value-range

    float minRange = 0f;
    float maxRange = 180f;
    // Allocate histogram object
    int dims = 1;
    int[] sizes = new int[] { numberOfBins };
    int histType = CV_HIST_ARRAY;
    float[] minMax = new float[] { minRange, maxRange };
    float[][] ranges = new float[][] { minMax };
    int uniform = 1;
    CvHistogram hist = cvCreateHist(dims, sizes, histType, ranges, uniform);
    // Compute histogram
    int accumulate = 1;

    cvCalcHist(hsvChannels.position(0), hist, accumulate, null);
    return channel0;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
