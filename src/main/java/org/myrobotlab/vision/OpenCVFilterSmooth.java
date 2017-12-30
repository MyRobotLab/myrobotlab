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

import static org.bytedeco.javacpp.opencv_imgproc.CV_GAUSSIAN;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSmooth extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSmooth.class.getCanonicalName());

  transient IplImage buffer = null;

  public OpenCVFilterSmooth() {
    super();
  }

  public OpenCVFilterSmooth(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    // cvDrawRect(image, startPoint, startPoint,
    // fillColor, 2, 1, 0);
    cvSmooth(image, image, CV_GAUSSIAN, 9, 7, 7, 1);

    return image;

  }

}
