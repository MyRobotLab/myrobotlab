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
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;

public interface ServoController extends Attachable {

  /**
   * attach with pin or address parameter - this will just call
   * servo.setPin(int) then servoController.attach(servo)
   * 
   * @param servo
   *          - servo reference
   * @param pinOrAddress
   *          - pin or address to attach
   * @throws Exception
   */
  @Deprecated /* use attachServo(ServoControl sc) */
  void attach(ServoControl servo, int pinOrAddress) throws Exception;

  /**
   * preferred method to attach a ServoControl to a ServoController previous
   * attach function is deprecated - ServoController "should not" be calling any
   * functions on ServoControl except possibly
   * ServoContro.attach(ServoController sc)
   * 
   * @param sc
   *          - servo reference
   */
  void attachServoControl(ServoControl sc);

  /**
   * The main function of the servo controller is to move the servo The
   * ServoControl is passed as a parameter such that the controller can get all
   * the necessary information to process the move correctly
   * 
   * @param move
   *          - servo reference
   */
  void onServoMoveTo(ServoMove move);

  /**
   * Stop the servo regardless of where it is in its move
   * 
   * @param servo
   *          - servo reference
   */
  void onServoStop(ServoControl servo);

  /**
   * A direct call using micro-seconds instead of degrees
   * 
   * @param servo
   *          - servo reference
   * @param uS
   *          - micro seconds of pwm
   */
  void onServoWriteMicroseconds(ServoControl servo, int uS);

  /**
   * set the speed of the servo
   * 
   * @param speed
   *          - contains the servo name and desired speed
   */
  void onServoSetSpeed(ServoSpeed speed);

  /**
   * enable the pwm on this servo
   * 
   * @param servoName
   *          - name of servo
   */
  void onServoEnable(String servoName);

  /**
   * disable servo
   * 
   * @param servoName
   *          - name of servo
   */
  void onServoDisable(String servoName);

}
