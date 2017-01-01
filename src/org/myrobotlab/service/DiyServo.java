/**
 *                    
 * @author GroG & Mats (at) myrobotlab.org
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.motor.MotorConfigDualPwm;
import org.myrobotlab.motor.MotorConfigSimpleH;
import org.myrobotlab.sensor.Encoder;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.NameProvider;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author Grog & Mats
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0 - 180, and output can relay those values directly
 *         to the servo's firmware (Arduino ServoLib, I2C controller, etc)
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
 *         This service is to be used if you have a motor without feedback and
 *         you want to use it as a Servo. So you connect the motor as a Motor
 *         and use an Aduino, Ads1115 or some other input source that can give
 *         an analog input from a potentiometer or other device that can give
 *         analog feedback.
 */

public class DiyServo extends Service implements ServoControl, MotorControl, PinListener {

	/**
	 * Sweeper
	 * 
	 */
	public class Sweeper extends Thread {

		/*
		 * int min; int max; int delay; // depending on type - this is 2
		 * different things COMPUTER // its ms delay - CONTROLLER its modulus
		 * loop count int step; boolean sweepOneWay;
		 */

		public Sweeper(String name) {
			super(String.format("%s.sweeper", name));
		}

		@Override
		public void run() {

			if (targetPos == null) {
				targetPos = sweepMin;
			}

			try {
				while (isSweeping) {
					// increment position that we should go to.
					if (targetPos < sweepMax && sweepStep >= 0) {
						targetPos += sweepStep;
					} else if (targetPos > sweepMin && sweepStep < 0) {
						targetPos += sweepStep;
					}

					// switch directions or exit if we are sweeping 1 way
					if ((targetPos <= sweepMin && sweepStep < 0) || (targetPos >= sweepMax && sweepStep > 0)) {
						if (sweepOneWay) {
							isSweeping = false;
							break;
						}
						sweepStep = sweepStep * -1;
					}
					moveTo(targetPos.intValue());
					Thread.sleep(sweepDelay);
				}

			} catch (Exception e) {
				isSweeping = false;
				if (e instanceof InterruptedException) {
					info("shutting down sweeper");
				} else {
					logException(e);
				}
			}
		}

	}

	/**
	 * MotorUpdater The control loop to update the MotorController with new
	 * values based on the PID calculations
	 * 
	 */
	public class MotorUpdater extends Thread {

		double lastOutput = 0;
		DiyServo mc;

		public MotorUpdater(DiyServo mc) {
			super(String.format("%s.MotorUpdater", mc.getName()));
			this.mc = mc;
		}

		@Override
		public void run() {

			try {
				while (true) {
					if (controller != null) {
						// Calculate the new value for the motor
						if (pid.compute(pidKey)) {
							double setPoint = pid.getSetpoint(pidKey);
							double output = pid.getOutput(pidKey);
							mc.setPowerLevel(output);
							log.debug(String.format("setPoint(%s), processVariable(%s), output(%s)", setPoint, processVariable, output));
							if (output != lastOutput) {
								// controller.move(output);
								controller.motorMove((MotorControl) mc);
								lastOutput = output;
							}
						}
						Thread.sleep(1000 / sampleTime);
					}
				}

			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					info("Shutting down MotorUpdater");
				} else {
					logException(e);
				}
			}
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(DiyServo.class);

	// MotorController
	transient MotorController controller;
	public String controllerName = null;
	MotorConfig config;

	/**
	 * // Reference to the Analog input service
	 */
	public List<String> pinArrayControls;
	/**
	 * // Handle to the selected analog input service and it's name
	 */
	transient PinArrayControl pinArrayControl;
	public String pinControlName;

	/**
	 * List of available pins on the analog input service
	 */
	public List<Integer> pinList = new ArrayList<Integer>();
	public Integer pin;

	/**
	 * mapper to be able to remap input values
	 */
	Mapper mapper = new Mapper(0, 180, 0, 180);

	Integer rest = 90;

	long lastActivityTime = 0;

