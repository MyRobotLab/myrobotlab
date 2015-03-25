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

import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.Trigger;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         http://en.wikipedia.org/wiki/Dead_reckoning#
 *         Differential_steer_drive_dead_reckoning
 * 
 *         Timer & TimerTask http://www.java2s.com/Code/Java/Development-Class/
 *         TimerScheduleataskthatexecutesonceeverysecond.htm
 * 
 *         finish Tweedle Dee & Dummer
 * 
 *         calibrate - make it go straight find the delta for going straight
 *         (are there two? ie servo differences) find the delta going CW vs CCW
 *         find the delta of error of having timing done on the uC versus the
 *         puter - MOVE_FOR int find the delta in time/distance at speed versus
 *         rest find the delta in time/distance at lower battery find the speed
 *         for some constant level (at rest) find the speed of a turn (constant
 *         level) (at rest) find the drift for shutting off speed what is
 *         WHEEL_BASE ?
 * 
 *         1. start - move forward - keep track of time
 * 
 *         Do some maneuvering tests
 * 
 *         Find out what the "real" min max and error is of the IR sensor
 * 
 *         Go forward (straight line!! error ouch!!) until something is reached
 *         (inside max range of sensor stop) - record/graph the time - draw a
 *         line (calibrate this) Turn heading until parallel with the wall (you
 *         must do this slowly)
 * 
 *         SLAM -------- calibrate as best as possible
 * 
 *         guess where you are with little data (time)
 * 
 *         when you get data corroberate it with what you have (saved info)
 * 
 *         refactor with RobotPlatform !!!
 */

public class TweedleBot extends Service {

	/**
	 * a timed event task - used to block in dead reckoning
	 * 
	 */
	class TimedTask extends TimerTask {
		@Override
		public void run() {
			stop();
			synchronized (event) {
				event.notifyAll();
			}
		}
	}

	private static final long serialVersionUID = 1L;

	transient public Timer timer = new Timer();

	transient private Object event = new Object();
	// cartesian
	public float positionX = 0;

	public float positionY = 0;
	// polar
	public float theta = 0;

	public float distance = 0;
	public int targetX = 0;

	public int targetY = 0;

	public int headingCurrent = 0;
	int leftPin = 4;
	int rightPin = 3;

	int neckPin = 9;
	int rightStopPos = 90;

	int leftStopPos = 90;
	transient public Servo left;
	transient public Servo right;
	transient public Servo neck;

	transient public SensorMonitor sensors;
	/**
	 * servos do not go both directions at the same speed - this will be a
	 * constant to attempt to adjust for the mechanical/electrical differences
	 */
	int leftError;

	int rightError;
	/**
	 * start & stops are not instantaneous - this adjustment is included as a
	 * constant in maneuvers which include stops & starts
	 */
	int startError;

	int stopError;

	transient public Arduino arduino;

	public final static Logger log = LoggerFactory.getLogger(TweedleBot.class.getCanonicalName());

	// behavior - TODO - pre-pend
	public final static String BEHAVIOR_IDLE = "i am idle";

	public final static String BEHAVIOR_EXPLORE = "i am exploring";

	// control functions begin -------------------

	// sensor (in) states
	public final static String ALERT_WALL = "ALERT_WALL";

	String state = BEHAVIOR_IDLE;

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		TweedleBot dee = new TweedleBot("dee");
		dee.start();
	}

	public TweedleBot(String n) {
		this(n, null);
	}

	public TweedleBot(String n, String serviceDomain) {
		super(n);

		neck = new Servo(getName() + "Neck");
		right = new Servo(getName() + "Right");
		left = new Servo(getName() + "Left");
		arduino = new Arduino(getName() + "BBB");
		sensors = new SensorMonitor(getName() + "Sensors");

		neck.attach(arduino.getName(), neckPin);
		right.attach(arduino.getName(), rightPin);
		left.attach(arduino.getName(), leftPin);
	}

	public void explore() {
		try {

			for (int i = 0; i < 100; ++i) {
				move(15);
				Thread.sleep(2000);
				stop();
			}

			log.info("here");

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "<html>used to encapsulate many of the functions and formulas regarding 2 motor platforms.<br>"
				+ "encoders and other feedback mechanisms can be added to provide heading, location and other information</html>";
	}

	// TODO - is relative and incremental - change to absolute
	public void move(int power) {
		// to attach or not to attach that is the question
		// right.attach();
		// left.attach();
		arduino.servoAttach("right", 9);
		arduino.servoAttach("left", 8);

		try {
			// must ramp
			for (int i = 0; i < power; ++i) {
				right.moveTo(rightStopPos + i);
				left.moveTo(leftStopPos - i); // + leftError
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logException(e);
		}
	}

	public void moveTo(float distance) {

	}

	// TODO spinLeft(int power, int time)
	// TODO - possibly have uC be the timer
	// TODO - bury any move or stop with attach & detach in the uC
	// TODO - make continuous rotational Servo handle all this
	public void moveUntil(int power, int time) {
		// start timer;
		timer.schedule(new TimedTask(), time);
		// right.attach(); // FIXME - attach right & left in single uC call -
		// Arduino platform API
		// left.attach();
		right.moveTo(power);
		left.moveTo(-power);
		waitForEvent(); // blocks
	}

	// turning related end --------------------------

	public TweedleBot publishState(TweedleBot t) {
		return t;
	}

	public void sensorAlert(Trigger alert) {
		stop();
		state = BEHAVIOR_IDLE;
	}

	// command (out) states

	// command to change heading and/or position
	public void setHeading(int value) // maintainHeading ?? if PID is operating
	{
		// headingTarget = value;
		setHeading(headingCurrent);// HACK? - a way to get all of the
									// recalculations publish
	}

	public void setTargetPosition(int x, int y) {
		targetX = x;
		targetY = y;
	}

	public void spinLeft(int power) {
		right.moveTo(-power);
		left.moveTo(power);
	}

	public void spinRight(int power) {
		right.moveTo(power);
		left.moveTo(-power);
	}

	// fsm ------------------------------------
	public void start() {

		/*
		 * // Graphics graphics = new Graphics("graphics"); //
		 * graphics.startService();
		 */

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */

		explore();

		// set a route of data from arduino to the sensor monitor
		arduino.addListener("publishPin", sensors.getName(), "sensorInput", Pin.class);

		// set an alert from sensor monitor to MyRobot
		sensors.addListener("publishPinAlert", this.getName(), "sensorAlert", Trigger.class);
		sensors.addTrigger(arduino.getName(), ALERT_WALL, 600, 700, 3, 5, 0);

		// move & set timer
		move(20);

	}

	// left > 101 backwards 101
	// left < 83 forwards
	// stop mid 92

	// right < 83 backwards
	// right > 99 forwards
	// stop mid 91

	@Override
	public void startService() {
		super.startService();
		sensors.startService();
		neck.startService();
		right.startService();
		left.startService();
		arduino.startService();
	}

	// FIXME - is absolute - but needs to be incremental
	public void stop() {
		right.moveTo(rightStopPos);
		left.moveTo(leftStopPos);
		arduino.servoDetach("right");
		arduino.servoDetach("left");
		// right.detach();
		// left.detach();
	}

	public void waitForEvent() {
		synchronized (event) {
			try {
				event.wait();
			} catch (InterruptedException e) {
			}
		}

	}
}
