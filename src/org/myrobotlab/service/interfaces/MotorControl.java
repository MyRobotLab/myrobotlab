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

import org.myrobotlab.sensor.Encoder;

public interface MotorControl extends  DeviceControl { // SensorDataPublisher, SensorDataListener, NameProvider, MessageSubscriber {

  public void attach(MotorController controller) throws Exception;
  
  public void detach(MotorController controller);

  double getPowerLevel();

  double getPowerOutput();

  int getTargetPos();

  /**
   * query the motor as to its inverted status
   * 
   * @return
   */
  boolean isInverted();

  /**
   * locks the motor so no other commands will affect it until it becomes
   * unlocked
   */
  void lock();

  /**
   * Move is the most common motor command. The command accepts a parameter of
   * power which can be of the range -1.0 to 1.0. Negative values are in one
   * direction and positive values are in the opposite value. For example -1.0
   * would be maximum power in a counter clock-wise direction and 0.9 would be
   * 90% power in a clockwise direction. 0.0 of course would be stop
   * 
   * @param power
   *          - new power level
   */
  void move(double power);

  /**
   * moveTo moves the motor to a specific location. Typically, an encoder is
   * needed in order to provide feedback data
   * 
   * @param newPos
   */
  void moveTo(int newPos);

  /**
   * moveTo moves the motor to a specific location. Typically, an encoder is
   * needed in order to provide feedback data
   * 
   * @param newPos
   */
  void moveTo(int newPos, Double power);


  void setEncoder(Encoder encoder);

  /**
   * change the motors direction such that negative power levels become
   * clockwise if previous levels were counter clockwise and positive power
   * levels would become counter clockwise
   * 
   * @param invert
   */
  void setInverted(boolean invert);

  void stop();

  /**
   * a safety mechanism - stop and lock will stop and lock the motor no other
   * commands will affect the motor until it is "unlocked"
   */
  void stopAndLock();

  /**
   * unlocks the motor, so other commands can affect it
   */
  void unlock();

}
