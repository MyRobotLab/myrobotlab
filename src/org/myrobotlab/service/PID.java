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
 * Arduino PID Library - Version 1.0.1
 * by Brett Beauregard <br3ttb@gmail.com> brettbeauregard.com
 *
 * This Library is licensed under a GPLv3 License
 * 
 * http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/
 * 
 * Thanks Brett !
 * */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class PID extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PID.class.getCanonicalName());

	// mode
	static final public int MODE_AUTOMATIC = 1;
	static final public int MODE_MANUAL = 0;

	// direction
	static final public int DIRECTION_DIRECT = 0;
	static final public int DIRECTION_REVERSE = 1;

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
	private double output; // This creates a hard link between the variables and
	// the
	private double setpoint; // PID, freeing the user from having to constantly

	// tell us
	// what these values are. with pointers we'll just know.
	private long lastTime;
								private double ITerm, lastInput;

	private long sampleTime = 100; // default Controller Sample Time is 0.1
	// seconds
	private double outMin, outMax;
	private boolean inAuto;

	private long sampleCount = 0;

	public static void main(String[] args) throws Exception {
		// Logger root =
		// (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Logging logging = LoggingFactory.getInstance();
		logging.configure();
		logging.setLevel(Level.INFO);
		// LoggingFactory.getInstance().setLevel(Level.INFO);

		int test = 35;
		log.info("{}", test);

		log.debug("hello");
		log.trace("trace");
		log.error("error");
		log.info("info");

		PID pid = new PID("pid");
		pid.startService();
		pid.setPID(2.0, 5.0, 1.0);
		log.info("{}", pid.getKp());
		pid.setControllerDirection(DIRECTION_DIRECT);
		pid.setMode(MODE_AUTOMATIC);
		pid.setOutputRange(0, 255);
		pid.setSetpoint(100);
		pid.setSampleTime(40);

		// GUIService gui = new GUIService("gui");
		// gui.startService();

		for (int i = 0; i < 200; ++i) {
			pid.setInput(i);
			Service.sleep(30);
			if (pid.compute()) {
				log.info(String.format("%d %f", i, pid.getOutput()));
			} else {
				log.warn("not ready");
			}
		}

	}

	public PID(String n) {
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
	public boolean compute() {
		if (!inAuto)
			return false;
		long now = System.currentTimeMillis();
		long timeChange = (now - lastTime);
		if (timeChange >= sampleTime) {
			++sampleCount;
			/* compute all the working error variables */
			double error = setpoint - input;
			ITerm += (ki * error);
			if (ITerm > outMax)
				ITerm = outMax;
			else if (ITerm < outMin)
				ITerm = outMin;
			double dInput = (input - lastInput);

			/* compute PID Output */
			double output = kp * error + ITerm - kd * dInput;

			if (output > outMax)
				output = outMax;
			else if (output < outMin)
				output = outMin;
			this.output = output;

			/* Remember some variables for next time */
			lastInput = input;
			lastTime = now;
			return true;
		} else
			return false;
	}

	public void direct() {
		setControllerDirection(DIRECTION_DIRECT);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control" };
	}

	public int getControllerDirection() {
		return controllerDirection;
	}

	@Override
	public String getDescription() {
		return "<html>a PID control service from<br>" + "http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/</html>";
	}

	public double getKd() {
		return dispKd;
	}

	public double getKi() {
		return dispKi;
	}

	public double getKp() {
		return dispKp;
	}

	public int getMode() {
		return inAuto ? MODE_AUTOMATIC : MODE_MANUAL;
	}

	public double getOutput() {
		return output;
	}

	public double getSetpoint() {
		return setpoint;
	}

	/*
	 * Initialize()**************************************************************
	 * ** does all the things that need to happen to ensure a bumpless transfer
	 * from manual to automatic mode.
	 * ********************************************
	 * ********************************
	 */
	public void init() {
		ITerm = output;
		lastInput = input;
		if (ITerm > outMax)
			ITerm = outMax;
		else if (ITerm < outMin)
			ITerm = outMin;

		lastTime = System.currentTimeMillis() - sampleTime; // FIXME - is this
															// correct ??? (was
															// in constructor)
	}

	public void invert() {
		setControllerDirection(DIRECTION_REVERSE);
	}

	/*
	 * SetControllerDirection(...)***********************************************
	 * ** The PID will either be connected to a DIRECT acting process (+Output
	 * leads to +Input) or a REVERSE acting process(+Output leads to -Input.) we
	 * need to know which one, because otherwise we may increase the output when
	 * we should be decreasing. This is called from the constructor.
	 * *************
	 * ***************************************************************
	 */
	public void setControllerDirection(Integer direction) {
		if (inAuto && direction != controllerDirection) {
			kp = (0 - kp);
			ki = (0 - ki);
			kd = (0 - kd);
		}
		controllerDirection = direction;
		broadcastState();
	}

	public void setInput(double input) {
		this.input = input;
	}

	/*
	 * SetMode(...)**************************************************************
	 * ** Allows the controller Mode to be set to manual (0) or Automatic
	 * (non-zero) when the transition from manual to auto occurs, the controller
	 * is automatically initialized
	 * **********************************************
	 * ******************************
	 */
	public void setMode(int Mode) {
		boolean newAuto = (Mode == MODE_AUTOMATIC);
		if (newAuto == !inAuto) { /* we just went from manual to auto */
			init();
		}
		inAuto = newAuto;
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
	public void setOutputRange(double Min, double Max) {
		if (Min >= Max)
			return;
		outMin = Min;
		outMax = Max;

		if (inAuto) {
			if (output > outMax)
				output = outMax;
			else if (output < outMin)
				output = outMin;

			if (ITerm > outMax)
				ITerm = outMax;
			else if (ITerm < outMin)
				ITerm = outMin;
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
	public void setPID(Double Kp, Double Ki, Double Kd) {
		if (Kp < 0 || Ki < 0 || Kd < 0)
			return;

		dispKp = Kp;
		dispKi = Ki;
		dispKd = Kd;

		double SampleTimeInSec = ((double) sampleTime) / 1000;
		kp = Kp;
		ki = Ki * SampleTimeInSec;
		kd = Kd / SampleTimeInSec;

		if (controllerDirection == DIRECTION_REVERSE) {
			kp = (0 - kp);
			ki = (0 - ki);
			kd = (0 - kd);
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
	public void setSampleTime(int NewSampleTime) {
		if (NewSampleTime > 0) {
			double ratio = (double) NewSampleTime / (double) sampleTime;
			ki *= ratio;
			kd /= ratio;
			sampleTime = NewSampleTime;
		}

		broadcastState();
	}

	public void setSetpoint(double setPoint) {
		setpoint = setPoint;
	}

}
