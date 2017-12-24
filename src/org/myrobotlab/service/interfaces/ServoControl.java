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

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.MessageSubscriber;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface ServoControl extends AbsolutePositionControl, Attachable, MessageSubscriber {

  // FIXME - do we want to support this & what do we expect from
  // 1. should it be energsetAbsoluteSpeedized when initially attached?
  // 2. should the position be set initially on attach ?
  // 3. should rest be set by pos if its not set already .. ie .. is the pos
  // passed in on attach the "rest" position of the servo ?
  // 4. should we 'please' rename servo.attach(pin) to servo.enablePwm(pin)
  // servo.disablePwm(pin)
  // !!!!

  /**
   * The point of the 'attach' is a concept to the user of the Servo. A simple
   * concept across all services where the "minimal" amount of
   * complexity/parameters are needed to 'attach' position shall be default to
   * rest/90 if not specified
   *
   * 
   * speed/velocity shall be defaulted to 'max' ie - no speed control
   * 
   * 
   * @param controller
   *          c
   * @param pin
   *          p
   * @throws Exception
   *           e
   */

  /**
   * The one and only one attach which does the work we expect attaching a
   * ServoControl to a ServoController
   * 
   * @param controller
   *          c
   * @throws Exception
   *           e
   */
  void attachServoController(ServoController controller) throws Exception;

  /**
   * the one and only one which detaches a 'specific' ServoControl from
   * ServoController
   * 
   * @param controller
   *          e
   * @throws Exception
   *           e
   */
  void detachServoController(ServoController controller) throws Exception;

  /**
   * determines if a 'specific' controller is currently attached
   * 
   * @param controller
   *          c
   * @return true/false
   * 
   */
  public boolean isAttachedServoController(ServoController controller);

  /**
   * attach with different parameters - it should set fields then call the "one
   * and only" single parameter attachServoController(controller)
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
   * attach "Pin" - simple command to energize the pin. Equivalent to Arduino
   * library's servo.attach(pin) with pin number coming from the servo
   */
  public void attach();

  /**
   * @param degreesPerSecond
   *          degrees per second rotational velocity cm per second linear
   *          velocity ?
   * 
   */
  public void setVelocity(double degreesPerSecond);

  /*
   * Re-attaches (re-energizes) the servo on its current pin NOT RELATED TO
   * CONTROLLER ATTACH/DETACH !
   * 
   * Deprecated - use enable(pin)
   */
  @Deprecated
  public void attach(int pin);

  /**
   * detaching a pin (NOT RELATED TO DETACHING A SERVICE !)
   */
  @Deprecated // should be explicit from which service is being detached - by
              // name or reference - this is "too" general
  public void detach();

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
  public void setMinMax(double min, double max);

  /**
   * @return min x
   */
  public double getMin();

  public double getMinInput();

  /**
   * @return max x
   */
  public double getMax();

  public double getMaxInput();

  /**
   * @param speed
   *          fractional speed settings 0.0 to 1.0
   * 
   */
  public void setSpeed(double speed);

  /**
   * stops the servo if currently in motion servo must be moving at incremental
   * speed for a stop to work (setSpeed &lt; 1.0)
   */
  public void stop();

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
  public Integer getPin();

  /**
   * command to move to the rest position
   */
  public void rest();

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
  public double getPos();

  /**
   * the calculated mapper output for the servo - this is <b> ALWAYS ALWAYS in
   * DEGREES !</b> because this method is used by the controller - and the
   * controller needs a stable method &amp; a stable unit FIXME - not sure if
   * this is a good thing to expose
   * 
   * @return the target output position
   */
  public double getTargetOutput();

  public double getMaxVelocity();

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
  public double getAcceleration();

  /*
   * synchronizing servos together e.g. leftEye.sync(rightEye)
   */
  public void sync(ServoControl sc);

  /**
   * @param rest
   *          A default position for the servo. Defaulted to 90 unless
   *          explicitly set. Position the servo will move to when method
   *          servo.rest() is called
   */
  public void setRest(double rest);

  /**
   * @return the current rest position value
   */
  public double getRest();

  boolean isInverted();

  /**
   * invert the map so a servo will go in reverse direction 0 == 180, 90 == 90, 180 == 0
   * @param invert - true is to invert
   */
  public void setInverted(boolean invert);

  // WTF ?
  void addIKServoEventListener(NameProvider service);

  /**
   * setAutoDisable tell the servo to disable when position reached
   * this make sense only if velocity > 0
   * if velocity == -1 : a timer is launched to delay disable
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
   * waitTargetPos is used by a global moveToBlocking command - pos usually is 0 - 180
   * a global moveToBlocking is a method that use multiple servo at same time
   * and wait every servo for last position arrived
   * 
   * @param pos
   *          - position to move to
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
   * Sometime we need to override autoDisable :
   * servoGui slider / tracking / gestures  that leave your arms in the air ...
   * so if overrideautoDisable(true) servo will never autoDisable
   * until overrideautoDisable(false)
   * ( we need to keep original autoDisable status, that is the reason ) 
   */
  void setOverrideAutoDisable(boolean overrideAutoDisable);

  void onServoEvent(Integer eventType, double currentPosUs);

  double getCurrentPosOutput();

  void addServoEventListener(NameProvider service);
  
  public void enable();
  
  public void disable();
}
