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

public interface ServoController extends DeviceController {
  
  /**
   * high level "attach" which internally will call attachDevice(Device device, int[] config)
   * and "might" call Servo.attach(pin) on MRLComm
   * 
   * "might" call - because Servo.attach & Device.attach are NOT related
   * 
   * @param servo
   * @param pin -  All of the config needed for the device 
   */
  public void attach(Servo servo, int pin);
  
  /**
   * retrieve the pin the servo is attached to
   * @param servo
   * @return null if pin is not set - otherwise the pin
   * @throws Exception
   */
  public Integer getPin(Servo servo);
  
 
  /**
   * high level "detach" with internally will call detachDevice(Device device) - this
   * most likely will call Servo.detach - because it represents the "removal" of the 
   * peripheral device from the Arduino ... similar to pulling the wires off ;)
   * 
   * @param servo
   */
  public void detach(Servo servo);

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

  public void setServoSpeed(Servo servo);
  
  /**
   * These are "System" calls for the Arduino and possibly other uCs e.g. .. ---> Servo.attach(10)
   * NOT DEVICE ATTACH & DETACH !!!! 
   * @param servo
   */
  public void servoAttach(Servo servo, int pin);
  
  /**
   * These are "System" calls for the Arduino  e.g. .. ---> Servo.detach()
   * NOT DEVICE ATTACH & DETACH !!!! 
   * @param servo
   */
  public void servoDetach(Servo servo);
    
  
}
