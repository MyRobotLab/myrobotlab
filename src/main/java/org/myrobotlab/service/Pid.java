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
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.PidControl;
import org.slf4j.Logger;

/**
 * 
 * Pid - a controller service, supports multiple pid loops
 * 
 * General idea gratefully inspired by:
 * http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-
 * introduction/
 * 
 * TODO - handle integral windup - reset to avoid windup - bumpless transfer
 * usually sets output and removes sets the ITerm to 0
 * https://en.wikipedia.org/wiki/PID_controller#Integral_windup
 * 
 */
public class Pid extends Service implements PidControl {

  public static class PidOutput {
    public long ts;
    public String src;
    public String key;
    public Double input;
    public Double value;

    public PidOutput(String src, String key, long ts, double input, double output) {
      this.src = src;
      this.key = key;
      this.ts = ts;
      this.input = input;
      this.value = output;
    }

    public String toString() {
      return String.format("%s %d %f %f", key, ts, input, value);
    }
  }

  public static class PidData implements Serializable {

    private static final long serialVersionUID = 1L;

    transient public boolean firstTime = true;

    /**
     * The key identifier of this control loop. This is how to support multiple
     * pid loops with different tuning.
     */
    transient public String key;

    /**
     * (P)roportional Tuning Parameter
     */
    public double kp;
    /**
     * (I)ntegral Tuning Parameter
     */
    public double ki;
    /**
     * (D)erivative Tuning Parameter
     */
    public double kd;

    
    public boolean inverted = false;

    /**
     * saved current input value
     */
    transient public double input;

    /**
     * saved current output value
     */
    transient public double output;

    /**
     * the set point where the pid "wishes" to be
     */
    public double setpoint;

    /**
     * deadband amount before output is changed
     */
    public double deadband;

    transient public long lastTime;

    transient public double iTerm;

    transient public double lastInput;

    public long sampleTime = 100;

    public Double outMin;

    public Double outMax;

    public double outCenter = 0.0;

    /**
     * enabled == true pid will be calculate and published, enabled == false
     * will not
     */
    public boolean enabled = true;

    @Deprecated /*
                 * what is inAuto auto vs manual ? does it even make sense in
                 * this context ?
                 */
    public boolean inAuto = true;

    public String toString() {
      return String.format("kp %f ki %f kd %f inverted %b input %f output %f setpoint %f deadband %f outMin %f outMax %f", kp, ki, kd, inverted, input, output, setpoint, deadband,
          outMin, outMax);
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Pid.class.getCanonicalName());

  static final public int MODE_AUTOMATIC = 1;

  @Deprecated /*
               * copied from C code - this should be a boolean called inverted
               */
  static final public int MODE_MANUAL = 0;
  // direction
  @Deprecated /* use just a invert boolean */
  static final public int DIRECTION_DIRECT = 0;

  @Deprecated /* use just a invert boolean */
  static final public int DIRECTION_REVERSE = 1;

  public Map<String, PidData> data = new HashMap<String, PidData>();

  public Pid(String n, String id) {
    super(n, id);
  }

  public PidData addPid(PidData pid) {
    log.info("adding pid {}", pid);
    data.put(pid.key, pid);
    broadcastState();
    return pid;
  }

  public PidData deletePid(String key) {
    PidData pid = data.remove(key);
    if (pid != null) {
      broadcastState();
    }
    return pid;
  }

  /**
   * This, as they say, is where the magic happens. this function should be
   * called every time "void loop()" executes. the function will decide for
   * itself whether a new pid Output needs to be computed. returns true when the
   * output is computed, false when nothing has been done
   * 
   * @param key
   *          - key of pid of interest
   */
  public void direct(String key) {
    setControllerDirection(key, DIRECTION_DIRECT);
  }

  public int getControllerDirection(String key) {
    PidData piddata = data.get(key);
    return (piddata.inverted ? 1 : 0);
  }

  public boolean getInverted(String key) {
    PidData piddata = data.get(key);
    return piddata.inverted;
  }

  public boolean setInverted(String key, boolean b) {
    PidData piddata = data.get(key);
    piddata.inverted = b;
    return b;
  }

  public int getMode(String key) {
    PidData piddata = data.get(key);
    return piddata.inAuto ? MODE_AUTOMATIC : MODE_MANUAL;
  }

