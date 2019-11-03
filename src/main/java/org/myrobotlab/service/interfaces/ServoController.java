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

package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

public interface ServoController extends Attachable {

  /**
   * attach with pin or address parameter - this will just call servo.setPin(int) then servoController.attach(servo)
   * @param servo
   * @param pinOrAddress
   * @throws Exception
   */
  void attach(ServoControl servo, int pinOrAddress) throws Exception;

  void servoSweepStart(ServoControl servo);

  void servoSweepStop(ServoControl servo);

  void servoMoveTo(ServoControl servo);
  
  void servoStop(ServoControl servo);

  void servoWriteMicroseconds(ServoControl servo, int uS);

  void servoSetVelocity(ServoControl servo);

  void servoSetAcceleration(ServoControl servo);

  /**
   * enable the pwm to a servo
   * @param servo - the servo to enable
   */
  void servoEnable(ServoControl servo);

  /**
   * disable the pwm to a servo
   * @param servo - the servo to disable
   */
  void servoDisable(ServoControl servo);

}
