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

import org.myrobotlab.framework.MRLException;

public interface MotorController extends NameProvider, MicrocontrollerPeripheral {

  /**
   * typed motorAttach, typed for a reason - the MotorControl's attach should
   * always be used any of the following methods expect the MotorControl to be
   * valid and filled with the appropriate data by the time it's "attached" to
   * the motor controller, so.... the only parameter needed is the motor
   * 
   * @param motor
   * @throws MRLException
   */
  public void motorAttach(MotorControl motor) throws Exception;

  // ======== new interface end ===================

  /**
   * MotorDetach - detach the MotorControl from a specific pin on the controller
   * 
   * @param name
   *          - name of the MotorControl
   * @return void
   */
  public boolean motorDetach(MotorControl motor);

  /**
   * 
   * request for motor to move the motor can be queried for the new powerlevel
   * and the controller shall appropriately change power level and direction if
   * necessary
   * 
   * @param name
   */
  public void motorMove(MotorControl motor);

  /**
   * moveTo - move the MotorControl a relative amount the amount can be negative
   * or positive an integer value is expected
   * 
   * @param name
   *          - name of the MotorControl
   * @param position
   *          - positive or negative absolute amount to move the MotorControl
   * @return void
   */
  public void motorMoveTo(MotorControl motor);

  /**
   * stops the motor
   * 
   * @param motor
   */
  public void motorStop(MotorControl motor);

  /**
   * method for resetting all the variables of a motor this will reset counters
   * if the motor is a stepper and / or other variables for other sorts of
   * motors
   * 
   * @param motor
   */
  public void motorReset(MotorControl motor);

  /**
   * tests if this controller is connected & ready
   * 
   * @return
   */
  public boolean isConnected();

  /**
   * attachMotor - for shields and controllers whcih support the concept of a
   * "portNumber". Typically this is the number written on the board e.g. M1
   * would be port 1
   * 
   * @param name
   * @param i
   */
  public void motorAttach(MotorControl motor, int portNumber);

  /**
   * These symmetric String methods should probably be better suited if this was
   * an abstract class rather than an interface - then the mundane string to
   * type methods could be implemented only once
   * 
   * @param name
   * @param portNumber
   */
  public void motorAttach(String name, int portNumber);

  public void connect(String port);

  public void connect(String port, Integer rate, int i, int j, int k);

}
