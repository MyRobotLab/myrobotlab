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

import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.opencv.global.opencv_video.createBackgroundSubtractorMOG2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_video.BackgroundSubtractor;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public double learningRate = -1; // 0 trigger || -1 learn and fade

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterDetector.class.getCanonicalName());

  transient BackgroundSubtractor mog;
  transient IplImage foreground;

  public int history = 10;
  public float threshold = 128f;
  public boolean shadowDetection = false;

  public OpenCVFilterDetector() {
    super();
  }

  public OpenCVFilterDetector(String name) {
    super(name);
  }

  public OpenCVFilterDetector(String name, int history, float threshold, boolean shadowDetection) {
    super(name);
    this.history = history;
    this.threshold = threshold;
    this.shadowDetection = shadowDetection;
  }

  @Override
  public void imageChanged(IplImage image) {
    foreground = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, 1);
    mog = createBackgroundSubtractorMOG2();

  }

  public void learn() {
    learningRate = -1;
  }

  @Override
  public IplImage process(IplImage image) {
    // constructor changed to require Mat in javacv 0.10
    // mog.app
    // 0 trigger || -1 learn
    mog.apply(new Mat(image), new Mat(foreground), learningRate); // and
    return foreground;
  }

  public void search() {
    learningRate = 0;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
