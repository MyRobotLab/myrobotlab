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

package org.myrobotlab.service.data;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class NameValuePair implements Serializable {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(NameValuePair.class);

  public int ID;
  final public String name; // name
  final public String value; // value

  // option constants

  public NameValuePair(final NameValuePair other) {
    this.name = other.name;
    this.value = other.value;
  }

  // ctors begin ----
  public NameValuePair(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /*
   * Default format was xml is now JSON TODO - make toStringStyler like spring
   */
  @Override
  public String toString() {
    StringBuffer ret = new StringBuffer();
    // ret.append("{<NameValuePair");
    ret.append("{");
    ret.append("\"ID\":\"" + ID + "\"");
    ret.append("\"name\":" + "\"" + name + "\"");
    ret.append("\"value\":" + "\"" + value + "\"");

    // ret.append("</NameValuePair>");
    ret.append("}");
    return ret.toString();
  }

}