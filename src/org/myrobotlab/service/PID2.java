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
 * Arduino PID2 Library - Version 1.0.1
 * by Brett Beauregard <br3ttb@gmail.com> brettbeauregard.com
 *
 * This Library is licensed under a GPLv3 License
 * 
 * http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/
 * 
 * Thanks Brett !
 * */

package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class PID2 extends Service {

	class PIDData {
		private double dispKp; // * we'll hold on to the tuning parameters in
		// user-entered
		private double dispKi; // format for display purposes
		private double dispKd; //

		private double kp; // * (P)roportional Tuning Parameter
		private double ki; // * (I)ntegral Tuning Parameter
		private double kd; // * (D)erivative Tuning Parameter

		private int controllerDirection;

		private double input; // * Pointers to the Input, Output, and Setpoint
		// variables
		private double output; // This creates a hard link between the variables
								// and
		// the
		private double setpoint; // PID2, freeing the user from having to
									// constantly
		// tell us
		// what these values are. with pointers we'll just know.

		private long lastTime;
		private double ITerm, lastInput;

		private long sampleTime = 100; // default Controller Sample Time is 0.1
		// seconds
		private double outMin, outMax;
		private boolean inAuto;
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PID2.class.getCanonicalName());
	// mode
	static final public int MODE_AUTOMATIC = 1;

	static final public int MODE_MANUAL = 0;
	// direction
	static final public int DIRECTION_DIRECT = 0;

	static final public int DIRECTION_REVERSE = 1;

	private HashMap<String, PIDData> data = new HashMap<String, PIDData>();

	public static void main(String[] args) throws ClassNotFoundException {
		// Logger root =
		// (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Logging logging = LoggingFactory.getInstance();
		logging.configure();
		logging.setLevel(Level.INFO);
		// LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			int test = 35;
			log.info("{}", test);

			log.debug("hello");
			log.trace("trace");
			log.error("error");
			log.info("info");

			PID2 pid = new PID2("pid");
			pid.startService();
			String key = "test";
			pid.setPID(key, 2.0, 5.0, 1.0);
			pid.setControllerDirection(key, DIRECTION_DIRECT);
			pid.setMode(key, MODE_AUTOMATIC);
			pid.setOutputRange(key, 0, 255);
			pid.setSetpoint(key, 100);
			pid.setSampleTime(key, 40);

			// GUIService gui = new GUIService("gui");
			// gui.startService();

			for (int i = 0; i < 200; ++i) {
				pid.setInput(key, i);
				Service.sleep(30);
				if (pid.compute(key)) {
					log.info(String.format("%d %f", i, pid.getOutput(key)));
				} else {
					log.warn("not ready");
				}
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public PID2(String n) {
		super(n);
	}

	/*
	 * compute()
	 * **********************************************************************
	 * This, as they say, is where the magic happens. this function should be
	 * called every time "void loop()" executes. the function will decide for
	 * itself whether a new pid Output needs to be computed. returns true when
	 * the output is computed, false when nothing has been done.
	 * *****************
	 * ***************************************************************
	 */
	public boolean compute(String key) {
		PIDData piddata = data.get(key);

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

			/* compute PID2 Output */
			double output = piddata.kp * error + piddata.ITerm - piddata.kd * dInput;

			if (output > piddata.outMax)
				output = piddata.outMax;
			else if (output < piddata.outMin)
				output = piddata.outMin;
			piddata.output = output;

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

	@Override
	public String[] getCategories() {
		return new String[] { "control" };
	}

	public int getControllerDirection(String key) {
		PIDData piddata = data.get(key);
		return piddata.controllerDirection;
	}

	@Override
	public String getDescription() {
		return "<html>a PID2 control service from<br>" + "http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/</html>";
	}

	public double getKd(String key) {
		PIDData piddata = data.get(key);
		return piddata.dispKd;
	}

	public double getKi(String key) {
		PIDData piddata = data.get(key);
		return piddata.dispKi;
	}

	public double getKp(String key) {
		PIDData piddata = data.get(key);
		return piddata.dispKp;
	}

	public int getMode(String key) {
		PIDData piddata = data.get(key);
		return piddata.inAuto ? MODE_AUTOMATIC : MODE_MANUAL;
	}

	public double getOutput(String key) {
		PIDData piddata = data.get(key);
		return piddata.output;
	}

	public double getSetpoint(String key) {
		PIDData piddata = data.get(key);
		return piddata.setpoint;
	}

	/*
	 * Initialize()**************************************************************
	 * ** does all the things that need to happen to ensure a bumpless transfer
	 * from manual to automatic mode.
	 * ********************************************
	 * ********************************
	 */
	public void init(String key) {
		PIDData piddata = data.get(key);
		piddata.ITerm = piddata.output;
		piddata.lastInput = piddata.input;
		if (piddata.ITerm > piddata.outMax)
			piddata.ITerm = piddata.outMax;
		else if (piddata.ITerm < piddata.outMin)
			piddata.ITerm = piddata.outMin;

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
	 * ** The PID2 will either be connected to a DIRECT acting process (+Output
	 * leads to +Input) or a REVERSE acting process(+Output leads to -Input.) we
	 * need to know which one, because otherwise we may increase the output when
	 * we should be decreasing. This is called from the constructor.
	 * *************
	 * ***************************************************************
	 */
	public void setControllerDirection(String key, Integer direction) {
		PIDData piddata = data.get(key);
		if (piddata.inAuto && direction != piddata.controllerDirection) {
			piddata.kp = (0 - piddata.kp);
			piddata.ki = (0 - piddata.ki);
			piddata.kd = (0 - piddata.kd);
		}
		piddata.controllerDirection = direction;
		broadcastState();
	}

	public void setInput(String key, double input) {
		PIDData piddata = data.get(key);
		piddata.input = input;
	}

	/*
	 * SetMode(...)**************************************************************
	 * ** Allows the controller Mode to be set to manual (0) or Automatic
	 * (non-zero) when the transition from manual to auto occurs, the controller
	 * is automatically initialized
	 * **********************************************
	 * ******************************
	 */
	public void setMode(String key, int Mode) {
		PIDData piddata = data.get(key);
		boolean newAuto = (Mode == MODE_AUTOMATIC);
		if (newAuto == !piddata.inAuto) { /* we just went from manual to auto */
			init(key);
		}
		piddata.inAuto = newAuto;
		broadcastState();
	}

	/*
	 * setOutputRange(...)****************************************************
	 * This function will be used far more often than SetInputLimits. while the
	 * input to the controller will generally be in the 0-1023 range (which is
	 * the default already,) the output will be a little different. maybe
	 * they'll be doing a time window and will need 0-8000 or something. or
	 * maybe they'll want to clamp it from 0-125. who knows. at any rate, that
	 * can all be done here.
	 * ************************************************************************
	 */
	public void setOutputRange(String key, double Min, double Max) {
		PIDData piddata = data.get(key);
		if (Min >= Max) {
			error("min >= max");
			return;
		}
		piddata.outMin = Min;
		piddata.outMax = Max;

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

	/*
	 * setPID(...)*************************************************************
	 * This function allows the controller's dynamic performance to be adjusted.
	 * it's called automatically from the constructor, but tunings can also be
	 * adjusted on the fly during normal operation
	 * *******************************
	 * *********************************************
	 */
	public void setPID(String key, Double Kp, Double Ki, Double Kd) {
		if (Kp < 0 || Ki < 0 || Kd < 0) {
			error("kp < 0 || ki < 0 || kd < 0");
			return;
		}

		PIDData piddata = new PIDData();

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

		broadcastState();
	}

	/*
	 * setSampleTime(...)
	 * ********************************************************* sets the
	 * period, in Milliseconds, at which the calculation is performed
	 * ************
	 * ****************************************************************
	 */
	public void setSampleTime(String key, int NewSampleTime) {
		PIDData piddata = data.get(key);
		if (NewSampleTime > 0) {
			double ratio = (double) NewSampleTime / (double) piddata.sampleTime;
			piddata.ki *= ratio;
			piddata.kd /= ratio;
			piddata.sampleTime = NewSampleTime;
		}

		broadcastState();
	}

	public void setSetpoint(String key, double setPoint) {
		PIDData piddata = data.get(key);
		piddata.setpoint = setPoint;
	}

}
