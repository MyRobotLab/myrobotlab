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

import java.util.ArrayList;
import java.util.Vector;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 * Servos have both input and output.  Input is usually of the range of integers between 0 - 180, and
 * output can relay those values directly to the servo's firmware (Arduino ServoLib, I2C controller, etc)
 * 
 * However there can be the occasion that the input comes from a system which does not have the same range.
 * Such that input can vary from 0.0 to 1.0.  For example, OpenCV coordinates are often returned in this range.
 * When a mapping is needed Servo.map can be used.  For this mapping Servo.map(0.0, 1.0, 0, 180) might be
 * desired.  Reversing input would be done with Servo.map(180, 0, 0, 180)
 * 
 * outputY - is the values sent to the firmware, and should not necessarily be confused with the inputX
 * which is the input values sent to the servo
 *
 */
@Root
public class Servo extends Service implements ServoControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Servo.class.getCanonicalName());

	ServoController controller;

	@Element
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

	private int rest = 90;
	
	private long lastActivityTime = 0;

	/**
	 * the pin is a necessary part of servo - even though this is really
	 * controller's information a pin is a integral part of a "servo" - so it is
	 * beneficial to store it allowing a re-attach during runtime
	 */
	private Integer pin;
	Vector<String> controllers;

	// FIXME - should be implemented inside the Arduino / ServoController - but
	// has to be tied to position
	int sweepStart = 0;
	int sweepEnd = 180;
	int sweepDelayMS = 1000;
	int sweepIncrement = 1;
	boolean sweeperRunning = false;
	transient Thread sweeper = null;

	private boolean isAttached = false;


	/**
	 * not needed but kept to allow setInverted 
	 */
	private boolean inverted = false;

	public Servo(String n) {
		super(n);
		load();
		lastActivityTime = System.currentTimeMillis();
	}

	public void releaseService() {
		detach();
		super.releaseService();
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.ServoControl#setController(org.myrobotlab.service.interfaces.ServoController)
	 */
	@Override
	public boolean setController(ServoController controller) {
		log.info(String.format("%s setController %s", getName(), controller));
		
		if (isAttached())
		{
			warn("can not set controller %s when servo %s is attached", controller, getName());
			return false;
		}
		
		if (controller == null) {
			error("setting null as controller");
			return false;
		}
		this.controller = controller;
		broadcastState();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.ServoControl#setPin(int)
	 */
	@Override
	public boolean setPin(int pin) {
		log.info(String.format("setting %s pin to %d", getName(), pin));
		if (isAttached())
		{
			warn("%s can not set pin %d when servo is attached", getName(), pin);
			return false;
		}
		this.pin = pin;
		broadcastState();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.ServoControl#attach(java.lang.String, int)
	 */
	public boolean attach(String controller, int pin) {
		setPin(pin);
		
		if (setController(controller)){
			return attach();
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.ServoControl#attach()
	 */
	public boolean attach() {
		lastActivityTime = System.currentTimeMillis();
		if (isAttached){
			log.info(String.format("%s.attach() - already attached - detach first", getName()));
			return false;
		}
		
		if (controller == null) {
			error("no valid controller can not attach %s", getName());
			return false;
		}

		isAttached  = controller.servoAttach(getName(), pin);
		
		if (isAttached){
			// changed state
			broadcastState();
		}
		
		return isAttached;
	}
	
	
	public void setInverted(boolean isInverted) {
		if (!inverted  && isInverted){
			map(maxX, minX, minY, maxY);
			inverted = true;
		} else {
			inverted = false;
		}
		
	}

	public boolean isInverted() {
		return inverted;
	}
	
	public boolean map(float minX, float maxX, float minY, float maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		return true;
	}

	public boolean map(int minX, int maxX, int minY, int maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		return true;
	}
	
	/*
	public static double mapRange(double minX, double maxX, double minY, double maxY, double s){
		return minY + ((s - minX)*(maxY - minY))/(maxX - minX);
	}
	*/
	
	public float calc(float s){
		return  minY + ((s - minX)*(maxY - minY))/(maxX - minX);
	}

	/**
	 * simple move servo to the location desired TODO Float Version - is range 0
	 * - 1.0 ???
	 */
	@Override
	public void moveTo(Integer pos) {
		moveTo((float)pos);
	}
	
	public void moveTo(float inputX) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}
		
		// the magic mapping
		float outputY = calc(inputX);
		
		if (outputY > outputYMax || outputY < outputYMin){
			if ((int)outputY == outputY){
				warn(String.format("%s.moveTo(%d) out of range", getName(), (int)outputY));
			} else {
				warn(String.format("%s.moveTo(%f) out of range", getName(), outputY));
			}
			return;
		}
		
		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range
		log.info("servoWrite({})", outputY);
		controller.servoWrite(getName(), (int)outputY);
		lastActivityTime = System.currentTimeMillis();
		this.inputX = inputX;
	}

	/**
	 * moves the servo in the range -1.0 to 1.0 where in a typical servo -1.0 =
	 * 0 0.0 = 90 1.0 = 180 setting the min and max will affect the range where
	 * -1.0 will always be the minPos and 1.0 will be maxPos
	 *//*
	@Override
	public void move(Float pos) {
		Float amount = pos;

		if (amount > 1 || amount < -1) {
			error("%s.move %d out of range", getName(), amount);
			return;
		}

		int range = limitMax - limitMin;
		int newPos = Math.abs((int) (range / 2 * amount - range / 2));

		controller.servoWrite(getName(), newPos);
		position = newPos;

	}
*/
	
	public boolean isAttached() {
		return isAttached;
	}

	public void setMinMax(int min, int max) {
		setMinMax((float)min, (float)max);
	}
	
	public void setMinMax(float min, float max){
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

	public Float getPosition() {
		return inputX;
	}
	
	public int getPositionInt() {
		return Math.round(inputX);
	}

	public long getLastActivityTime(){
		return lastActivityTime;
	}

	@Override
	public String getDescription() {
		return "<html>service for a servo</html>";
	}

	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother
	 * function
	 * 
	 */
	private class Sweeper implements Runnable {
		@Override
		public void run() {

			while (sweeperRunning) {
				// controller.servoMoveTo(name, position);
				inputX += sweepIncrement;

				// switch directions
				if ((inputX <= sweepStart && sweepIncrement < 0) || (inputX >= sweepEnd && sweepIncrement > 0)) {
					sweepIncrement = sweepIncrement * -1;
				}

				moveTo(inputX);

				try {
					Thread.sleep(sweepDelayMS);
				} catch (InterruptedException e) {
					sweeperRunning = false;
					logException(e);
				}
			}
		}

	}

	public void sweep(int sweepStart, int sweepEnd, int sweepDelayMS, int sweepIncrement) {
		if (sweeperRunning) {
			stopSweep();
		}
		this.sweepStart = sweepStart;
		this.sweepEnd = sweepEnd;
		this.sweepDelayMS = sweepDelayMS;
		this.sweepIncrement = sweepIncrement;

		sweeperRunning = true;

		sweeper = new Thread(new Sweeper());
		sweeper.start();
	}

	public void stopSweep() {
		sweeperRunning = false;
		sweeper = null;
	}

	public void sweep() {
		sweep(sweepStart, sweepEnd, sweepDelayMS, sweepIncrement);
	}

	@Override
	public String getControllerName() {
		if (controller == null) {
			return null;
		}

		return controller.getName();
	}

	@Override
	public Integer getPin() {
		/*
		 * valiant attempt of normalizing - but Servo needs to know its own pin
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
		if (!isAttached){
			log.info(String.format("%s.detach() - already detach - attach first", getName()));
			return false;
		}
		
		if (controller != null) {
			if (controller.servoDetach(getName())){
				isAttached = false;
				// changed state
				broadcastState();
				return true;
			}
		}
		
		return false;
	}
	
	
	public void test() {
		
		try {
		ArrayList<String> errors = new ArrayList<String>();
		
		// test standard Python service page script
		log.info("service python script begin---");
		Python python = (Python)Runtime.createAndStart("python","Python");
		String resourceName = "Python/examples/bork.py";
		python.execResource(resourceName);
		if (python.getLastError() != null){
			errors.add(String.format("service python script error [%s]", python.getLastError()));
			python.clearLastError();
		}
		
		log.info("service python script end---");
		
		// start peers ?
		Arduino arduino = (Arduino)Runtime.createAndStart("arduino","Arduino");
		
		// TEST RE-ENTRANT attach !!
	 
		// test limits
		
		// test refreshControllers 
		
		// if it gets in a limit - must be able to move out.. no?
		// TODO - testing invalid states
		setController("arduino");
		attach();
		
		
		//put in testMode - collect controller data
		
	 	moveTo(0);
	 	moveTo(180);
	 
	 
		for (int i = 0; i < 10000; ++i) {
			setSpeed(0.6f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.6f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.5f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.5f);
			moveTo(90);
			moveTo(180);
		}
		
		} catch (Exception e){
			error(e);
		}
		
		info("test completed");
	}

	public Vector<String> refreshControllers() {
		controllers = Runtime.getServicesFromInterface(ServoController.class.getCanonicalName());
		return controllers;
	}

	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.ServoControl#stopServo()
	 */
	@Override
	public void stopServo() {
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
		if (sc == null){
			return false;
		}
		
		return setController(sc);
	}
	
	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		Servo right = new Servo("servo01");
		
		right.map(5, 180, 5, 180);
		log.info("{}",right.calc(1));
		log.info("{}",right.calc(3));
		right.map(180, 0, 0, 180);
		log.info("{}",right.calc(3));
		right.map(0, 180, 5, 178);
		log.info("{}",right.calc(0));
		
		right.startService();
		right.test();

		// FIXME - routing of servo.attach("arduino", 3);

		Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");

		arduino.connect("COM4");

		
		arduino.servoAttach(right.getName(), 13);

		right.test();

		Runtime.createAndStart("gui", "GUIService");

		// right.attach(serviceName)
		/*
		 * Servo left = new Servo("left"); left.startService();
		 * 
		 * //Servo neck = new Servo("neck"); //neck.startService();
		 * 
		 * for (int i = 0; i < 30; ++i) {
		 * 
		 * right.attach("arduino", 2); left.attach("arduino", 3);
		 * 
		 * right.moveTo(120); // 70 back left.moveTo(70); // 118 back
		 * 
		 * Thread.sleep(10000);
		 * 
		 * right.moveTo(90); left.moveTo(90);
		 * 
		 * //right.detach(); //left.detach(); }
		 */

	}

	


	
	
}
