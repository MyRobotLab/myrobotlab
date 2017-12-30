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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.vision;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterCanny extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterCanny.class.getCanonicalName());

  public int apertureSize = 5;
  public double lowThreshold = 0.0;
  public double highThreshold = 50.0;

  transient IplImage gray = null;
  transient IplImage inlines = null;

  public OpenCVFilterCanny() {
    super();
  }

  public OpenCVFilterCanny(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    if (image == null) {
      log.error("image is null");
    }

    if (gray == null) {
      gray = cvCreateImage(cvGetSize(image), 8, 1);
    }
    if (inlines == null) {
      inlines = cvCreateImage(cvGetSize(image), 8, 1);
    }

    if (image.nChannels() == 3) {
      cvCvtColor(image, gray, CV_BGR2GRAY);
    } else {
      gray = image.clone();
    }
    /*
     * lowThreshold = 600.0; highThreshold = 1220.0; apertureSize = 5;
     */
    // lowThreshold = 90.0;
    // highThreshold = 110.0;
    // apertureSize = 3;
    // log.warn(String.format("%f, %f, %d", lowThreshold, highThreshold,
    // apertureSize));
    cvCanny(gray, inlines, lowThreshold, highThreshold, apertureSize);

    return inlines;
  }

}
