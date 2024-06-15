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

package org.myrobotlab.service.data;

import java.io.Serializable;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * A generalized Pin
 * 
 * consider definition being separate from data
 * 
 * interestingly - at the moment gson does not serialize statics - e.g. -
 * {"pin":5,"type":0,"value":1,"source":"here"}
 *
 */
public class Pin implements Serializable {
  public final static Logger log = LoggerFactory.getLogger(Pin.class);

  private static final long serialVersionUID = 1L;

  public static final int DIGITAL_VALUE = 1; // normalized with data coming
  // from
  // Arduino.DIGITAL_READ_POLLING_START
  public static final int PWM_VALUE = 2;
  public static final int ANALOG_VALUE = 3; // normalized with data coming
  // from
  // Adruino.ANALOG_READ_POLLING_START

  // TODO - make pin attributes !
  public static final int TYPE_DIGITAL_MASK = 1;
  public static final int TYPE_PWM_MASK = 2;
  public static final int TYPE_ANALOG_MASK = 4;

  // FIXME - change pin to address sometime
  public int pin;
  public int type;
  public int value;
  public String source;

  public Pin() {
  }

  public Pin(int pin, int type, int value, String source) {
    this.pin = pin;
    this.type = type;
    this.value = value;
    this.source = source;
  }

  public Pin(Pin pin) {
    this.pin = pin.pin;
    this.type = pin.type;
    this.value = pin.value;
    this.source = pin.source;
  }

  @Override
  public String toString() {
    return CodecUtils.toJson(this);
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      Pin pin = new Pin();
      pin.pin = 5;
      pin.source = "here";
      pin.value = 1;
      log.info("Pin:", pin);
    } catch (Exception e) {
      log.info(e.getMessage());
    }
  }

}