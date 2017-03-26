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

import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_imgproc.cvFloodFill;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterFloorFinder extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFloorFinder.class.getCanonicalName());

  transient IplImage buffer = null;

  CvPoint startPoint = cvPoint(180, 120);
  CvScalar fillColor = cvScalar(255.0, 255.0, 255.0, 1.0);
  CvScalar lo_diff = CV_RGB(2, 2, 2);// cvScalar(20, 0.0, 0.5, 1.0);
  CvScalar up_diff = CV_RGB(2, 2, 2);

  public OpenCVFilterFloorFinder() {
    super();
  }

  public OpenCVFilterFloorFinder(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  public void updateLoDiff(int r, int g, int b) {
    lo_diff = CV_RGB(r, g, b);
  }

  public void updateUpDiff(int r, int g, int b) {
    up_diff = CV_RGB(r, g, b);
  }
  
  public void updateFillColor(int r, int g, int b) {
    fillColor = cvScalar(r, g, b, 1.0);
  }
  
  @Override
  public IplImage process(IplImage image, OpenCVData data) {
    // if (startPoint == null) {
    startPoint = cvPoint(image.width() / 2, image.height() - 4);
    //}    
    cvFloodFill(image, startPoint, fillColor, lo_diff, up_diff, null, 4, null);
    return image;

  }

}
