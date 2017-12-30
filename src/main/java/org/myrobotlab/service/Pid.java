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
 * References :
 * A port of the great library of
 * 
 * Arduino Pid Library - Version 1.0.1
 * by Brett Beauregard <br3ttb@gmail.com> brettbeauregard.com
 *
 * This Library is licensed under a GPLv3 License
 * 
 * http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/
 * 
 * Thanks Brett !
 * */

package org.myrobotlab.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * Pid - control service from
 * 
 * http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-
 * introduction/ This will likely get merged/replaced with Pid service.
 * 
 */
public class Pid extends Service {

  public static class PidData implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * original user entered data for Kp value
     */
    private double dispKp;

    /**
     * original user entered data for Ki value
     */
    private double dispKi;

    /**
     * original user data entered for Kd value
     */
    private double dispKd;

    /**
     * (P)roportional Tuning Parameter
     */
    private double kp;
    /**
     * (I)ntegral Tuning Parameter
     */
    private double ki;
    /**
     * (D)erivative Tuning Parameter
     */
    private double kd;

    private int controllerDirection;

    private double input; // * Pointers to the Input, Output, and Setpoint
    // variables
    private double output; // This creates a hard link between the variables
    // and
    // the
    private double setpoint; // Pid, freeing the user from having to
    // constantly
    // tell us
    // what these values are. with pointers we'll just know.

    private long lastTime;
    private double ITerm, lastInput;