	/**
	 * the requested INPUT position of the servo
	 */
	Double targetPos;

	/**
	 * the calculated output for the servo
	 */
	Integer targetOutput;

	/**
	 * list of names of possible controllers
	 */
	public List<String> controllers;

	// FIXME - currently is only computer control - needs to be either
	// microcontroller or computer
	boolean isSweeping = false;
	double sweepMin = 0;
	double sweepMax = 180;
	int sweepDelay = 1;

	int sweepStep = 1;
	boolean sweepOneWay = false;

	transient Thread sweeper = null;

	/**
	 * feedback of both incremental position and stops. would allow blocking
	 * moveTo if desired
	 */
	boolean isEventsEnabled = false;

	private int maxVelocity = 425;

	private boolean isAttached = false;
	private boolean isControllerSet = false;
	private boolean isPinArrayControlSet = false;

	// Initial parameters for PID.

	static final public int MODE_AUTOMATIC = 1;
	static final public int MODE_MANUAL = 0;
	public int mode = MODE_MANUAL;

	public Pid pid;
	private String pidKey;
	private double kp = 0.020;
	private double ki = 0.001; // 0.020;
	private double kd = 0.0; // 0.020;
	public double setPoint = 90; // Intial
									// setpoint
									// corresponding
									// to
									// a
									// centered
									// servo
									// The
									// pinListener
									// value
									// depends
									// on
									// the
									// hardwawe
									// behind
									// it,
									// so
									// the
									// value
									// from
									// the
	int resolution = 1024; // AD
							// converter
							// needs
							// to
							// be
							// remapped
							// to
							// 0
							// -
							// 180.
							// D1024
							// is
							// the
							// default
							// for
							// the
							// Arduino
	int sampleTime = 20; // Sample
							// time
							// 20
							// ms
							// =
							// 50
							// Hz
	public double processVariable = 0; // Initial
										// process
	// variable
	transient MotorUpdater motorUpdater;

	double powerLevel = 0;
	double maxPower = 1.0;
	double minPower = -1.0;

	Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);

	/**
	 * Constructor
	 * 
	 * @param n
	 *            name of the service
	 */
	public DiyServo(String n) {
		super(n);
		refreshControllers();
		refreshPinArrayControls();
		initPid();
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
		lastActivityTime = System.currentTimeMillis();
	}

