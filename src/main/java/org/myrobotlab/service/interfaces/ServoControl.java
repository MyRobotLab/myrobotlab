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
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderListener;

public interface ServoControl extends AbsolutePositionControl, EncoderListener, Attachable, StateSaver, org.myrobotlab.framework.interfaces.StatePublisher {

  /**
   * attaches a servo data listener for servo data - like position information
   * 
   * @param listener
   */
  void attach(ServoController listener);

  /**
   * remove the listener
   * 
   * @param listener
   */
  void detach(ServoController listener);

  /**
   * disable the PWM pulses/power to the servo/motor
   */
  void disable();

  /**
   * enable the PWM pulses/power to the servo
   */
  @Config
  void enable();

  /**
   * isAutoDisable return value set by setAutoDisable
   * 
   * @return boolean
   */
  boolean isAutoDisable();

  /**
   * @return name of the current controllers - empty if not set
   * 
   */
  String getController();

  /**
   * returns the encoder attached to this ServoControl
   * 
   * @return - the Encoder
   */
  EncoderControl getEncoder();

  /**
   * @return The last time the servo was asked to move (system current time in
   *         ms?)
   * 
   */
  long getLastActivityTime();

  /**
   * @return get this servos mapper
   * 
   */
  Mapper getMapper();

  /**
   * gets the Max Y of the mapper (output)
   * 
   * @return maxY
   */
  double getMax();

  /**
   * gets the minY of the mapper (output)
   * 
   * @return minY
   */
  double getMin();

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
   * @return the current input position of the servo. For a typical hobby servo
   *         this is estimated based on a TimerEncoder.
   */
  double getCurrentInputPos();

  /**
   * @return the current output "real" position of the servo. For a typical
   *         hobby servo this is estimated based on a TimerEncoder.
   */
  double getCurrentOutputPos();

  /**
   * @return the current rest position value
   */
  double getRest();

  /**
   * @return current speed if set - if speed/speed control is not being use it
   *         is null.
   * 
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
  double getTargetOutput();

  /**
   * @return This method returns the input target position of the servo. This is
   *         the input position that the servo was requested to move to.
   * 
   */
  double getTargetPos();

  /**
   * @return When moveBlocking is in motion, not only should it block the
   *         calling thread until the end of the move, it should also prevent
   *         (cancel) other threads (even ones doing moveTo commands) until its
   *         done... conversely mutli-threaded moveTo commands are a
   *         free-for-all .. if you call a servo thats in process of a
   *         moveBlocking with a moveTo - your moveTo is canceled (not blocked)
   *         until the moveToBlocking is done. When a moveToBlocking is called
   *         from a different thread it should be blocked until the original is
   *         finished.
   * 
   */
  boolean isBlocking();

  /**
   * @return is the servo currently sending pwm position control
   */
  boolean isEnabled();

  /**
   * @return true if mapper is inverted
   * 
   */
  boolean isInverted();

  /**
   * @return if the sevo is currently moving
   * 
   */
  boolean isMoving();

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
  void map(double minX, double maxX, double minY, double maxY);

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
  @Override
  Double moveToBlocking(Double pos);

  /**
   * moveToBlocking with a timeout blocking calling thread until either move has
   * been completed, or timeout reached
   */
  @Override
  Double moveToBlocking(Double pos, Long timeoutMs);

  /**
   * command to move to the rest position
   */
  void rest();

  /**
   * setAutoDisable tell the servo to disable when position reached this make
   * sense only if speed &gt; 0 if speed == -1 : a timer is launched to delay
   * disable
   * 
   * @param autoDisable
   *          - boolean
   */
  void setAutoDisable(boolean autoDisable);

  /**
   * invert the map so a servo will go in reverse direction 0 == 180, 90 == 90,
   * 180 == 0
   * 
   * @param invert
   *          - true is to invert
   */
  void setInverted(boolean invert);

  /**
   * set a mapper to do the mapping between input and output for this servo
   * control
   * 
   * @param m
   */
  void setMapper(Mapper m);

  /**
   * This specifies both the input and the output limits for the mapper. It
   * specifies the minY and maxY This method is deprecated, as it's ambigious as
   * to the behavior. use map(minXY,maxXY,minXY,maxXY) instead
   * 
   * @param minXY
   *          minXY value
   * @param maxXY
   *          maxXY value
   * 
   */
  @Deprecated
  void setMinMax(double minXY, double maxXY);

  /**
   * Helper function that can be used to set the output limits on an existing
   * mapper. This will leave the input limits unchanged on the mapper and it
   * will set the output minY and maxY values on the mapper.
   * 
   * @param minY
   *          the output minimum value of the mapper
   * @param maxY
   *          the output maxiumum value of the mapper.
   */
  void setMinMaxOutput(double minY, double maxY);

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
   * This method sets the input position without "moving" the servo. Typically,
   * this is useful for setting the initial position of the servo during startup
   * 
   * @param pos
   */
  void setPosition(double pos);

  /**
   * @param rest
   *          A default position for the servo. Defaulted to 90 unless
   *          explicitly set. Position the servo will move to when method
   *          servo.rest() is called
   */
  void setRest(double rest);

  public void setSpeed(Integer degreesPerSecond);

  /**
   * set the speed of the servo measured in degrees per second.
   * 
   * @param degreesPerSecond
   */
  void setSpeed(Double degreesPerSecond);

  /**
   * stops the servo if currently in motion servo must be moving at incremental
   * speed for a stop to work (setSpeed &lt; 1.0)
   */
  void stop();

  /**
   * synchronizing servos together e.g. leftEye.sync(rightEye)
   * 
   * @param sc
   *          the servo that's being synched e.g. master.synch(slave)
   */
  void sync(ServoControl sc);

  /**
   * synchronizing servos together e.g. leftEye.sync("rightEye")
   * 
   * @param name
   *          name that's being synched e.g. master.synch("slave")
   */
  void sync(String name);

  /**
   * unsync a servo
   * 
   * @param name
   *          of the servo being synched
   */
  void unsync(String name);

  /**
   * unsync a servo
   * 
   * @param sc
   *          reference of the servo beign synched
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

  // for instance attachment
  void attachServoController(String sc);

  /**
   * disable speed control and move the servos at full speed.
   */
  @Deprecated /* implement setSpeed(null) */
  void fullSpeed();

}
