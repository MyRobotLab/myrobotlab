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

import org.myrobotlab.framework.Config;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.StateSaver;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

public interface ServoControl extends AbsolutePositionControl, EncoderListener, Attachable, StateSaver, org.myrobotlab.framework.interfaces.StatePublisher {

  /**
   * attaches a servo data listener for servo data - like position information
   * 
   * @param listener
   */
  void attach(ServoDataListener listener);

  /**
   * remove the listener
   * 
   * @param listener
   */
  void detach(ServoDataListener listener);

  /**
   * enable the PWM pulses/power to the servo
   */
  @Config
  void enable();

  /**
   * disable the PWM pulses/power to the servo/motor
   */
  void disable();

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
   * gets the Max X of the mapper (input)
   * 
   * @return max x
   */
  Double getMax();

  /**
   * gets the Max applied after the output is calculated (clipping)
   * 
   * @return max y output
   */
  Double getMaxOutput();

  /**
   * returns max speed if set
   * 
   * @return - speed
   */
  // Double getMaxSpeed();

  /**
   * gets the min X of the mapper (input)
   * 
   * @return min x
   */
  Double getMin();

  /**
   * gets the Min applied after the output is calculated (clipping)
   * 
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
   * A possible better solution might be to use ServoData to get the position
   * through time which a controller with speed control can provide, so as the
   * servo is told incrementally where to go - it sends that command back as an
   * event which sets the "current" position.
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
   * acceleration of the servo
   * 
   * @return the acceleration
   */
  Double getAcceleration();

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
   * 
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

  void setMapper(Mapper m);

  Mapper getMapper();

  void setMaxSpeed(Double speed);

  Double getMaxSpeed();

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

  /**
   * moveToBlocking with a timeout blocking calling thread until either move has
   * been completed, or timeout reached
   */
  Double moveToBlocking(Double pos, Long timeoutMs);

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
   * Publishing topic for a servo stop event - returns position
   * 
   * @param pos
   * @return
   */
  public Double publishServoStopped(Double pos);

  /**
   * command to move to the rest position
   */
  void rest();

  /**
   * Set the acceleration of the servo
   * 
   * @param acceleration
   */
  void setAcceleration(Double acceleration);

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

  /**
   * @param speed
   *          degrees per second rotational speed cm per second linear
   * 
   */
  @Deprecated
  void setVelocity(Double speed);

  @Deprecated
  Double getVelocity();

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

  /**
   * The last time the servo was asked to move (system current time in ms?)
   * 
   * @return
   */
  @Deprecated /* use an encoder ! */
  long getLastActivityTime();

  public void setSpeed(Double d);

  Double getTargetPos();

  /**
   * This method sets the position without "moving" the servo. Typically, this
   * is useful for setting the initial position of the servo during startup
   * 
   * @param pos
   */
  void setPosition(Double pos);

  /**
   * returns the encoder attached to this ServoControl
   * 
   * @return - the Encoder
   */
  EncoderControl getEncoder();

  /**
   * When moveBlocking is in motion, not only should it block the calling thread
   * until the end of the move, it should also prevent (cancel) other threads
   * (even ones doing moveTo commands) until its done... conversely
   * mutli-threaded moveTo commands are a free-for-all .. if you call a servo
   * thats in process of a moveBlocking with a moveTo - your moveTo is canceled
   * (not blocked) until the moveToBlocking is done. When a moveToBlocking is
   * called from a different thread it should be blocked until the original is
   * finished.
   * 
   * @return
   */
  boolean isBlocking();

  /**
   * Returns if the sevo is currently moving
   * 
   * @return
   */
  boolean isMoving();

  /**
   * Writes a value in microseconds (uS) to the servo, controlling the shaft
   * accordingly. On a standard servo, this will set the angle of the shaft. On
   * standard servos a parameter value of 1000 is fully counter-clockwise, 2000
   * is fully clockwise, and 1500 is in the middle.
   *
   * Note that some manufactures do not follow this standard very closely so
   * that servos often respond to values between 700 and 2300. Feel free to
   * increase these endpoints until the servo no longer continues to increase
   * its range. Note however that attempting to drive a servo past its endpoints
   * (often indicated by a growling sound) is a high-current state, and should
   * be avoided.
   *
   * Continuous-rotation servos will respond to the writeMicrosecond function in
   * an analogous manner to the write function.
   * 
   * @param uS
   */
  void writeMicroseconds(int uS);

}
