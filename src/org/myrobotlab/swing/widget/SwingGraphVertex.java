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

package org.myrobotlab.swing.widget;

import java.io.Serializable;

public class SwingGraphVertex implements Serializable {
  public static enum Type {
    SERVICE, INPORT, OUTPORT
  }

  static final long serialVersionUID = 1L;
  public String name;
  public String toolTip;
  public String canonicalName;
  public String displayName;
  public String status;

  public Type type;

  public SwingGraphVertex() {
  }

  public SwingGraphVertex(String name, String canonicalName, String displayName, String toolTip, Type t) {
    this.name = name;
    this.canonicalName = canonicalName;
    this.displayName = displayName;
    this.toolTip = toolTip;
    this.type = t;
  }

  @Override
  public String toString() {
    return displayName;
  }

}
