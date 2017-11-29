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

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.MotorControl;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         The Chassis service is for general movement control. The criteria for
 *         using this service is a 2 motor differential drive and parameters of
 *         shape / size of platform. It will probably need relative location of
 *         motors and size of wheels. A switch for metric/imperial units of
 *         measure would be nice. It will also need feedback interfaces. Some
 *         feedback devices could include webcam tracking, motor or wheel
 *         encoders, compass, gyros, or accelerometers. Possibly all these
 *         feedback devices will resolve into a RelativePosition or
 *         AbsolutePosition interface.
 * 
 *         Possible expectations : motors will have to be local
 * 
 *         http://en.wikipedia.org/wiki/Dead_reckoning#
 *         Differential_steer_drive_dead_reckoning
 */
@Deprecated // use Chassis
public class MobilePlatform extends Service {
 
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MobilePlatform.class);

  public int positionX = 0;
  public int positionY = 0;
  public int targetX = 0;

  public int targetY = 0;
  
  public Double headingCurrent = 0.0;
  public Double headingTarget = 0.0;
  public Double headingLast = 0.0;
  public Double headingDelta = 0.0;
  
  transient MotorControl left = null;
  transient MotorControl right = null;
  
  // String directionTarget = null;

  // TODO - determine if control needs to be serialized
  // Lock for "syncing" time vs read write contention - don't want a turning
  // thread spinning too tightly
  transient private final Object lock = new Object();

  boolean isTurning = false;
  // calibration / tuning
  // min power start - this can change as battery dies
  public Float minPowerToStartRight = new Float(0);

  public Float minPowerToStartLeft = new Float(0);
  public Float MotorPowerToTurnRatioRight = new Float(0); // forwards/backwards
  // ?
  public Float MotorPowerToTurnRatioLeft = new Float(0);

  public Float MotorPowerToTurnRatioBoth = new Float(0);

  public Float currentTurnRate = new Float(0); // signed for directionCurrent?
  // lag
  public Float feedbackLagTime = new Float(0);

  public Float controlbackLagTime = new Float(0);
  // drift overshoot?

  public Float speed = new Float(0);
  // TODO - fault Timer
  public long updateHeadingTime = 0;
  public long updateHeadingTimeLast = 0;
  public long updatePositionTime = 0;

  public long updatePositionTimeLast = 0;
  public long beginMotion = 0;

  public long endMotion = 0;

  /*
   * This is a (first) attempt of making a Pid (PD) (P) controller for turning.
   * This article
   * (http://www.inpharmix.com/jps/PID_Controller_For_Lego_Mindstorms_Robots
   * .html) was EXTREMELY helpful for someone (like me) who has never
   * implemented a Pid controller.
   * 
   * The added complexity for Video Tracking feed back is the HUGE delay in the
   * feedback stream (up to 1.5 seconds) TODO - encapsulate into a utility -
   * generalize for all to use PIDUtil PIDThread Reference :
   * http://www.arduino.cc/playground/Code/PIDLibrary - Arduino's library, would
   * be helpful on local Pid applications http://brettbeauregard.com/blog
   * /2011/04/improving-the-beginners-pid-introduction/ - quick and excellent
   * explanation http://en.wikipedia.org/wiki/Lead-lag_compensator - for
   * lead/lag compensation http://brettbeauregard.com/blog/2011/04/improving-the
   * -beginners-pid-introduction/ - REALLY NICE FORMULA/CODE SNIPPET
   * 
   * startHeadingPID startDistancePID ... or one Pid two errors?
   * 
   * deltaHeading ~= error
   * 
   * offset already done will need a max power value - don't want it going at
   * 100% power to get to anywhere (i think)
   * 
   * turn = Kp * deltaHeading | Turn = Kp*(error)
   * 
   * Tp = tunable parameter (base power)
   * 
   * rightPower = Tp - turn leftPower = TP + turn
   * 
   * Turn = Kp*(error) + Ki*(integral) + Kd*(derivative) + the complexity of
   * error being 1.5 second lag
   */

  public boolean inMotion = false;

  public MobilePlatform(String n) {
    super(n);
  }

  public void attach(Motor left, Motor right) {
    this.left = left;
    this.right = right;
  }

  // absolute / relative functions end -------------------

  public void calibrate() {
    // check for feedback devices
    // check if valid motors exist
    // time and power can fluctuate so there is a graph of power & time per
    // distance
    // we will simplify by making a set time (1 sec?) to calibrate - might
    // want to have an array of power values
    // THERE IS A HUGE difference between at rest and in motion when
    // applying power

    // HEADING CALIBRATION BEGIN ----
    // begin turn right
    // wait for feedback
    // compute LAG
    // computer right ratio power (some set time) distance

    // find lag - use high power level to guarantee movement
    // we are going to do a spin turn for a very short time to get to the
    // correct heading

    /*
     * Lag time beginMotion = System.currentTimeMillis();
     * 
     * right.move(0.4f); left.move(-0.4f);
     * 
     * float heading = waitForHeadingChange(); // blocks on feedback system
     * 
     * right.stop(); left.stop(); endMotion = System.currentTimeMillis();
     * 
     * log.error("hc? " + heading + " hc " + headingCurrent + " hl " +
     * headingLast); log.error("lagTime " + (endMotion - beginMotion));
     */

    // attempt to go 10 degrees
    try {

      beginMotion = System.currentTimeMillis();

      right.move(0.13f);
      left.move(-0.13f);

      Thread.sleep(9000);

      right.stop();
      left.stop();

      endMotion = System.currentTimeMillis();

      log.error("move time " + (endMotion - beginMotion));

    } catch (InterruptedException e) {
      log.info("shutting down");
    }

  }

  // getters setters begin -------------------
  public MotorControl getLeftMotor() {
    return left;
  }

  // creators - getters setters
  public MotorControl getRightMotor() {
    return right;
  }

  // relative position control
  public void move(float power) {
    right.move(power);
    left.move(power);
  }

  // absolute position control
  public void moveTo(float distance) {

  }

  // new state function
  public MobilePlatform publishState(MobilePlatform t) {
    return t;
  }

  // FEEDBACK related begin ------------------------
  /*
   * setHeading is to be used by feedback mechanisms encoders, hall effect,
   * optical tracking It "invokes" to message the listeners of changed state.
   */
  public final void setHeading(double value) {

    synchronized (lock) {
      // set times of feedback
      updateHeadingTimeLast = updateHeadingTime;
      updateHeadingTime = System.currentTimeMillis();
      headingLast = headingCurrent;
      headingCurrent = value;
      lock.notifyAll();
    }

    // adjust other values

    headingDelta = headingCurrent - headingTarget;
    headingDelta = (headingDelta > 180) ? -(180 - (headingDelta - 180)) : headingDelta;
    headingDelta = (headingDelta < -180) ? (180 + (headingDelta + 180)) : headingDelta;

    double at = (headingTarget < 0) ? headingTarget + 180 : headingTarget - 180;

    // TODO - speed adjustment
    if (((headingCurrent < at) && (at < headingTarget)) || ((at < headingTarget) && (headingTarget < headingCurrent))
        || ((headingTarget < headingCurrent) && (headingCurrent < at))) {
      log.error("turn right");
      // directionTarget = DIRECTION_RIGHT;
    } else {
      log.error("turn left");
      // directionTarget = DIRECTION_LEFT;
    }

    // TODO configurable publishing
    // invoke("publishState", this);
  }

  // control functions end -------------------

  public void setLeftMotor(Motor left) {
    this.left = left;
  }

  public void setPosition(int x, int y) {
    // set times of feedback
    updatePositionTimeLast = updatePositionTime;
    updatePositionTime = System.currentTimeMillis();

    synchronized (lock) {
      positionX = x;
      positionY = y;
      lock.notifyAll();
    }

    // TODO configurable publishing
    invoke("publishState", this);

  }

  public void setRightMotor(Motor right) {
    this.right = right;
  }

  // command to change heading and/or position
  public void setTargetHeading(double value) // maintainHeading ?? if Pid is
  // operating
  {
    headingTarget = value;
    setHeading(headingCurrent);// HACK? - a way to get all of the
    // recalculations publish
  }

  public void setTargetPosition(int x, int y) {
    targetX = x;
    targetY = y;
  }

  // control functions begin -------------------
  public void spinLeft(float power) {
    right.move(-power);
    left.move(power);
  }

  // getters setters end --------------------------

  public void spinRight(float power) {
    right.move(power);
    left.move(-power);
  }


  // from motor interface begin-------
  public void stop() {
    right.stop();
    left.stop();
  }

  public void stopAndLock() {
    right.stopAndLock();
    left.stopAndLock();
  }

  
  // waitForHeadingChange will block and wait for heading change
  public final double waitForHeadingChange() {
    synchronized (lock) {
      try {
        lock.wait();
      } catch (InterruptedException e) {
    	  log.info("lock interrupted");
      }
    }

    return headingCurrent;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MobilePlatform.class);
    meta.addDescription(
        "used to encapsulate many of the functions and formulas regarding 2 motor platforms encoders and other feedback mechanisms can be added to provide heading, location and other information");
    meta.addCategory("robot", "control");
    meta.setAvailable(false);

    return meta;
  }
  
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // Android template = (Android) Runtime.start("template",
      // "_TemplateService");

      Runtime.start("mobile", "MobilePlatform");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
