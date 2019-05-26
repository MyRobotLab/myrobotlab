/**
 *                    
 * @author GroG (at) myrobotlab.org
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

package org.myrobotlab.service;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.service.abstracts.AbstractServo;

/**
 * @author GroG
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0.0 - 180.0, and output can relay those values
 *         directly to the servo's firmware (Arduino ServoLib, I2C controller,
 *         etc)
 * 
 *         However there can be the occasion that the input comes from a system
 *         which does not have the same range. Such that input can vary from 0.0
 *         to 1.0. For example, OpenCV coordinates are often returned in this
 *         range. When a mapping is needed Servo.map can be used. For this
 *         mapping Servo.map(0.0, 1.0, 0, 180) might be desired. Reversing input
 *         would be done with Servo.map(180, 0, 0, 180)
 * 
 *         outputY - is the values sent to the firmware, and should not
 *         necessarily be confused with the inputX which is the input values
 *         sent to the servo
 *         
 *         FIXME - inherit from AbstractMotor ..
 * 
 */

public class HobbyServo extends AbstractServo {

  private static final long serialVersionUID = 1L;

  public HobbyServo(String name) {
    super(name);
  }
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(HobbyServo.class);
    meta.addDescription("General hobby servo control with absolute positioning");
    meta.addCategory("motor", "control");
    meta.setAvailable(true);

    return meta;
  }
  
  public static void main(String[] args) throws InterruptedException {
    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      
      
      Platform.setVirtual(true);

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      // mega.setBoardMega();
      HobbyServo servo = (HobbyServo) Runtime.start("servo", "HobbyServo");
      servo.setPin(12);
      servo.sweepDelay = 3;
      // servo.save();
      servo.load();
      servo.save();
      log.info("sweepDely {}", servo.sweepDelay);
   
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}