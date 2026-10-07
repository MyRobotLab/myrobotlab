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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterInput extends OpenCVFilter {
  /**
   * Catalog metadata for the WebGui filter guide. Static so it can be read
   * without constructing the filter (constructors may start services).
   */
  public static OpenCVFilterInfo catalogInfo() {
    return new OpenCVFilterInfo("Input",
        "Pass-through of the grabber image. Use it as a named tap so later filters can select this stage as their sourceKey, or to pin the WebGui viewer on the raw frame.",
        "Add at the start of a pipeline when you want a stable \"input\" display name. It does not modify pixels.",
        OpenCVFilterInfo.DEP_CORE);
  }

  @Override
  public OpenCVFilterInfo getFilterInfo() {
    return catalogInfo();
  }


  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterInput.class.getCanonicalName());

  public OpenCVFilterInput() {
    super();
  }

  public OpenCVFilterInput(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) {
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
