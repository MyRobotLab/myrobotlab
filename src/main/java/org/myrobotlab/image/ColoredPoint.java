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

package org.myrobotlab.image;

import java.awt.Color;
import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ColoredPoint implements Serializable {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ColoredPoint.class);

  public int x;
  public int y;
  public int color;

  // ctors begin ----
  public ColoredPoint() {
  }

  public ColoredPoint(final ColoredPoint other) {
    this();
    set(other);
  }

  public Color getAWTColor() {
    return new Color(getRed(), getGreen(), getBlue());
  }

  public int getBlue() {
    return ((color & 0x0000ff00) >> 8);
  }

  public int getGreen() {
    return color & 0x000000ff;
  }

  public int getRed() {
    return ((color & 0x00ff0000) >> 16);
  }

  public void set(final ColoredPoint other) {
    x = other.x;
    y = other.y;
    color = other.color;
  }

}