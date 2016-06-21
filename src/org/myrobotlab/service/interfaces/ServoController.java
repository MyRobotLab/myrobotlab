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

package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.Servo;

public interface ServoController extends NameProvider, DeviceController {

  public final static String servoWrite = "servoWrite";
  public final static String servoAttach = "servoAttach";
  public final static String servoDetach = "servoDetach";

  /**
   * attachDevice - attach the servo to a specific pin on the controller
   * @return boolean boolean
   */
  public void attachDevice(Device servo);

  /**
   * servoDetach - detach the servo from a specific pin on the controller
   * 
   * @param name
   *          - name of the servo
   * @return boolean
   */
  void servoDetach(Servo servo);

  void servoSweepStart(Servo servo);

  public void servoSweepStop(Servo servo);

  /**
   * servoWrite - move the servo at an angle between 0 - 180
   * 
   * @param name
   *          - name of the servo
   * @param newPos
   *          - positive or negative relative amount to move the servo
   * @return void
   */
  void servoWrite(Servo servo);

  public void servoWriteMicroseconds(Servo servo);

  public boolean servoEventsEnabled(Servo servo, boolean enabled);

  /**
   * return the current pin this servo is attached to
   * 
   * @param servoName
   * @return
   */
  // public Integer getServoPin(String servoName);

  public void setServoSpeed(Servo servo);

}
