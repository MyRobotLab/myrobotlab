package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         TODO - implement Servo interface or Servo Shield create a GUIService for it
 * 
 * 
 */
public class ACEduinoMotorShield extends Service {

	private static final long serialVersionUID = 1L;

	public transient final static Logger log = LoggerFactory.getLogger(ACEduinoMotorShield.class.getCanonicalName());

	public static final int ACEDUINO_MOTOR_SHIELD_START = 50;
	public static final int ACEDUINO_MOTOR_SHIELD_STOP = 51;
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_POSITION = 52;
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_MIN_BOUNDS = 53;
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_MAX_BOUNDS = 54;

	// name of the Arduino
	Arduino arduino;

	public ACEduinoMotorShield(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void setPosition(int servo, int position) {
		arduino.sendMsg(ACEDUINO_MOTOR_SHIELD_SERVO_SET_POSITION, servo, position);
	}
	
	public boolean attach(Arduino arduino)
	{
		this.arduino = arduino;
		return true;
	}

	public void setBounds(int servo, int minposition, int maxposition) {
		// FIXME
		// 3 parameter method have to be decomposed into
		// 2 methods of 1 parameter until the Arduino
		// either accepts multiple parameters
		// not multi-threaded safe - ie get Servo then setServo Position
		arduino.sendMsg(ACEDUINO_MOTOR_SHIELD_SERVO_SET_MIN_BOUNDS, servo, minposition);
		arduino.sendMsg(ACEDUINO_MOTOR_SHIELD_SERVO_SET_MAX_BOUNDS, servo, maxposition);
	}

	public void start() {
		arduino.sendMsg(ACEDUINO_MOTOR_SHIELD_START);
	}

	public void stop() {
		arduino.sendMsg(ACEDUINO_MOTOR_SHIELD_STOP);
	}

	public Object getControllerName() {
		return arduino;
	}

	public boolean attach(String controllerName) {
		ServiceInterface sw = Runtime.getService(controllerName);
		if (sw == null)
		{
			log.error("can not find {}", controllerName);
			return false;
		}
		
		if (sw.getClass() != Arduino.class)
		{
			log.error("{} must be an Arduino", controllerName);
			return false;
		}
		
		return attach((Arduino) sw);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Arduino arduino = new Arduino("arduino");
		arduino.startService();

		ACEduinoMotorShield aceduinoShield = new ACEduinoMotorShield("aceduinoShield");
		aceduinoShield.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}

}
