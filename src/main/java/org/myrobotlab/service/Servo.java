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
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.abstracts.AbstractServo;
import org.myrobotlab.service.interfaces.ServoControl;

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

public class Servo extends AbstractServo implements ServoControl {

  private static final long serialVersionUID = 1L;

  public Servo(String n, String id) {
    super(n, id);
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

    ServiceType meta = new ServiceType(Servo.class);
    meta.addDescription("General hobby servo control with absolute positioning");
    meta.addCategory("motor", "control", "servo");
    meta.setAvailable(true);

    return meta;
  }
  
  public static void main(String[] args) throws InterruptedException {
    try {

      Runtime.main(new String[] { "--interactive", "--id", "servo"});
      // LoggingFactory.init(Level.INFO);
      // Platform.setVirtual(true);
      
      //Runtime.start("gui", "SwingGui");
      // Runtime.start("python", "Python");
      Runtime.start("webgui", "WebGui");
      
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino"); 
      Servo tilt = (Servo) Runtime.start("tilt", "Servo");
      Servo pan = (Servo) Runtime.start("pan", "Servo");

      boolean done = true;
      if (done) {
        return;
      }

      mega.connect("/dev/ttyACM1");
      // mega.setBoardMega();
            
      
      log.info("servo pos {}", tilt.getPos());
      
      // double pos = 170;
      // servo03.setPosition(pos);
      tilt.setPin(3);
      
      double min = 3;
      double max = 170;
      double speed = 60; // degree/s
      
      mega.attach(tilt);
      // mega.attach(servo03,3);
      
      for (int i = 0; i < 100 ; ++i) {
        tilt.moveTo(20.0);
      }
      
      tilt.sweep(min, max, speed);
      
      /*
      Servo servo04 = (Servo) Runtime.start("servo04", "Servo");
      Servo servo05 = (Servo) Runtime.start("servo05", "Servo");
      Servo servo06 = (Servo) Runtime.start("servo06", "Servo");
      Servo servo07 = (Servo) Runtime.start("servo07", "Servo");
      Servo servo08 = (Servo) Runtime.start("servo08", "Servo");
      Servo servo09 = (Servo) Runtime.start("servo09", "Servo");
      Servo servo10 = (Servo) Runtime.start("servo10", "Servo");
      Servo servo11 = (Servo) Runtime.start("servo11", "Servo");
      Servo servo12 = (Servo) Runtime.start("servo12", "Servo");
      */
      // Servo servo13 = (Servo) Runtime.start("servo13", "Servo");

     // servo03.attach(mega, 8, 38.0);
      /*
      servo04.attach(mega, 4, 38.0);
      servo05.attach(mega, 5, 38.0);
      servo06.attach(mega, 6, 38.0);
      servo07.attach(mega, 7, 38.0);
      servo08.attach(mega, 8, 38.0);
      servo09.attach(mega, 9, 38.0);
      servo10.attach(mega, 10, 38.0);
      servo11.attach(mega, 11, 38.0);
      servo12.attach(mega, 12, 38.0);
      */
      
      // TestCatcher catcher = (TestCatcher)Runtime.start("catcher", "TestCatcher");
      // servo03.attach((ServoDataListener)catcher);
      
      // servo.setPin(12);
      
      /*
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      servo.attach(mega, 7, 38.0);
      */
      
      // servo.sweepDelay = 3;
      // servo.save();
      // servo.load();
      // servo.save();
      // log.info("sweepDely {}", servo.sweepDelay);
   
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  

}