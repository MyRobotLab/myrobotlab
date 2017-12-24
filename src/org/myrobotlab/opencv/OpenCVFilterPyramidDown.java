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

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterPyramidDown extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterPyramidDown.class.getCanonicalName());

  final static int CV_GAUSSIAN_5X5 = 7;

  transient IplImage dst = null;

  public OpenCVFilterPyramidDown() {
    super();
  }

  public OpenCVFilterPyramidDown(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    dst = cvCreateImage(cvSize(image.width() / 2, image.height() / 2), image.depth(), image.nChannels());
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {
    cvPyrDown(image, dst, CV_GAUSSIAN_5X5);
    return dst;
  }

}