    private long sampleTime = 100; // default Controller Sample Time is 0.1
    // seconds
    private double outMin, outMax, outCenter;
    private boolean inAuto;
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Pid.class.getCanonicalName());
  // mode
  static final public int MODE_AUTOMATIC = 1;

  static final public int MODE_MANUAL = 0;
  // direction
  static final public int DIRECTION_DIRECT = 0;

  static final public int DIRECTION_REVERSE = 1;

  public Map<String, PidData> data = new HashMap<String, PidData>();

  public Pid(String n) {
    super(n);
  }

  /*
   * compute()
   * **********************************************************************
   * This, as they say, is where the magic happens. this function should be
   * called every time "void loop()" executes. the function will decide for
   * itself whether a new pid Output needs to be computed. returns true when the
   * output is computed, false when nothing has been done. *****************
   * ***************************************************************
   */
  public boolean compute(String key) {
    PidData piddata = data.get(key);

    if (!piddata.inAuto)
      return false;
    long now = System.currentTimeMillis();
    long timeChange = (now - piddata.lastTime);
    if (timeChange >= piddata.sampleTime) {
      // ++sampleCount;
      /* compute all the working error variables */
      double error = piddata.setpoint - piddata.input;
      piddata.ITerm += (piddata.ki * error);
      if (piddata.ITerm > piddata.outMax)
        piddata.ITerm = piddata.outMax;
      else if (piddata.ITerm < piddata.outMin)
        piddata.ITerm = piddata.outMin;
      double dInput = (piddata.input - piddata.lastInput);

      /* compute Pid Output */
      double output = piddata.kp * error + piddata.ITerm - piddata.kd * dInput;

      if (output > piddata.outMax)
        output = piddata.outMax;
      else if (output < piddata.outMin)
        output = piddata.outMin;
      piddata.output = output;

      broadcastState();

      /* Remember some variables for next time */
      piddata.lastInput = piddata.input;
      piddata.lastTime = now;
      return true;
    } else
      return false;
  }

  public void direct(String key) {
    setControllerDirection(key, DIRECTION_DIRECT);
  }

  public int getControllerDirection(String key) {
    PidData piddata = data.get(key);
    return piddata.controllerDirection;
  }

  public double getKd(String key) {
    PidData piddata = data.get(key);
    return piddata.dispKd;
  }

  public double getKi(String key) {
    PidData piddata = data.get(key);
    return piddata.dispKi;
  }

  public double getKp(String key) {
    PidData piddata = data.get(key);
    return piddata.dispKp;
  }

  public int getMode(String key) {
    PidData piddata = data.get(key);
    return piddata.inAuto ? MODE_AUTOMATIC : MODE_MANUAL;
  }

  public double getOutput(String key) {
    PidData piddata = data.get(key);
    return piddata.output + piddata.outCenter;
  }

  public void setOutput(String key, double Output) {
    setMode(key, MODE_MANUAL);
    PidData piddata = data.get(key);
    piddata.output = Output - piddata.outCenter;
  }

  public double getSetpoint(String key) {
    PidData piddata = data.get(key);
    return piddata.setpoint;
  }

  /**
   * does all the things that need to happen to ensure a bumpless transfer
   * from manual to automatic mode. 
   */
  public void init(String key) {
    PidData piddata = data.get(key);
    piddata.ITerm = piddata.output;
    piddata.lastInput = piddata.input;
    if (piddata.ITerm > piddata.outMax) {
      piddata.ITerm = piddata.outMax;
    } else if (piddata.ITerm < piddata.outMin) {
      piddata.ITerm = piddata.outMin;
    }

    piddata.lastTime = System.currentTimeMillis() - piddata.sampleTime; // FIXME
    // -
    // is
    // this
    // correct ??? (was
    // in constructor)
  }

  public void invert(String key) {
    setControllerDirection(key, DIRECTION_REVERSE);
  }

  /*
   * SetControllerDirection(...)***********************************************
   * ** The Pid will either be connected to a DIRECT acting process (+Output
   * leads to +Input) or a REVERSE acting process(+Output leads to -Input.) we
   * need to know which one, because otherwise we may increase the output when
   * we should be decreasing. This is called from the constructor. *************
   * ***************************************************************
   */
  public void setControllerDirection(String key, Integer direction) {
    PidData piddata = data.get(key);
    if (piddata.inAuto && direction != piddata.controllerDirection) {
      piddata.kp = (0 - piddata.kp);
      piddata.ki = (0 - piddata.ki);
      piddata.kd = (0 - piddata.kd);
    }
    piddata.controllerDirection = direction;
    broadcastState();
  }

  public void setInput(String key, double input) {
    PidData piddata = data.get(key);
    piddata.input = input;
  }

  /**
   * Allows the controller Mode to be set to manual (0) or Automatic (non-zero)
   * when the transition from manual to auto occurs, the controller is
   * automatically initialized
   */
  public void setMode(String key, int Mode) {
    PidData piddata = data.get(key);
    boolean newAuto = (Mode == MODE_AUTOMATIC);
    if ((newAuto == !piddata.inAuto)
        && (Mode == MODE_AUTOMATIC)) { /* we just went from manual to auto */
      init(key);
    }
    piddata.inAuto = newAuto;
    broadcastState();
  }

  /**
   * This function will be used far more often than SetInputLimits. while the
   * input to the controller will generally be in the 0-1023 range (which is the
   * default already,) the output will be a little different. maybe they'll be
   * doing a time window and will need 0-8000 or something. or maybe they'll
   * want to clamp it from 0-125. who knows. at any rate, that can all be done
   * here.
   * 
   * @param key
   *          - named pid compute instance, so the Pid "service" can manage pid
   *          systems
   * @param min
   * @param max
   * 
   */
  public void setOutputRange(String key, double min, double max) {
    PidData piddata = data.get(key);
    if (min >= max) {
      error("min {} >= max {}", min, max);
      return;
    }

    piddata.outCenter = (min + max) / 2;
    piddata.outMin = min - piddata.outCenter;
    piddata.outMax = max - piddata.outCenter;

    if (piddata.inAuto) {
      if (piddata.output > piddata.outMax)
        piddata.output = piddata.outMax;
      else if (piddata.output < piddata.outMin)
        piddata.output = piddata.outMin;

      if (piddata.ITerm > piddata.outMax)
        piddata.ITerm = piddata.outMax;
      else if (piddata.ITerm < piddata.outMin)
        piddata.ITerm = piddata.outMin;
    }
    broadcastState();
  }

  /**
   * This function allows the controller's dynamic performance to be adjusted.
   * it's called automatically from the constructor, but tunings can also be
   * adjusted on the fly during normal operation
   * 
   * @param key
   *          - named pid compute instance, so the Pid "service" can manage pid
   *          systems
   * @param Kp
   *          - constant proportional value
   * @param Ki
   *          - constant integral value
   * @param Kd
   *          - constant derivative value
   */
  public void setPID(String key, Double Kp, Double Ki, Double Kd) {
    PidData piddata = new PidData();

    if (Kp < 0 || Ki < 0 || Kd < 0) {
      error("kp < 0 || ki < 0 || kd < 0");
      return;
    }

    if (data.containsKey(key)) {
      piddata = data.get(key);
    }

    piddata.dispKp = Kp;
    piddata.dispKi = Ki;
    piddata.dispKd = Kd;

    double SampleTimeInSec = ((double) piddata.sampleTime) / 1000;
    piddata.kp = Kp;
    piddata.ki = Ki * SampleTimeInSec;
    piddata.kd = Kd / SampleTimeInSec;

    if (piddata.controllerDirection == DIRECTION_REVERSE) {
      piddata.kp = (0 - piddata.kp);
      piddata.ki = (0 - piddata.ki);
      piddata.kd = (0 - piddata.kd);
    }

    data.put(key, piddata);
    broadcastState();
  }

  /*
   * setSampleTime(...)
   * ********************************************************* sets the period,
   * in Milliseconds, at which the calculation is performed ************
   * ****************************************************************
   */
  public void setSampleTime(String key, int NewSampleTime) {
    PidData piddata = data.get(key);
    if (NewSampleTime > 0) {
      double ratio = (double) NewSampleTime / (double) piddata.sampleTime;
      piddata.ki *= ratio;
      piddata.kd /= ratio;
      piddata.sampleTime = NewSampleTime;
    }

    broadcastState();
  }

  public void setSetpoint(String key, double setPoint) {
    PidData piddata = data.get(key);
    piddata.setpoint = setPoint;
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

    ServiceType meta = new ServiceType(Pid.class.getCanonicalName());
    meta.addDescription("A proportional integral derivative controller (Pid controller) commonly used in industrial control systems");
    meta.addCategory("control", "industrial");
    return meta;
  }

  public Map<String, PidData> getPidData() {
    return data;
  }

  public static void main(String[] args) throws ClassNotFoundException {

    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("gui", "SwingGui");
      Pid pid = (Pid) Runtime.start("pid", "Pid");
      String key = "test";
      pid.setPID(key, 2.0, 5.0, 1.0);
      pid.setControllerDirection(key, DIRECTION_DIRECT);
      // pid.setMode(key, MODE_AUTOMATIC);
      pid.setOutputRange(key, 0, 255);
      pid.setSetpoint(key, 100);
      pid.setSampleTime(key, 40);

      // SwingGui gui = new SwingGui("gui");
      // gui.startService();

      for (int i = 0; i < 200; ++i) {
        pid.setInput(key, i);
        Service.sleep(30);
        if (pid.compute(key)) {
          log.info(String.format("%d %f", i, pid.getOutput(key)));
        }
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
