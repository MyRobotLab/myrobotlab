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

package org.myrobotlab.opencv;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterOutput extends OpenCVFilter {
  /**
   * Catalog metadata for the WebGui filter guide. Static so it can be read
   * without constructing the filter (constructors may start services).
   */
  public static OpenCVFilterInfo catalogInfo() {
    return new OpenCVFilterInfo("Output",
        "Pass-through at the end of a pipeline so the WebGui can pin the display on a named output stage without changing pixels.",
        "Add last when you want displayFilter \"output\" (or your instance name) to show the fully processed image.",
        OpenCVFilterInfo.DEP_CORE);
  }

  @Override
  public OpenCVFilterInfo getFilterInfo() {
    return catalogInfo();
  }


  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterOutput.class.getCanonicalName());

  // display related
  Graphics2D graphics = null;

  public OpenCVFilterOutput() {
    super();
  }

  public OpenCVFilterOutput(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

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
