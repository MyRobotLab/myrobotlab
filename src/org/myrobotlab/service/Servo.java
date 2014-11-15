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

import java.util.Vector;

import org.myrobotlab.framework.MRLError;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.VirtualSerialPort;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

/**
 * @author GroG
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
 */
@Root
public class Servo extends Service implements ServoControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Servo.class);

	ServoController controller;

	private Float inputX;

	// clipping
	@Element
	private float outputYMin = 0;
	@Element
	private float outputYMax = 180;

	// range mapping
	@Element
	private float minX = 0;
	@Element
	private float maxX = 180;
	@Element
	private float minY = 0;
	@Element
	private float maxY = 180;

	@Element
	private int rest = 90;

	private long lastActivityTime = 0;

	/**
	 * the pin is a necessary part of servo - even though this is really
	 * controller's information a pin is a integral part of a "servo" - so it is
	 * beneficial to store it allowing a re-attach during runtime
	 * 
	 * FIXME - not true - attach on the controller puts in the data - it should
	 * leave it even on a detach - a attach with pin should only replace it -
	 * that way pin does not need to be stored on the Servo
	 */
	private Integer pin;
	Vector<String> controllers;
	// computer thread based sweeping
	boolean isSweeping = false;

	// sweep types
	// TODO - computer implemented speed control (non-sweep)
	boolean speedControlOnUC = false;

	transient Thread sweeper = null;

	private boolean isAttached = false;
	private boolean inverted = false;

	public Servo(String n) {
		super(n);
		lastActivityTime = System.currentTimeMillis();
	}

	public void releaseService() {
		detach();
		super.releaseService();
	}

	@Override
	public boolean setController(ServoController controller) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}

		log.info(String.format("%s setController %s", getName(), controller.getName()));

		if (isAttached()) {
			warn("can not set controller %s when servo %s is attached", controller, getName());
			return false;
		}

		this.controller = controller;
		broadcastState();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#setPin(int)
	 */
	@Override
	public boolean setPin(int pin) {
		log.info(String.format("setting %s pin to %d", getName(), pin));
		if (isAttached()) {
			warn("%s can not set pin %d when servo is attached", getName(), pin);
			return false;
		}
		this.pin = pin;
		broadcastState();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.myrobotlab.service.interfaces.ServoControl#attach(java.lang.String,
	 * int)
	 */
	public boolean attach(String controller, Integer pin) {
		return attach((Arduino) Runtime.getService(controller), pin);
	}

	public boolean attach(Arduino controller, Integer pin) {
		setPin(pin);

		if (setController(controller)) {
			return attach();
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#attach()
	 */
	public boolean attach() {
		lastActivityTime = System.currentTimeMillis();
		if (isAttached) {
			log.info(String.format("%s.attach() - already attached - detach first", getName()));
			return false;
		}

		if (controller == null) {
			error("no valid controller can not attach %s", getName());
			return false;
		}

		isAttached = controller.servoAttach(getName(), pin);

		if (isAttached) {
			// changed state
			broadcastState();
		}

		return isAttached;
	}

	public void setInverted(boolean invert) {
		if (!inverted && invert) {
			map(maxX, minX, minY, maxY);
			inverted = true;
		} else {
			inverted = false;
		}

	}

	public boolean isInverted() {
		return inverted;
	}

	public float getMinInput() {
		return minX;
	}

	public float getMaxInput() {
		return maxX;
	}

	public void map(float minX, float maxX, float minY, float maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		broadcastState();
	}

	public void map(int minX, int maxX, int minY, int maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		broadcastState();
	}

	public int calc(float s) {
		return Math.round(minY + ((s - minX) * (maxY - minY)) / (maxX - minX));
	}

	/**
	 * basic move command of the servo - usually is 0 - 180 valid range but can
	 * be adjusted and / or re-mapped with min / max and map commands
	 * 
	 * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
	 * response
	 */

	@Override
	public void moveTo(Integer pos) {
		moveTo((float) pos);
	}

	public void moveTo(Double pos) {
		moveTo(pos.floatValue());
	}

	public void moveTo(Float pos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		inputX = pos;

		// the magic mapping
		int outputY = calc(inputX);

		if (outputY > outputYMax || outputY < outputYMin) {
			warn(String.format("%s.moveTo(%d) out of range", getName(), (int) outputY));
			return;
		}

		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range
		log.info("servoWrite({})", outputY);
		controller.servoWrite(getName(), outputY);
		lastActivityTime = System.currentTimeMillis();

	}

	public void writeMicroseconds(Integer pos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		inputX = pos.floatValue();

		// the magic mapping
		int outputY = calc(inputX);

		if (outputY > outputYMax || outputY < outputYMin) {
			warn(String.format("%s.moveTo(%d) out of range", getName(), (int) outputY));
			return;
		}

		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range
		log.info("servoWrite({})", outputY);
		controller.servoWriteMicroseconds(getName(), outputY);
		lastActivityTime = System.currentTimeMillis();

		// trying out broadcast after
		// position change
		broadcastState();
	}

	public boolean isAttached() {
		return isAttached;
	}

	public void setMinMax(int min, int max) {
		outputYMin = min;
		outputYMax = max;
		broadcastState();
	}

	public Integer getMin() {
		return (int) outputYMin;
	}

	public Integer getMax() {
		return (int) outputYMax;
	}

	public Float getPosFloat() {
		return inputX;
	}

	public int getPos() {
		return Math.round(inputX);
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	@Override
	public String getDescription() {
		return "basic servo service";
	}

	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother
	 * function
	 * 
	 */
	public class Sweeper extends Thread {

		int min;
		int max;
		int delay; // depending on type - this is 2 different things COMPUTER
		// its ms delay - CONTROLLER its modulus loop count
		int step;
		boolean oneWay;

		public Sweeper(String name, int min, int max, int delay, int step) {
			super(String.format("%s.sweeper", name));
			this.min = min;
			this.max = max;
			this.delay = delay;
			this.step = step;
			this.oneWay = false;
		}

		public Sweeper(String name, int min, int max, int delay, int step, boolean oneWay) {
			super(String.format("%s.sweeper", name));
			this.min = min;
			this.max = max;
			this.delay = delay;
			this.step = step;
			this.oneWay = oneWay;
		}

		@Override
		public void run() {

			if (inputX == null) {
				inputX = (float) min;
			}

			isSweeping = true;

			try {
				while (isSweeping) {
					// increment position that we should go to.
					if (inputX < max && step >= 0) {
						inputX += step;
					} else if (inputX > min && step < 0) {
						inputX += step;
					}

					// switch directions or exit if we are sweeping 1 way
					if ((inputX <= min && step < 0) || (inputX >= max && step > 0)) {
						if (oneWay) {
							isSweeping = false;
							break;
						}
						step = step * -1;
					}
					moveTo(inputX.intValue());
					Thread.sleep(delay);
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

		// for non-controller based sweeping,
		// this is the delay for the sweeper thread.
		public int getDelay() {
			return delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}
	}

	public void sweep(int min, int max) {
		sweep(min, max, 1, 1);
	}

	// FIXME - is it really speed control - you don't currently thread for
	// factional speed values
	public void sweep(int min, int max, int delay, int step) {
		sweep(min, max, delay, step, false);
	}

	public void sweep(int min, int max, int delay, int step, boolean oneWay) {
		// CONTROLLER TYPE SWITCH
		if (speedControlOnUC) {
			controller.servoSweep(getName(), min, max, step); // delay & step
																// implemented
		} else {
			if (isSweeping) {
				stop();
			}

			sweeper = new Sweeper(getName(), min, max, delay, step, oneWay);
			sweeper.start();
		}
	}

	public void setSweeperDelay(int delay) {
		((Sweeper) sweeper).setDelay(delay);
	}

	public void sweep() {
		sweep(Math.round(minX), Math.round(maxX), 1, 1);
	}

	@Override
	public String getControllerName() {
		if (controller == null) {
			return null;
		}

		return controller.getName();
	}

	// FIXME - really this could be normalized...
	// attach / detach - the detach would not remove the ServoData from
	// the controller only another attach with a different pin would
	// change it
	@Override
	public Integer getPin() {
		/*
		 * valiant attempt of normalizing - but Servo needs to know its own pin
		 * to support attach()
		 * 
		 * if (controller == null) { return null; }
		 * 
		 * return controller.getServoPin(getName());
		 */
		return pin;
	}

	public void setSpeed(Float speed) {
		if (speed == null) {
			return;
		}

		if (controller == null) {
			error("setSpeed - controller not set");
			return;
		}
		controller.setServoSpeed(getName(), speed);
	}

	public boolean detach() {
		if (!isAttached) {
			log.info(String.format("%s.detach() - already detach - attach first", getName()));
			return false;
		}

		if (controller != null) {
			if (controller.servoDetach(getName())) {
				isAttached = false;
				// changed state
				broadcastState();
				return true;
			}
		}

		return false;
	}

	public Vector<String> refreshControllers() {
		controllers = Runtime.getServicesFromInterface(ServoController.class.getCanonicalName());
		return controllers;
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
		controller.servoStop(getName());
	}

	public int setRest(int i) {
		rest = i;
		return rest;
	}

	public int getRest() {
		return rest;
	}

	public void rest() {
		moveTo(rest);
	}

	@Override
	public boolean setController(String controller) {

		ServoController sc = (ServoController) Runtime.getService(controller);
		if (sc == null) {
			return false;
		}

		return setController(sc);
	}

	public boolean setEventsEnabled(boolean b) {
		controller.setServoEventsEnabled(getName(), b);
		return b;
	}

	// choose to handle sweep on arduino or in MRL on host computer thread.
	public void setSpeedControlOnUC(boolean b) {
		speedControlOnUC = b;
	}

	// only if the sweep control is controled by computer and not arduino
	public boolean isSweeping() {
		return isSweeping;
	}

	// uber good
	public Integer publishServoEvent(Integer position) {
		return position;
	}

	// uber good
	public void addServoEventListener(Service service) {
		addListener("publishServoEvent", service.getName(), "onServoEvent", Integer.class);
	}

	public Status test() {
		Status status = super.test();

		try {

			// FIXME GSON or PYTHON MESSAGES

			boolean useGUI = true;
			boolean useVirtualPorts = true;
			boolean testBasicMoves = false;
			boolean testSweep = true;
			boolean testDetachReAttach = false;
			boolean testBlocking = false;

			
			// roll call of current services - because we are
			// going to make sure we whipe out all the "test" services
			// when we are done
			
			String port = "COM15";
			int pin = 4;

			info("setting up environment");
	
			Serial uart = null;
			if (useVirtualPorts) {
				info("starting virtual uart and serial port %s", port);
				// virtual testing
				VirtualSerialPort.createNullModemCable(port, "UART");
				uart = (Serial) Runtime.start("uart", "Serial");
				uart.connect("UART");
				info("recording rx file of uart");
				uart.recordRX("Servo.test.rx");
			}
			
			// TODO - check if !headless
			if (useGUI) {
				Runtime.start("gui", "GUIService");
			}

			// step 1 - test for success
			// step 2 - test for failures & recovery
			//String arduinoName = "arduino";
			int min = 10;
			int max = 170;

			int pause = 2000;

			// weird notation - but its nice when copying out
			// into some script or other code
			// also good to guarantee being started

			Servo servo = (Servo) Runtime.start(getName(), "Servo");

			// TODO test errros on servo move before attach

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect(port);
			info("attaching to pin %d", pin);

			if (!servo.attach(arduino, pin)) {
				error("could not attach to arduino");
			}

			if (servo.getPin() != pin) {
				error("bad pin value");
			}

			Vector<String> controllers = servo.refreshControllers();
			if (controllers.size() != 1) {
				error("should be on controller");
			}

			servo.setMinMax(min, max);

			info("should not move");
			sleep(pause);
			servo.moveTo(min - 1);
			servo.moveTo(max + 1);

			if (testBasicMoves) {

				info("testing 10 speeds on uC");
				sleep(pause);
				// TODO - moveToBlocking or callback when
				// servo reaches position would be nice here !
				for (int i = 0; i < 10; ++i) {
					float newSpeed = 1.0f - ((float) i * 0.1f);
					info("moveTo(pos=%d) %03f speed ", min, newSpeed);
					servo.setSpeed(newSpeed);
					servo.moveTo(min);
					sleep(pause);
					info("moveTo(pos=%d) %03f speed ", max, newSpeed);
					servo.moveTo(max);
					sleep(pause);
				}
			}

			info("back to rest");
			servo.setSpeed(1.0f);
			servo.rest();

			servo.setEventsEnabled(true);

			if (testSweep) {

				servo.setSpeed(0.9f);
				servo.sweep(min, max, 30, 1);

				servo.setEventsEnabled(false);

				/*
				 * 
				 * info("computer controlled sweep speed"); int newDelay; for
				 * (int i = 0; i < 10; ++i) { newDelay = i * 100 + 1; // FIXME -
				 * make GSON or PYTHON message output
				 * info("sweep (min=%d max=%d delay=%d step=%d )", min, max,
				 * newDelay, 1); servo.setSpeed(0.3f); servo.sweep(min, max,
				 * newDelay, 1); sleep(3 * pause); servo.stop(); servo.rest(); }
				 */

				info("uc controlled sweep speed");
				servo.stop();

				// TODO - test blocking ..
				servo.setSpeed(1.0f);

				servo.setSpeedControlOnUC(false);
				servo.sweep(min, max, 10, 1);
			}

			// TODO - detach - re-attach - detach (move) - re-attach - check for
			// no
			// move !

			if (testDetachReAttach) {
				info("testing detach re-attach");
				servo.detach();
				sleep(pause);
				servo.attach();

				info("make sure we can move after a re-attach");
				// put in testMode - collect controller data

				servo.moveTo(min);
				sleep(pause);
				servo.moveTo(max);
			}

			info("test completed");
			
			if (useVirtualPorts){
				uart.stopRecording();
			}
			
		} catch (Exception e) {
			status.addError(e);
		}
		return status;
	}

	public void test2() {

		Servo servo = (Servo) Runtime.start(getName(), "Servo");
		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
		arduino.connect("COM15");

		RemoteAdapter remote2 = (RemoteAdapter) Runtime.create("remote2", "RemoteAdapter");
		remote2.setTCPPort(6868);
		remote2.setUDPPort(6868);
		remote2.startService();
		servo.map(-1.0f, 1.0f, 0.0f, 180.0f);
		Runtime.start("gui2", "GUIService");
	}

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			Servo servo = (Servo) Runtime.start("servo", "Servo");
			servo.test();
			//servo.test2();
			/*
			 * Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			 * servo.setSpeedControlOnUC(true);
			 * Serial.createNullModemCable("COM15", "UART");
			 */
			// servo.test();
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	@Override
	public void move(Float offset) {
		//ss();
		
	}

}