  /**
   * Forcefully sets the output to a manual value putting the controller into
   * manual mode
   * 
   * @param key
   *          - pid
   * @param output
   */
  public void setOutput(String key, double output) {
    setMode(key, MODE_MANUAL);
    PidData piddata = data.get(key);
    piddata.output = output - piddata.outCenter;
  }

  public double getSetpoint(String key) {
    PidData piddata = data.get(key);
    return piddata.setpoint;
  }

  /**
   * FIXME - relates to firstTime ?
   * 
   * does all the things that need to happen to ensure a bumpless transfer from
   * manual to automatic mode.
   * 
   * @param key
   *          pid key
   */
  public void init(String key) {
    PidData piddata = data.get(key);
    piddata.iTerm = piddata.output;
    piddata.lastInput = piddata.input;
    if (piddata.outMax != null && piddata.iTerm > piddata.outMax) {
      piddata.iTerm = piddata.outMax;
    } else if (piddata.outMin != null && piddata.iTerm < piddata.outMin) {
      piddata.iTerm = piddata.outMin;
    }

    piddata.lastTime = System.currentTimeMillis() - piddata.sampleTime;
  }

  public void invert(String key) {
    setControllerDirection(key, DIRECTION_REVERSE);
  }

  /**
   * SetControllerDirection - The Pid will either be connected to a DIRECT
   * acting process (+Output leads to +Input) or a REVERSE acting
   * process(+Output leads to -Input.) we need to know which one, because
   * otherwise we may increase the output when we should be decreasing. This is
   * called from the constructor.
   * 
   * DIRECT = 0 REVERSE = 1
   * 
   * @param key
   * @param direction
   */
  public void setControllerDirection(String key, Integer direction) {
    PidData piddata = data.get(key);
    boolean inverted = (direction != 0);
    if (piddata.inAuto && inverted != piddata.inverted) {
      piddata.kp = (0 - piddata.kp);
      piddata.ki = (0 - piddata.ki);
      piddata.kd = (0 - piddata.kd);
    }
    piddata.inverted = inverted;
    broadcastState();
  }

