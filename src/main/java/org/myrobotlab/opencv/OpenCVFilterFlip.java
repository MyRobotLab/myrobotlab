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
package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvFlip;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterFlip extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  transient IplImage flipped;
  public int flipCode = 0;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFlip.class.getCanonicalName());

  public OpenCVFilterFlip() {
    super();
  }

  public OpenCVFilterFlip(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    flipped = IplImage.createCompatible(image);
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    cvFlip(image, flipped, flipCode);

    return flipped;
  }

}
