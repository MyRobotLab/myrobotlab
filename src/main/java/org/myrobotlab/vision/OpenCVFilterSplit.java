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

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;

import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterSplit extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSplit.class.getCanonicalName());

  public final String splitKey = String.format("%s_SPLIT", name);;
  transient public IplImage splitImage;

  public OpenCVFilterSplit() {
    super();
  }

  public OpenCVFilterSplit(String name) {
    super(name);
  }

  @Override
  public ArrayList<String> getPossibleSources() {
    ArrayList<String> ret = new ArrayList<String>();
    ret.add(name);
    ret.add(splitKey);
    return ret;
  }

  @Override
  public void imageChanged(IplImage image) {
    splitImage = cvCreateImage(cvSize(image.width() / 2, image.height() / 2), image.depth(), image.nChannels());
  }

  @Override
  public IplImage process(IplImage image, VisionData data) {
    cvCopy(image, splitImage);
    return image;
  }

}
