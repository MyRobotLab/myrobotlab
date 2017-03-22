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

public interface ServoControl extends DeviceControl, AbsolutePositionControl, ServiceInterface {

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
   * @param pin
   * @throws Exception
   */

  // preferred - sets control
  void attach(ServoController controller) throws Exception;
  
  void attach(ServoController controller, int pin) throws Exception;

  void attach(ServoController controller, int pin, double pos) throws Exception;

  void attach(ServoController controller, int pin, double pos, double speed) throws Exception;

  /**
   * determines if a 'specific' controller is currently attached
   * 
   * @param controller
   * @return
   */
  public boolean isAttached(ServoController controller);

  /**
   * detaches a 'specific' controller
   * 
   * @param controller
   */
  void detach(ServoController controller);

  /**
   * attach "Pin" - simple command to energize the pin. Equivalent to Arduino
   * library's servo.attach(pin) with pin number coming from the servo
   * 
   * @return
   */
  public void attach();

  /**
   * degrees per second rotational velocity cm per second linear velocity ?
   * 
   * @param degreesPerSecond
   */
  public void setVelocity(double degreesPerSecond);

  /**
   * Re-attaches (re-energizes) the servo on its current pin NOT RELATED TO
   * CONTROLLER ATTACH/DETACH !
   * 
   * @return
   */
  public void attach(int pin);

  /**
   * detaching a pin (NOT RELATED TO DETACHING A SERVICE !)
   * 
   * @return
   */
  public void detach();

  /**
   * limits input of servo - to prevent damage or problems if servos should not
   * move thier full range
   * 
   * @param max
   */
  public void setMinMax(double min, double max);
  
  /**
   * min x 
   * @return
   */
  public double getMin();
  
  /**
   * max x
   * @return
   */
  public double getMax();

  /**
   * fractional speed settings 0.0 to 1.0
   * 
   * @param speed
   */
  public void setSpeed(double speed);

  /**
   * stops the servo if currently in motion servo must be moving at incremental
   * speed for a stop to work (setSpeed < 1.0)
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
   * @return
   */
  public Integer getPin();

  /**
   * command to move to the rest position
   */
  public void rest();
  
  /**
   * Unmapped current position of last input.
   * ie. equivalent to the last position from moveTo(pos) 
   * 
   * A possible better solution might be to use ServoEvent(s)
   * to get the position through time which a controller with
   * speed control can provide, so as the servo is told incrementally 
   * where to go - it sends that command back as an event which sets
   * the "current" position.
   * 
   */
  public double getPos();
  
  /**
   * the calculated mapper output for the servo - this is
   * <b> ALWAYS ALWAYS in DEGREES !</b>
   * because this method is used by the controller - and the controller needs
   * a stable method & a stable unit
   * FIXME - not sure if this is a good thing to expose
   */
  public double getTargetOutput();
 
  public double getMaxVelocity();

  double getVelocity();

  /**
   * set the pin of the servo this does not 'attach' energize the pin only set
   * the pin value
   * 
   * @param pin
   */
  void setPin(int pin);

  /**
   * get the current acceleration value
   * 
   * @return
   */
  public double getAcceleration();

  /**
   * synchronizing servos together
   * e.g.  leftEye.sync(rightEye)
   * @param sc
   */
  default public void sync(ServoControl sc) {
    subscribe(sc.getName(), "publishServoEvent", getName(), "moveTo");
  }
  
  /**
   * A default position for the servo.
   * Defaulted to 90 unless explicitly set.
   * Position the servo will move to when method servo.rest() is called
   * @param rest
   */
  public void setRest(int rest);

  
  /**
   * Gets the current rest position value
   * @return
   */
  public double getRest();

  boolean isInverted();

}
