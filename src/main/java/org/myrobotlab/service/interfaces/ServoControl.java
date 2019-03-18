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
import org.myrobotlab.framework.interfaces.MessageSubscriber;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface ServoControl extends AbsolutePositionControl, Attachable, MessageSubscriber {

  /**
   * attach with different parameters - it should set fields then call the "one
   * and only" single parameter attachServoController(controller)
   * 
   * if no controller is provided, last used is set from json related.
   * 
   * @param controller
   *          the controller
   * @param pin
   *          the pin
   * @throws Exception
   *           e
   */
  void attach(ServoController controller, int pin) throws Exception;

  void attach(ServoController controller, int pin, double pos) throws Exception;

  void attach(ServoController controller, int pin, double pos, double speed) throws Exception;

  /**
   * @param degreesPerSecond
   *          degrees per second rotational velocity cm per second linear
   *          velocity ?
   * 
   */
  void setVelocity(double degreesPerSecond);

  /**
   * limits input of servo - to prevent damage or problems if servos should not
   * move their full range
   * 
   * @param min
   *          min value
   * @param max
   *          max value
   * 
   */
  void setMinMax(double min, double max);

  /**
   * @return min x
   */
  double getMin();

  double getMinInput();

  /**
   * @return max x
   */
  double getMax();

  double getMaxInput();

  /**
   * @param speed
   *          fractional speed settings 0.0 to 1.0
   * 
   */
  void setSpeed(double speed);

  /**
   * stops the servo if currently in motion servo must be moving at incremental
   * speed for a stop to work (setSpeed &lt; 1.0)
   */
  void stop();

  /**
   * configuration method - a method the controller will call when the servo is
   * attached.
   * 
   * What should happen is if (controller != null) { pin =
   * controller.servoGetPin(); } return pin; This returns the pin info the
   * controller has - updates the Servo's pin and returns the refreshed data.
   * Not worth it. What will happen is the pin which was set on the servo will
   * simply be returned
   * 
   * @return the pin as an integer
   */
  Integer getPin();

  /**
   * command to move to the rest position
   */
  void rest();

  /**
   * Unmapped current position of last input. ie. equivalent to the last
   * position from moveTo(pos)
   * 
   * A possible better solution might be to use ServoEvent(s) to get the
   * position through time which a controller with speed control can provide, so
   * as the servo is told incrementally where to go - it sends that command back
   * as an event which sets the "current" position.
   * 
   * @return the current position as a double
   */
  double getPos();

  /**
   * the calculated mapper output for the servo - this is <b> ALWAYS ALWAYS in
   * DEGREES !</b> because this method is used by the controller - and the
   * controller needs a stable method &amp; a stable unit FIXME - not sure if
   * this is a good thing to expose
   * 
   * @return the target output position
   */
  double getTargetOutput();

  double getMaxVelocity();

  double getVelocity();

  /**
   * set the pin of the servo this does not 'attach' energize the pin only set
   * the pin value
   * 
   * @param pin
   *          the pin number for the servo
   */
  void setPin(int pin);

  /**
   * @return the current acceleration value
   */
  double getAcceleration();

  /*
   * synchronizing servos together e.g. leftEye.sync(rightEye)
   */
  void sync(ServoControl sc);

  /**
   * unsync a servo
   * 
   * @param sc
   */
  void unsync(ServoControl sc);

  /**
   * @param rest
   *          A default position for the servo. Defaulted to 90 unless
   *          explicitly set. Position the servo will move to when method
   *          servo.rest() is called
   */
  void setRest(double rest);

  /**
   * @return the current rest position value
   */
  double getRest();

  boolean isInverted();

  /**
   * invert the map so a servo will go in reverse direction 0 == 180, 90 == 90,
   * 180 == 0
   * 
   * @param invert
   *          - true is to invert
   */
  void setInverted(boolean invert);

  // WTF ?
  void addIKServoEventListener(NameProvider service);

  /**
   * setAutoDisable tell the servo to disable when position reached this make
   * sense only if velocity &gt; 0 if velocity == -1 : a timer is launched to delay
   * disable
   * 
   * @param autoDisable
   *          - boolean
   */
  void setAutoDisable(boolean autoDisable);

  /**
   * getAutoDisable return value set by setAutoDisable
   * 
   * @return boolean
   */
  boolean getAutoDisable();

  /**
   * waitTargetPos is used by a global moveToBlocking command - pos usually is 0
   * - 180 a global moveToBlocking is a method that use multiple servo at same
   * time and wait every servo for last position arrived
   * 
   */
  void waitTargetPos();

  /**
   * moveToBlocking is a basic move command of the servo - usually is 0 - 180
   * valid range but can be adjusted and / or re-mapped with min / max and map
   * commands
   * 
   * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
   * response
   * 
   * @param pos
   *          - position to move to
   * @return true (why?)
   */
  boolean moveToBlocking(double pos);

  /**
   * Sometime we need to override autoDisable : servoGui slider / tracking /
   * gestures that leave your arms in the air ... so if
   * overrideautoDisable(true) servo will never autoDisable until
   * overrideautoDisable(false) ( we need to keep original autoDisable status,
   * that is the reason )
   */
  void setOverrideAutoDisable(boolean overrideAutoDisable);

  void onServoEvent(Integer eventType, double currentPosUs);

  double getCurrentPosOutput();

  void addServoEventListener(NameProvider service);

  void removeServoEventListener(NameProvider service);

  void enable();

  /**
   * disable the pulses to the servo
   */
  void disable();

  String getControllerName();

  boolean isAttached();

  ServoControl publishMoveTo(ServoControl sc);

  Double getLastPos();

}