  /**
   * Allows the controller Mode to be set to manual (0) or Automatic (non-zero)
   * when the transition from manual to auto occurs, the controller is
   * automatically initialized
   * 
   * @param key
   *          pid key
   * @param Mode
   *          mode to run in.
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
   * 
   * @param key
   * @param min
   * @param max
   */
  public void setOutputRange(String key, double min, double max) {
    PidData piddata = data.get(key);
    if (min >= max) {
      error("min %.2f >= max %.2f", min, max);
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

      if (piddata.iTerm > piddata.outMax)
        piddata.iTerm = piddata.outMax;
      else if (piddata.iTerm < piddata.outMin)
        piddata.iTerm = piddata.outMin;
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
   * @param kp
   *          - constant proportional value
   * @param ki
   *          - constant integral value
   * @param kd
   *          - constant derivative value
   */
  public void setPid(String key, double kp, double ki, double kd) {
    PidData piddata = new PidData();

    if (data.containsKey(key)) {
      piddata = data.get(key);
    }

    double SampleTimeInSec = ((double) piddata.sampleTime) / 1000;
    piddata.kp = kp;
    piddata.ki = ki * SampleTimeInSec;
    piddata.kd = kd / SampleTimeInSec;

    if (piddata.inverted) {
      piddata.kp = (0 - piddata.kp);
      piddata.ki = (0 - piddata.ki);
      piddata.kd = (0 - piddata.kd);
    }
    piddata.key = key;
    data.put(key, piddata);
    broadcastState();
  }

  /**
   * setSampleTime - sets the period, in Milliseconds, at which the calculation
   * is performed
   * 
   * @param key
   * @param NewSampleTime
   */
  public void setSampleTime(String key, int NewSampleTime) {
    PidData piddata = data.get(key);
    if (NewSampleTime > 0) {
      double ratio = (double) NewSampleTime / (double) piddata.sampleTime;
      piddata.ki *= ratio;
      piddata.kd /= ratio;
      piddata.sampleTime = NewSampleTime;
    }
  }

  public void setSetpoint(String key, double setPoint) {
    PidData piddata = data.get(key);
    piddata.setpoint = setPoint;
  }

  public void setDeadBand(String key, double deadband) {
    PidData piddata = data.get(key);
    piddata.deadband = deadband;
  }

  public Map<String, PidData> getPidData() {
    return data;
  }

  public Double compute(String key, double sensorValue) {
    PidData piddata = null;
    if (data.containsKey(key)) {
      piddata = data.get(key);
    } else {
      log.error("Unknown key for PID control.  Key: {}", key);
      return null;
    }

    piddata.input = sensorValue;
    //////////////////////////////////////////////////////////////

    if (!piddata.enabled) {
      return null;
    }

    // TODO - remove this
    if (!piddata.inAuto)
      return null;

    if (piddata.firstTime) {
      init(key);
      piddata.firstTime = false;
    }

    long now = System.currentTimeMillis();

    long timeChange = (now - piddata.lastTime);

    if (timeChange < piddata.sampleTime) {
      // input overflow - will not compute
      // although its more efficient to do flow control at the source
      log.warn("{} {} overflow will not compute", getName(), key);
      return null;
    }
    // ++sampleCount;

    /* compute all the working error variables */
    double error = piddata.setpoint - piddata.input;
    piddata.iTerm += (piddata.ki * error);
    piddata.iTerm = constrain(piddata.iTerm, piddata.outMin, piddata.outMax);
    double dInput = (piddata.input - piddata.lastInput);

    /* compute Pid Output */
    double output = piddata.kp * error + piddata.iTerm - piddata.kd * dInput;

    output = constrain(output, piddata.outMin, piddata.outMax);

    if (Math.abs(piddata.output - output) > piddata.deadband) {
      piddata.output = output;
    }

    /* Remember some variables for next time */
    piddata.lastInput = piddata.input;
    piddata.lastTime = now;

    double newOutput = piddata.output + piddata.outCenter;

    // WTH is outCenter - offCenter ?
    send(getName(), "publishPid", new PidOutput(getName(), key, now, piddata.input, newOutput));

    return newOutput;
  }

  private double constrain(double value, Double min, Double max) {
    if (max != null && value > max) {
      return max;
    }
    if (min != null && value < min) {
      return min;
    }
    return value;
  }

  @Override
  public ServiceConfig getConfig() {
    PidConfig config = new PidConfig();
    config.data = data;
    return config;
  }

  @Override
  public void reset(String key) {
    PidData piddata = data.get(key);
    piddata.firstTime = true;
    piddata.iTerm = 0;
  }

  public ServiceConfig apply(ServiceConfig c) {
    PidConfig config = (PidConfig) c;
    if (config.data != null) {
      data = config.data;
      for (String key : config.data.keySet()) {
        PidData pd = config.data.get(key);
        if (pd.key == null || !pd.key.equals(key)) {
          warn("re-assigning config pid key %s to %s", pd.key, key);
        }
        pd.key = key; // normalize
        data.put(key, pd);
      }
    }
    broadcastState();
    return config;
  }

  @Override
  public boolean enable(String key, boolean b) {
    PidData piddata = data.get(key);
    piddata.enabled = b;
    return b;
  }

  public static void main(String[] args) throws ClassNotFoundException {

    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");
      Pid pid = (Pid) Runtime.start("test", "Pid");
      pid.setPid("pan", 1.0, 0.1, 0.0);
      pid.setSetpoint("pan", 320);

      pid.setPid("tilt", 1.0, 0.1, 0.0);
      pid.setSetpoint("tilt", 240);

      // pid.setControllerDirection(key, DIRECTION_DIRECT);
      // pid.setMode(key, MODE_AUTOMATIC);
      // pid.setOutputRange(key, 0, 255);
      // pid.setSampleTime(key, 40);
      boolean done = false;
      // for (int i = 0; i < 200; ++i) {
      while (!done) {

        int i = (int) ((Math.random() * (440 - 200)) + 200);
        // pid.setInput(key, i);
        // Service.sleep(30);
        if (pid.compute("pan", i) == null) {
          log.warn("pan overrun");
        }

        i = (int) ((Math.random() * (440 - 200)) + 200);

        if (pid.compute("tilt", i) == null) {
          log.warn("tilt overrun");
        }

        Service.sleep(100);
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
