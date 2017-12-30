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

import java.awt.Rectangle;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSetImageROI extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  Rectangle r = new Rectangle(10, 10, 100, 100);
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSetImageROI.class.getCanonicalName());

  public OpenCVFilterSetImageROI() {
    super();
  }

  public OpenCVFilterSetImageROI(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    // cvSetImageROI(image, cvRect(1.0,1.0,20));

    return image; // TODO - src dst or image? consistency?
  }

}
