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
 * Enjoy.
 * 
 * */

package org.myrobotlab.service;

import java.awt.Dimension;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.bytedeco.javacpp.opencv_core.CvPoint;

public class FaceTracking extends Service {

	public class PID {

		/* working variables */
		public long lastTime;
		public float Input, Output, Setpoint;
		public float errSum, lastErr;
		public float kp, ki, kd;

		void Compute() {
			/* How long since we last calculated */
			long now = System.currentTimeMillis();
			float timeChange = now - lastTime;

			/* Compute all the working error variables */
			float error = Setpoint - Input;
			errSum += (error * timeChange);
			float dErr = (error - lastErr) / timeChange;

			/* Compute PID Output */
			Output = kp * error + ki * errSum + kd * dErr;

			/* Remember some variables for next time */
			lastErr = error;
			lastTime = now;
		}

		void SetTunings(float Kp, float Ki, float Kd) {
			kp = Kp;
			ki = Ki;
			kd = Kd;
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(FaceTracking.class.getCanonicalName());
	/*
	 * TODO - dead zone - scan / search
	 */
	Servo tilt = new Servo("tilt");
	Servo pan = new Servo("pan");

	OpenCV camera = (OpenCV) Runtime.create("camera", "OpenCV");

	Arduino arduino = new Arduino("arduino");

	Log logger = new Log("logger");

	Speech speech = new Speech("speech");
	transient PID xpid = new PID();

	transient PID ypid = new PID();

	String state = null;

	// TODO - put cfg
	int width = 640;

	int height = 480;

	int centerX = width / 2;
	int centerY = height / 2;
	int errorX = 0;
	int errorY = 0;
	boolean canSpeak = true;
	public static void main(String[] args) throws ClassNotFoundException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		try {
			FaceTracking ft = new FaceTracking("face tracker");
			ft.startService();

			ft.camera.addFilter("Gray", "Gray");
			ft.camera.addFilter("PyramidDown2", "PyramidDown");
			ft.camera.addFilter("MatchTemplate", "MatchTemplate");
			// ft.camera.addFilter("PyramidDown2", "PyramidDown");
			// ft.camera.useInput = "camera";
			ft.camera.capture();

			// ft.arduino.se - TODO setPort("/dev/ttyUSB0");

			// ft.tilt.attach(ft.arduino.getName(), 12);
			// ft.pan.attach(ft.arduino.getName(), 13);

			GUIService gui = new GUIService("gui");
			gui.startService();

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public FaceTracking(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "tracking" };
	}

	@Override
	public String getDescription() {
		return "used for tracking";
	}

	public void input(CvPoint point) {

		xpid.Input = point.x();
		xpid.Compute();

		ypid.Input = point.y();
		ypid.Compute();

		int correctX = (int) xpid.Output;
		int correctY = (int) ypid.Output;

		// log.error(point.x() + "," + point.y() + " correct x " + correctX +
		// " correct y " + correctY);

		if (correctX == 0 && correctY == 0) {
			if (state == null) {
				// I found my num num
				log.error("I found my num num");
				speak("I found my num num");
				state = "foundSomethingNew";
			} else if (!"centered".equals(state)) {
				log.error("I'm hooked up, num num num");
				speak("I got my num num");
			}
			state = "centered";
		} else {
			if (!"tracking".equals(state)) {
				log.error("I wan't dat");
				speak("I wan't dat");

			}
			invoke("pan", correctX);
			invoke("tilt", correctY);
			state = "tracking";
		}
	}

	public void isTracking(Boolean b) {
		if (b == true) {
			log.error("num num - I' gonna get it");
			speak("I'm gonna get my num num");
		} else {
			// if (!"lostTracking".equals(state))
			// wah wah - search routine
			log.error("wah wah - where is it?");
			speak("boo who who - where is my num num?");
			state = "lostTracking";
		}
	}

	public Integer pan(Integer position) {
		return position;
	}

	public void playingFile(Boolean b) {
		if (b) {
			canSpeak = false;
		} else {
			canSpeak = true;
		}
	}

	// TODO - reflectively do it in Service? !?
	// No - the overhead of a Service warrants a data only proxy - so to
	// a single container class "ClockData data = new ClockData()" could allow
	// easy maintenance and extensibility - possibly even reflective sync if
	// names are maintained
	public FaceTracking setState(FaceTracking o) {
		return o;
	}

	public void sizeChange(Dimension d) {
		width = d.width;
		height = d.height;
		xpid.Setpoint = width / 2;
		ypid.Setpoint = height / 2;

	}

	/*
	 * Un-needed public FaceTracking publishState() { return this; }
	 */

	public void speak(String s) {
		send("speechAudioFile", "playWAV", s);
		if (canSpeak)
			speech.speak(s); // TODO bury in framework so Speech Service
								// variable maintains canSpeak
	}

	@Override
	public void startService() {
		speech.startService();
		tilt.startService();
		pan.startService();
		camera.startService();
		logger.startService();
		arduino.startService();
		// pan.attach(arduino.getName(), 13);

		camera.addListener("publish", getName(), "input");
		camera.addListener("isTracking", getName(), "isTracking", Boolean.class);
		camera.addListener("sizeChange", getName(), "sizeChange", Dimension.class);
		// addListener("pan", "logger", "log");
		// addListener("tilt", "logger", "log");
		addListener("pan", "pan", "move");
		addListener("tilt", "tilt", "move");

		xpid.SetTunings(0.05f, 0f, 0.6f);
		xpid.Setpoint = 320;

		ypid.SetTunings(0.05f, 0f, 0.6f);
		ypid.Setpoint = 120;

	}

	public void startTracking() {
	}

	public void stopTracking() {
	}

	public Integer tilt(Integer position) {
		return position;
	}

}
