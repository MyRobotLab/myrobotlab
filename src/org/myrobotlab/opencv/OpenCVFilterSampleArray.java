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

import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterSampleArray extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSampleArray.class.getCanonicalName());

  transient IplImage buffer = null;

  transient BufferedImage frameBuffer = null;

  ColoredPoint points[] = new ColoredPoint[] { new ColoredPoint() };

  public OpenCVFilterSampleArray() {
    super();
  }

  public OpenCVFilterSampleArray(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    frameBuffer = OpenCV.IplImageToBufferedImage(image);// image.getBufferedImage();

    points[0].x = image.width() / 2;
    points[0].y = image.height() - 20;

    for (int i = 0; i < points.length; ++i) {
      points[i].color = frameBuffer.getRGB(points[i].x, points[i].y);
      frameBuffer.setRGB(points[0].x, points[0].y, 0x00ff22);
    }

    invoke("publish", (Object) points);

    return image;

  }

}
