/**
 *                    
 * @author GroG (at) myrobotlab.org
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

package org.myrobotlab.service;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.TimeEncoder;
import org.myrobotlab.service.abstracts.AbstractServo;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0.0 - 180.0, and output can relay those values
 *         directly to the servo's firmware (Arduino ServoLib, I2C controller,
 *         etc)
 * 
 *         However there can be the occasion that the input comes from a system
 *         which does not have the same range. Such that input can vary from 0.0
 *         to 1.0. For example, OpenCV coordinates are often returned in this
 *         range. When a mapping is needed Servo.map can be used. For this
 *         mapping Servo.map(0.0, 1.0, 0, 180) might be desired. Reversing input
 *         would be done with Servo.map(180, 0, 0, 180)
 * 
 *         outputY - is the values sent to the firmware, and should not
 *         necessarily be confused with the inputX which is the input values
 *         sent to the servo
 * 
 *         FIXME - inherit from AbstractMotor ..
 * 
 */

public class Servo extends AbstractServo implements ServoControl, ServiceLifeCycleListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Servo.class);

  public Servo(String n, String id) {
    super(n, id);
  }

  /**
   * max complexity moveTo
   * 
   * FIXME - move is more general and could be the "max" complexity method with
   * positional information supplied
   * 
   * @param newPos
   * @param blocking
   * @param timeoutMs
   */
  protected boolean processMove(Double newPos, boolean blocking, Long timeoutMs) {

    // This is to allow attaching disabled
    // then delay enabling until the first moveTo command
    // is used
    if (firstMove && !enabled) {
      enable();
      firstMove = false;
    }

    if (autoDisable && !enabled) {
      // if the servo was disable with a timer - re-enable it
      enable();
    }
    // purge any timers currently in process
    // if currently configured to autoDisable - the timer starts now
    // we cancel any pre-existing timer if it exists
    purgeTask("disable");
    // blocking move will be idleTime out enabled later.

    if (!enabled) {
      log.info("cannot moveTo {} not enabled", getName());
      return false;
    }
    targetPos = newPos;
    log.debug("pos {} output {}", targetPos, getTargetOutput());

    /**
     * <pre>
     * 
     * BLOCKING 
     *   
     *   if isBlocking already, and incoming request is not blocking - we cancel it 
     *   if isBlocking already, and incoming request is a blocking one - we block it
     *   if not currently blocking, and incoming request is blocking - we start blocking 
     *               with default encoder until it - unblocks or max-timeout is reached
     * 
     * </pre>
     *
     */
    // FIXME - poor implementation - should addListener(sync)
    // and use pub/sub :(
    for (String syncServo : syncedServos) {
      send(syncServo, "moveTo", newPos);
    }
    // TODO: this block isn't tested by ServoTest
    if (isBlocking && !blocking) {
      // if isBlocking already, and incoming request is not blocking - we cancel
      log.info("{} is currently blocking - ignoring request to moveTo({})", getName(), newPos);
      return false;
    }

    broadcast("publishServoMoveTo", new ServoMove(getName(), newPos, mapper.calcOutput(newPos)));

    // TODO: this block isn't tested by ServoTest
    if (isBlocking && blocking) {
      // if isBlocking already, and incoming request is a blocking one - we
      // block it
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          /* don't care */
        }
      }
      return false;
    }
    if (!isBlocking && blocking) {
      // if not currently blocking, and incoming request is blocking - we start
      // blocking with default encoder until it - unblocks or max-timeout is
      // reached - if timeout not specified - we block until an encoder unblocks
      // us
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      isBlocking = true;
    }

    lastActivityTimeTs = System.currentTimeMillis();
    isMoving = true;
    // "real" encoders are electrically hooked up to the servo and get their
    // events through
    // data lines - faux encoders need to be told in software when servos begin
    // movement
    // usually knowing about encoder type is "bad" but the timer encoder is the
    // default native encoder
    Long blockingTimeMs = null;
    if (encoder != null && encoder instanceof TimeEncoder) {
      TimeEncoder timeEncoder = (TimeEncoder) encoder;
      // calculate trajectory calculates and processes this move
      blockingTimeMs = timeEncoder.calculateTrajectory(getCurrentOutputPos(), getTargetOutput(), getSpeed());
    }

    if (isBlocking) {
      // our thread did a blocking call - we will wait until encoder notifies us
      // to continue or timeout (if supplied) has been reached
      sleep(blockingTimeMs);
      isBlocking = false;
      isMoving = false;
      if (autoDisable) {
        // and start our countdown
        addTaskOneShot(idleTimeout, "disable");
      }
    }
    return true;
  }

  @Deprecated
  public void enableAutoDisable(boolean value) {
    setAutoDisable(value);
  }

  @Override
  public ServiceConfig getConfig() {

    ServoConfig config = new ServoConfig();

    config.autoDisable = autoDisable;
    config.enabled = enabled;

    if (mapper != null) {
      config.clip = mapper.isClip();
      config.maxIn = mapper.getMaxX();
      config.maxOut = mapper.getMaxY();
      config.minIn = mapper.getMinX();
      config.minOut = mapper.getMinY();
      config.inverted = mapper.isInverted();
    }

    // config.controller = controller;

    config.idleTimeout = idleTimeout;
    config.pin = pin;
    config.rest = rest;
    config.speed = speed;
    config.sweepMax = sweepMax;
    config.sweepMin = sweepMin;

    config.controller = this.controller;

    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    ServoConfig config = (ServoConfig) c;

    autoDisable = config.autoDisable;

    // important - if starting up
    // and autoDisable - then the assumption at this point
    // is it is currently disabled, otherwise it will take
    // a move to disable
    if (config.autoDisable) {
      disable();
    }
    if (config.minIn != null && config.maxIn != null && config.minOut != null && config.maxOut != null) {
      mapper = new MapperLinear(config.minIn, config.maxIn, config.minOut, config.maxOut);
    }
    mapper.setInverted(config.inverted);
    mapper.setClip(config.clip);
    enabled = config.enabled;
    if (config.idleTimeout != null) {
      idleTimeout = config.idleTimeout;
    }
    pin = config.pin;

    speed = config.speed;
    sweepMax = config.sweepMax;
    sweepMin = config.sweepMin;

    // rest = config.rest;
    if (config.rest != null) {
      rest = config.rest;
      targetPos = config.rest;
      currentOutputPos = mapper.calcOutput(config.rest);
      broadcast("publishEncoderData", new EncoderData(getName(), pin, config.rest, config.rest));
    }
    
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch (Exception e) {
        error(e);
      }
    }

    return c;
  }

  public static void main(String[] args) throws InterruptedException {
    try {

      // log.info("{}","blah$Blah".contains("$"));

      Runtime.main(new String[] { "--from-launcher", "--id", "servo" });
      // LoggingFactory.init(Level.INFO);
      // Platform.setVirtual(true);

      // Runtime.start("python", "Python");
      Runtime runtime = Runtime.getInstance();
      runtime.load();

      boolean done = true;
      if (done) {
        return;
      }

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      Servo tilt = (Servo) Runtime.start("tilt", "Servo");
      Servo pan = (Servo) Runtime.start("pan", "Servo");

      tilt.setPin(4);
      pan.setPin(5);
      tilt.setMinMax(10, 100);
      pan.setMinMax(5, 105);
      tilt.setInverted(true);

      mega.connect("/dev/ttyACM0");

      mega.attach(tilt);
      mega.attach(pan);

      runtime.save();

      /*
       * mega.save(); tilt.save(); pan.save();
       * 
       * mega.load(); tilt.load(); pan.load();
       */

      // TODO - attach before and after connect..

      // mega.setBoardMega();

      // log.info("servo pos {}", tilt.getCurrentInputPos());
      //
      // // double pos = 170;
      // // servo03.setPosition(pos);
      //
      // double min = 3;
      // double max = 170;
      // double speed = 60; // degree/s
      //
      // mega.attach(tilt);
      // // mega.attach(servo03,3);
      //
      // for (int i = 0; i < 100; ++i) {
      // tilt.moveTo(20.0);
      // }
      //
      // tilt.sweep(min, max, speed);

      /*
       * Servo servo04 = (Servo) Runtime.start("servo04", "Servo"); Servo
       * servo05 = (Servo) Runtime.start("servo05", "Servo"); Servo servo06 =
       * (Servo) Runtime.start("servo06", "Servo"); Servo servo07 = (Servo)
       * Runtime.start("servo07", "Servo"); Servo servo08 = (Servo)
       * Runtime.start("servo08", "Servo"); Servo servo09 = (Servo)
       * Runtime.start("servo09", "Servo"); Servo servo10 = (Servo)
       * Runtime.start("servo10", "Servo"); Servo servo11 = (Servo)
       * Runtime.start("servo11", "Servo"); Servo servo12 = (Servo)
       * Runtime.start("servo12", "Servo");
       */
      // Servo servo13 = (Servo) Runtime.start("servo13", "Servo");

      // servo03.attach(mega, 8, 38.0);
      /*
       * servo04.attach(mega, 4, 38.0); servo05.attach(mega, 5, 38.0);
       * servo06.attach(mega, 6, 38.0); servo07.attach(mega, 7, 38.0);
       * servo08.attach(mega, 8, 38.0); servo09.attach(mega, 9, 38.0);
       * servo10.attach(mega, 10, 38.0); servo11.attach(mega, 11, 38.0);
       * servo12.attach(mega, 12, 38.0);
       */

      // TestCatcher catcher = (TestCatcher)Runtime.start("catcher",
      // "TestCatcher");
      // servo03.attach((ServoEventListener)catcher);

      // servo.setPin(12);

      /*
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       */

      // servo.sweepDelay = 3;
      // servo.save();
      // servo.load();
      // servo.save();
      // log.info("sweepDely {}", servo.sweepDelay);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void onCreated(String name) {
  }

  @Override
  public void onStopped(String name) {
  }

  @Override
  public void onReleased(String name) {
  }

}
