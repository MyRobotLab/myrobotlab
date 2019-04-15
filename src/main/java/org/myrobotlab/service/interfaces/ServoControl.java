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

import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

public interface ServoControl extends AbsolutePositionControl, Attachable {

  /**
   * Attaches a servo controller with this servo control, supports a variety of
   * different parameter types as convenience methods for the user
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
  void attach(ServoController controller, Integer pin) throws Exception;

  void attach(ServoController controller, Integer pin, Double pos) throws Exception;

  void attach(ServoController controller, Integer pin, Double pos, Double speed) throws Exception;

  /**
   * Servo events types are yet to be standardized - but probably movement,
   * stopped are two possible types used in the past
   * 
   * @param service
   *          - the listener
   */
  void attach(ServoDataListener listener);

  void attach(String controllerName, Integer pin) throws Exception;

  void attach(String controllerName, Integer pin, Double pos) throws Exception;

  void attach(String controllerName, Integer pin, Double pos, Double speed) throws Exception;

  /**
   * detaches the control from the controller
   * 
   * @param controller
   */
  void detach(ServoController controller);

  /**
   * remove the servo listener
   * 
   * @param service
   *          - to remove
   */
  void detach(ServoDataListener service);

  /**
   * disable the pulses to the servo
   */
  void disable();

  /**
   * enable the pulses to the servo
   */
  void enable();
  
  void enable(Integer pin);
  
  void enable(String pin);
  
  /**
   * acceleration of the servo
   * 
   * @return the acceleration
   */
  Double getAcceleration();

  /**
   * getAutoDisable return value set by setAutoDisable
   * 
   * @return Boolean
   */
  Boolean getAutoDisable();

  /**
   * name of the current controller - null if not set
   * 
   * @return
   */
  String getControllerName();

  /**
   * set of controller names
   * 
   * @return
   */
  public Set<String> getControllers();

  /**
   * gets the Max X of the mapper (input)
   * 
   * @return max x
   */
  Double getMax();
  
  /**
   * gets the Max applied after the output is calculated (clipping)
   * @return max y output
   */
  Double getMaxOutput();

  /**
   * returns max speed if set
   * 
   * @return - speed
   */
  Double getMaxSpeed();

  /**
   * gets the min X of the mapper (input)
   * 
   * @return min x
   */
  Double getMin();
  
  /**
   * gets the Min applied after the output is calculated (clipping)
   * @return - min output value
   */
  Double getMinOutput();

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
  String getPin();

  /**
   * Unmapped current position of the servo. This can be incrementally updated
   * by an encoder.
   * 
   * A possible better solution might be to use ServoData to get the
   * position through time which a controller with speed control can provide, so
   * as the servo is told incrementally where to go - it sends that command back
   * as an event which sets the "current" position.
   * 
   * This is a read only current position reported from the ServoControl. If a
   * mapper calculation is supplied to generate output - and the ServoController
   * uses it, and the ServoController sends back current position it "must" be
   * in the form of the original input range
   *
   * set by feedback encoders if available - this is typically updated during
   * movement - a read-only value
   * 
   * @return the current position as a Double
   */
  Double getPos();

  /**
   * @return the current rest position value
   */
  Double getRest();

  /**
   * Return current speed if set - if speed/speed control is not being use it is
   * null.
   * 
   * @return
   */
  Double getSpeed();

  /**
   * This value is for the ServoController to consume.
   * 
   * The calculated mapper output for the servo - this is <b> ALWAYS ALWAYS in
   * DEGREES !</b> because this method is used by the servo controller - and the
   * controller needs a stable method &amp; a stable unit
   * 
   * The is the output of the mapper, the servo controller may have its own
   * global mapper, this one is unique for a specific Servo
   * 
   * This is position the mapper returns after the calculation of mapper
   * 
   * @return the target output position
   */
  Double getTargetOutput();

  /**
   * If currently attached to a controller
   * 
   * @return
   */
  Boolean isAttached();

  /**
   * is the servo currently sending pwm position control
   * @return
   */
  Boolean isEnabled();

  /**
   * Returns true if mapper is inverted
   * 
   * @return
   */
  Boolean isInverted();

  /**
   * This sets the servo's mapper explicitly
   * 
   * @param minX
   *          - min input
   * @param maxX
   *          - max input
   * @param minY
   *          - min output
   * @param maxY
   *          - max output
   */
  void map(Double minX, Double maxX, Double minY, Double maxY);
  
  void map(Integer minX, Integer maxX, Integer minY, Integer maxY);

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
  Double moveToBlocking(Double pos);
  
  Double moveToBlocking(Integer pos);

  /**
   * control message publishing moveTo
   * 
   * @param sc
   * @return
   */
  ServoControl publishMoveTo(ServoControl sc);

  /**
   * 
   * @param eventType
   * @param currentPosUs
   * @return
   */
  ServoData publishServoData(ServoStatus eventType, Double currentPosUs);

  /**
   * command to move to the rest position
   */
  void rest();
  
  /**
   * return a list of possible controllers 
   * @return
   */
  public List<String> refreshControllers() ;

  /**
   * Set the acceleration of the servo
   * 
   * @param acceleration
   */
  void setAcceleration(Double acceleration);
  
  void setAcceleration(Integer acceleration);

  /**
   * setAutoDisable tell the servo to disable when position reached this make
   * sense only if speed &gt; 0 if speed == -1 : a timer is launched to delay
   * disable
   * 
   * @param autoDisable
   *          - Boolean
   */
  void setAutoDisable(Boolean autoDisable);

  /**
   * invert the map so a servo will go in reverse direction 0 == 180, 90 == 90,
   * 180 == 0
   * 
   * @param invert
   *          - true is to invert
   */
  void setInverted(Boolean invert);

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
  void setMinMax(Double min, Double max);

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
  void setMinMax(Integer min, Integer max);

  /**
   * set the pin of the servo this does not 'attach' energize the pin only set
   * the pin value
   * 
   * @param pin
   *          the pin number for the servo
   */
  void setPin(Integer pin);

  /**
   * set the pin of the servo this does not 'attach' energize the pin only set
   * the pin value
   * 
   * @param pin
   *          the pin number for the servo
   */
  void setPin(String pin);

  /**
   * @param rest
   *          A default position for the servo. Defaulted to 90 unless
   *          explicitly set. Position the servo will move to when method
   *          servo.rest() is called
   */
  void setRest(Double rest);

  void setRest(Integer rest);

  /**
   * @param speed
   *          degrees per second rotational speed cm per second linear
   * 
   */
  void setSpeed(Double speed);
  
  void setSpeed(Integer speed);

  @Deprecated
  void setVelocity(Double speed);
  @Deprecated
  void setVelocity(Integer speed);

  /**
   * stops the servo if currently in motion servo must be moving at incremental
   * speed for a stop to work (setSpeed &lt; 1.0)
   */
  void stop();

  /**
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
   * waitTargetPos is used by a global moveToBlocking command - pos usually is 0
   * - 180 a global moveToBlocking is a method that use multiple servo at same
   * time and wait every servo for last position arrived
   * 
   */
  void waitTargetPos();

}
