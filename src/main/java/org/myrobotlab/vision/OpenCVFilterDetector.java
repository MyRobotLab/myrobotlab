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

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractor;
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorMOG2;
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

    Pointer pointer = new Pointer();
    
    //mog = new BackgroundSubtractorMOG2(history, threshold, shadowDetection);
    // TODO: test this..
    mog = new BackgroundSubtractorMOG2(pointer);
    
  }

  public void learn() {
    learningRate = -1;
  }

  @Override
  public IplImage process(IplImage image, VisionData data) {
    // constructor changed to require Mat in javacv 0.10
    // mog.app
    mog.apply(new Mat(image), new Mat(foreground), learningRate); // 0 trigger
                                                                  // || -1 learn
                                                                  // and
    return foreground;
  }

  public void search() {
    learningRate = 0;
  }

}
