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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvFlip;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterFlip extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  transient IplImage flipped;
  public int flipCode = 0;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFlip.class.getCanonicalName());

  public OpenCVFilterFlip(String name) {
    super(name);
  }

  public OpenCVFilterFlip() {
    super();
  }

  @Override
  public void imageChanged(IplImage image) {
    flipped = AbstractIplImage.createCompatible(image);
  }

  @Override
  public IplImage process(IplImage image) {

    cvFlip(image, flipped, flipCode);

    /**
     * <pre>
     Flip (Mirror) Vertically
    
     flip(image, image, 0);
     Flip (Mirror) Horizontally
    
     flip(image, image, +1);
     Rotate 90 Degrees Clockwise
    
     transpose(image, image);
     flip(image, image, +1);
     Rotate 90 Degrees Counter Clockwise (Same as -90 Degrees and 270 Degrees)
    
     transpose(image, image);
     flip(image, image, 0);
     Rotate 180 Degrees (Same as Flipping vertically and horizontally at the same time)
    
     flip(image, image, -1);
     * </pre>
     */

    return flipped;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