	/**
	 * Update the list of MotorControllers and PinArrayControls
	 * 
	 * @param s
	 */
	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		refreshPinArrayControls();
		broadcastState();

	}

	/**
	 * Initiate the PID controller
	 */
	void initPid() {
		pid = (Pid) createPeer("Pid");
		pidKey = this.getName();
		pid.setPID(pidKey, kp, ki, kd); // Create a PID with the name of this
										// service instance
		pid.setMode(pidKey, MODE_AUTOMATIC); // Initial mode is manual
		pid.setOutputRange(pidKey, -1.0, 1.0); // Set the Output range to match
												// the
												// Motor input
		pid.setSampleTime(pidKey, sampleTime); // Sets the sample time
		pid.setSetpoint(pidKey, setPoint);
		pid.startService();
	}

	/**
	 * @param service
	 */
	public void addServoEventListener(NameProvider service) {
		addListener("publishServoEvent", service.getName(), "onServoEvent");
	}

	/**
	 * Re-attach to servo's current pin. The pin must have be set previously.
	 * Equivalent to Arduino's Servo.attach(currentPin) In this service it stops
	 * the motor and PID is set to manual mode
	 */
	@Override
	public void attach() {
		attach(pin);
		broadcastState();
	}

	/**
	 * Equivalent to Arduino's Servo.attach(pin). It energizes the servo sending
	 * pulses to maintain its current position.
	 */
	@Override
	public void attach(int pin) {
		// TODO Activate the motor and PID
		lastActivityTime = System.currentTimeMillis();
		isAttached = true;
		broadcastState();
	}

	/**
	 * Equivalent to Arduino's Servo.detach() it de-energizes the servo
	 */
	@Override
	// TODO DeActivate the motor and PID
	public void detach() {
		if (controller != null) {
			controller.motorStop((MotorControl) this);
		}
		isAttached = false;
		broadcastState();
	}

	/////// config start ////////////////////////
	public void setPwmPins(int leftPin, int rightPin) {
		config = new MotorConfigDualPwm(leftPin, rightPin);
		broadcastState();
	}

	public void setPwrDirPins(int pwrPin, int dirPin) {
		config = new MotorConfigSimpleH(pwrPin, dirPin);
		broadcastState();
	}

	public MotorConfig getConfig() {
		return config;
	}

	/**
	 * Method to check if events are enabled or not
	 * 
	 * @param b
	 * @return
	 */

	public boolean eventsEnabled(boolean b) {
		isEventsEnabled = b;
		broadcastState();
		return b;
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	public double getMax() {
		return mapper.getMaxX();
	}

	
	public double getPos() {
		return targetPos;
	}

	public double getRest() {
		return rest;
	}

	// FIXME - change to isPinAttached()
	public boolean isAttached() {
		return isAttached;
	}

	public boolean isControllerSet() {
		return isControllerSet;
	}

	public boolean isPinArrayControlSet() {
		return isPinArrayControlSet;
	}

	public boolean isInverted() {
		return mapper.isInverted();
	}

	public void map(double minX, double maxX, double minY, double maxY) {
		mapper = new Mapper(minX, maxX, minY, maxY);
		broadcastState();
	}

	/**
	 * The most important method, that tells the servo what position it should
	 * move to
	 */
	public void moveTo(double pos) {

		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		if (motorUpdater == null) {
			motorUpdater = new MotorUpdater(this);
			motorUpdater.run();
		}

		targetPos = pos;
		targetOutput = mapper.calcOutputInt(targetPos);

		pid.setSetpoint(pidKey, targetOutput);
		lastActivityTime = System.currentTimeMillis();

		if (isEventsEnabled) {
			// update others of our position change
			invoke("publishServoEvent", targetOutput);
		}
	}

	/**
	 * basic move command of the servo - usually is 0 - 180 valid range but can
	 * be adjusted and / or re-mapped with min / max and map commands
	 * 
	 * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
	 * response
	 */

	// uber good
	public Integer publishServoEvent(Integer position) {
		return position;
	}

	/**
	 * FIXME - Hmmm good canidate for Microcontroller Peripheral
	 * 
	 * @return
	 */
	public List<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(MotorController.class);
		return controllers;
	}

	public List<String> refreshPinArrayControls() {
		pinArrayControls = Runtime.getServiceNamesFromInterface(PinArrayControl.class);
		return pinArrayControls;
	}

	@Override
	public void releaseService() {
		detach();
		if (controller != null){
		  detach(controller);
		}
		super.releaseService();
	}

	public void rest() {
		moveTo(rest);
	}

	/*
	@Override
	public void setController(DeviceController controller) {
		if (controller == null) {
			info("setting controller to null");
			this.controllerName = null;
			this.controller = null;
			return;
		}

		log.info(String.format("%s setController %s", getName(), controller.getName()));
		this.controller = (MotorController) controller;
		this.controllerName = controller.getName();
		broadcastState();
	}
	*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#setPin(int)
	 */
	/*
	 * @Override public boolean setPin(int pin) { log.info(String.format(
	 * "setting %s pin to %d", getName(), pin)); if (isAttached()) { warn(
	 * "%s can not set pin %d when servo is attached", getName(), pin); return
	 * false; } this.pin = pin; broadcastState(); return true; }
	 */

	public void setInverted(boolean invert) {
		mapper.setInverted(invert);
	}

	@Override
	public void setMinMax(double min, double max) {
		mapper.setMin(min);
		mapper.setMax(max);
		broadcastState();
	}

	public void setRest(int rest) {
		this.rest = rest;
	}

	public void setSweepDelay(int delay) {
		sweepDelay = delay;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#stopServo()
	 */
	@Override
	public void stop() {
		isSweeping = false;
		sweeper = null;
		// TODO Replace with internal logic for motor and PID
		// getController().servoSweepStop(this);
		broadcastState();
	}

	public void sweep() {
		double min = mapper.getMinX();
		double max = mapper.getMaxX();
		// sweep(min, max, 1, 1);
	}

	public void sweep(double min, double max) {
		// sweep(min, max, 1, 1);
	}

	// FIXME - is it really speed control - you don't currently thread for
	// fractional speed values
	public void sweep(int min, int max, int delay, int step) {
		sweep(min, max, delay, step, false);
	}

	public void sweep(int min, int max, int delay, int step, boolean oneWay) {

		this.sweepMin = min;
		this.sweepMax = max;
		this.sweepDelay = delay;
		this.sweepStep = step;
		this.sweepOneWay = oneWay;

		if (isSweeping) {
			stop();
		}

		sweeper = new Sweeper(getName());
		sweeper.start();

		isSweeping = true;
		broadcastState();
	}

	/**
	 * Writes a value in microseconds (uS) to the servo, controlling the shaft
	 * accordingly. On a standard servo, this will set the angle of the shaft.
	 * On standard servos a parameter value of 1000 is fully counter-clockwise,
	 * 2000 is fully clockwise, and 1500 is in the middle.
	 * 
	 * Note that some manufactures do not follow this standard very closely so
	 * that servos often respond to values between 700 and 2300. Feel free to
	 * increase these endpoints until the servo no longer continues to increase
	 * its range. Note however that attempting to drive a servo past its
	 * endpoints (often indicated by a growling sound) is a high-current state,
	 * and should be avoided.
	 * 
	 * Continuous-rotation servos will respond to the writeMicrosecond function
	 * in an analogous manner to the write function.
	 * 
	 * @param pos
	 */
	public void writeMicroseconds(Integer uS) {
		log.info("writeMicroseconds({})", uS);
		// TODO. This need to be remapped to Motor and PID internal to this
		// Service
		// getController().servoWriteMicroseconds(this, uS);
		lastActivityTime = System.currentTimeMillis();
		broadcastState();
	}

	/*
	 * @Override public void setPin(int pin) { this.pin = pin; }
	 */

	@Override
	public Integer getPin() {
		return this.pin;
	}

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {
			// Runtime.start("webgui", "WebGui");
			Runtime.start("gui", "GUIService");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect("COM3");

			Ads1115 ads = (Ads1115) Runtime.start("Ads1115", "Ads1115");
			ads.setController(arduino, "1", "0x48");

			DiyServo dyiServo = (DiyServo) Runtime.start("DiyServo", "DiyServo");
			dyiServo.attach((ServoController)arduino);
			dyiServo.attach((PinArrayControl) ads, 0); // PIN 14 = A0

			// Servo Servo = (Servo) Runtime.start("Servo", "Servo");

			dyiServo.moveTo(90);
			dyiServo.setRest(30);
			dyiServo.moveTo(10);
			dyiServo.moveTo(90);
			dyiServo.moveTo(180);
			dyiServo.rest();

			dyiServo.setMinMax(30, 160);

			dyiServo.moveTo(40);
			dyiServo.moveTo(140);

			dyiServo.moveTo(180);

			dyiServo.setSpeed(0.5);
			dyiServo.moveTo(31);
			dyiServo.setSpeed(0.2);
			dyiServo.moveTo(90);
			dyiServo.moveTo(180);

			// servo.test();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	@Override
	public double getMin() {
		return mapper.getMinX();
	}

	@Override
	public double getTargetOutput() {
		return targetOutput;
	}

	public void attach(String controllerName) throws Exception {
		attach((MotorController) Runtime.getService(controllerName));
	}

	public void attach(MotorController controller) throws Exception {
		this.controller = controller;
		if (controller != null) {
			controllerName = controller.getName();
			isControllerSet = true;
		}
		broadcastState();
	}

	@Override
	public void detach(String controllerName) {
		ServiceInterface si = Runtime.getService(controllerName);
		if (si instanceof MotorController) {
			detach((MotorController) Runtime.getService(controllerName));
		}
		if (si instanceof PinArrayControl) {
			detach((PinArrayControl) Runtime.getService(controllerName));
		}
	}

	public void detach(MotorController controller) {
		if (this.controller == controller) {
			this.controller = null;
			isAttached = false;
			isControllerSet = false;
			broadcastState();
		}
	}

	public void detach(PinArrayControl pinArrayControl) {
		if (this.pinArrayControl == pinArrayControl) {
			this.pinArrayControl = null;
			isPinArrayControlSet = false;
			broadcastState();
		}
	}

	public void setMaxVelocity(int velocity) {
		this.maxVelocity = velocity;
	}

	@Override
	public double getMaxVelocity() {
		return maxVelocity;
	}

	@Override
	public void onPin(PinData pindata) {
		int inputValue = pindata.getValue();
		processVariable = 180 * inputValue / resolution;
		// log.info(String.format("onPin received value %s converted to %s",
		// inputValue, processVariable));
		pid.setInput(pidKey, processVariable);
	}

	@Override
	public DeviceController getController() {
		return (DeviceController) controller;
	}

	public void attach(String pinArrayControlName, Integer pin) throws Exception {
		attach(pinArrayControlName, (int) pin);
	}

	public void attach(PinArrayControl pinArrayControl, int pin) throws Exception {
		this.pinArrayControl = pinArrayControl;
		if (pinArrayControl != null) {
			pinControlName = pinArrayControl.getName();
			isPinArrayControlSet = true;
			this.pin = pin;
		}

		// TODO The resolution is a property of the AD converter and should be
		// fetched thru a method call like controller.getADResolution()
		if (pinArrayControl instanceof Arduino) {
			resolution = 1024;
		}
		if (pinArrayControl instanceof Ads1115) {
			resolution = 65536;
		}

		int rate = 1000 / sampleTime;
		pinArrayControl.attach(this, pin);
		pinArrayControl.enablePin(pin, rate);
		broadcastState();
	}

	@Override
	public double getPowerLevel() {
		return powerLevel;
	}

	@Override
	public void setPowerLevel(double power) {
		this.powerLevel = power;
	}

	@Override
	public double getPowerOutput() {
		return powerMap.calcOutput(powerLevel);
	}

	@Override
	public double getTargetPos() {
		return targetPos;
	}

	/**
	 * // A bunch of unimplemented methods from ServoControl and MotorControl.
	 * // Perhaps I should create a new // DiyServoControl interface. // I was
	 * hoping to be able to avoid that, but might be a better solution
	 */
	@Override
	public void attach(ServoController controller, int pin, double pos) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void detach(ServoController controller) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(ServoController controller, int pin) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void lock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(double power) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveTo(double newPos, Double power) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEncoder(Encoder encoder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopAndLock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSpeed(double speed) {
		log.error("speed is depreciated, use setVelocity instead");

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

		ServiceType meta = new ServiceType(DiyServo.class.getCanonicalName());
		meta.addDescription("Controls a motor so that it can be used as a Servo");
		meta.addCategory("motor", "control");
		meta.addPeer("Arduino", "Arduino", "MotorController");
		meta.addPeer("Adafruit16CServoDriver", "Adafruit16CServoDriver", "MotorController");
		meta.addPeer("Pid", "Pid", "PID service");

		return meta;
	}

	@Override
	public void setPin(int pin) {
		this.pin = pin;
	}

	@Override
	public void attach(ServoController controller, int pin, double pos, double velocity) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override // FIXME - could be in ServoBase
	public boolean isAttached(ServoController controller) {
		return this.controller == controller;
	}

	@Override
	public boolean isAttached(MotorController controller) {
		return this.controller == controller;
	}

  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null){
      ret.add(controller.getName());
    }
    return ret;
  }
  
  	@Override
	public double getAcceleration() {
		return 1.0;
	}

    @Override
    public void setVelocity(double velocity) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public double getVelocity() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void attach(ServoController controller) throws Exception {
      // TODO Auto-generated method stub
      
    }
  

}