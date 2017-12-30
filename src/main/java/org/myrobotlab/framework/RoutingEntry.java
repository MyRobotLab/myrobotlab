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

package org.myrobotlab.framework;

import java.io.Serializable;

public class RoutingEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  public int ID;
  public String name; // routing name - any service which recieves a message
  // will add its name to the history
  public long timestamp; // timestamp of message arrival

  // option constants

  // ctors begin ----
  public RoutingEntry() {
    name = new String();
    timestamp = System.currentTimeMillis();
  }

  public RoutingEntry(final RoutingEntry other) {
    this();
    set(other);
  }

  // ctors end ----
  // assignment begin --- todo - look @ clone copy
  public void set(final RoutingEntry other) {
    ID = other.ID;
    name = other.name;
    timestamp = other.timestamp;

  }

  // assignment end ---

  /*
   * Default format was xml is now JSON TODO - make toStringStyler like spring
   */
  @Override
  public String toString() {
    StringBuffer ret = new StringBuffer();
    // ret.append("{<RoutingEntry");
    ret.append("{");
    ret.append("\"ID\":\"" + ID + "\"");
    ret.append("\"name\":" + "\"" + name + "\"");
    ret.append("\"timestamp\":" + "\"" + timestamp + "\"");

    // ret.append("</RoutingEntry>");
    ret.append("}");
    return ret.toString();
  }

}