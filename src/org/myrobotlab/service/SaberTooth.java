package org.myrobotlab.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.Arduino.MotorData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 * 
 *         SaberTooth service for the sabertooth motor controller command
 *         reference :
 *         http://www.dimensionengineering.com/datasheets/Sabertooth2x25.pdf
 * 
 *         Packet PseudoCode Putc(address); Putc(0); Putc(speed); Putc((address
 *         + 0 + speed) & 0b01111111);
 * 
 * 
 */
public class SaberTooth extends Service implements MotorController {

	private static final long serialVersionUID = 1L;

	public final static int PACKETIZED_SERIAL_MODE = 4;

	int mode = PACKETIZED_SERIAL_MODE;

	public final static Logger log = LoggerFactory.getLogger(SaberTooth.class);

	public SerialDeviceService serial;

	private Integer address = 128;

	// range mapping
	@Element
	private float minX = 0;
	@Element
	private float maxX = 180;
	@Element
	private float minY = 0;
	@Element
	private float maxY = 180;

	boolean setSaberToothBaud = false;

	class MotorData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient MotorControl motor = null;
		int motorPort = -1;
		String port = null;
	}

	// Motor name to its Data
	HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		// put peer definitions in
		peers.put("serial", "Serial", "Serial Port");

		return peers;

	}

	public SaberTooth(String n) {
		super(n);
		serial = (SerialDeviceService) startPeer("serial");
	}

	public boolean connect(String port) {
		// The valid baud rates are 2400, 9600, 19200 and 38400 baud
		return connect(port, 38400, 8, 1, 0);
	}

	public boolean connect(String port, int baud, int data, int stop, int parity) {
		// The valid baud rates are 2400, 9600, 19200 and 38400 baud
		if (baud > 38400 || baud < 2400 || data != 8 || stop != 1 || parity != 0) {
			error("parameters not valid for sabertooth - 38400 8 1 0 - %d %d %d %d", baud, data, stop, parity);
			return false;
		}
		return serial.connect(port, baud, data, stop, parity);
	}

	public boolean disconnect() {
		if (serial != null) {
			return serial.disconnect();
		}
		return true;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void sendPacket(int command, int data) {
		try {
			if (serial == null || !serial.isConnected()) {
				error("serial device not connected");
				return;
			}

			if (!setSaberToothBaud) {
				serial.write(170);
				setSaberToothBaud = true;
			}

			serial.write(address);
			serial.write(command);
			serial.write(data);
			serial.write((address + 0 + data) & 0b01111111);
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// ----------Sabertooth Packetized Serial Mode Interface Begin
	// --------------

	public final static int MOTOR1_FORWARD = 0;
	public final static int MOTOR1_BACKWARD = 1;
	public final static int SET_MIN_VOLTAGE = 2;
	public final static int SET_MAX_VOLTAGE = 3;
	public final static int MOTOR2_FORWARD = 4;
	public final static int MOTOR2_BACKWARD = 5;

	public void driveForwardMotor1(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR1_FORWARD, speed);
	}

	public void driveBackwardsMotor1(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR1_BACKWARD, speed);
	}

	public void setMinVoltage(int min) {
		int actualValue = (min - 6) * 5;
		info("setting max voltage to %d volts - actual value %d", actualValue);
		if (actualValue < 0 || actualValue > 120) {
			error("invalid value must be between 0 and 120 %d", actualValue);
			return;
		}
		sendPacket(SET_MIN_VOLTAGE, actualValue);
	}

	public void setMaxVoltage(int maxVolts) {
		int actualValue = (int) Math.round(maxVolts / 5.12);
		info("setting max voltage to %d volts - actual value %f", actualValue);
		sendPacket(SET_MAX_VOLTAGE, actualValue);
	}

	public void driveForwardMotor2(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR2_FORWARD, speed);
	}

	public void driveBackwardsMotor2(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR2_BACKWARD, speed);
	}

	// ----------Sabertooth Packetized Serial Mode Interface End --------------

	// ----------MotorController Interface Begin --------------

	/**
	 * Motor Controller specific method for attaching a motor In the case of a
	 * SaberTooth - we will need the motor name (of course) and the motor port
	 * number - SaberTooth supports 2 (M1 & M2)
	 * 
	 * @param motorName
	 * @param motorPort
	 * @return
	 */
	public boolean motorAttach(String motorName, int motorPort) {
		return motorAttach(motorName, new Object[] { motorPort });
	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		ServiceInterface sw = Runtime.getService(motorName);
		if (!sw.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}

		ServiceInterface service = sw;
		MotorControl motor = (MotorControl) service; // BE-AWARE - local
														// optimization ! Will
														// not work on remote
														// !!!
		if (motor == null || motorData == null) {
			error("null data or motor - can't attach motor");
			return false;
		}

		if (motorData.length != 1 || motorData[0] == null || motorData[0].getClass() != String.class || (!motorData[0].equals("m1") && !motorData[0].equals("m2"))) {
			error("motor data must be of the folowing format - motorAttach(motorName, (\"m1\" || \"m2\"))");
			return false;
		}

		MotorData md = new MotorData();
		md.motor = motor;
		md.port = (String) motorData[0];
		// FIXME optimize with sendMovePacket
		/*
		if (md.port.equals("m1")){
			bla
		}
		*/
		motors.put(motor.getName(), md);
		motor.setController(this);
		return true;

	}

	// FIXME - this seems very Arduino specific?
	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void motorMoveTo(String name, Integer position) {
		error("not implemented");
	}

	@Override
	public void motorMove(String name) {
		// a bit weird indirection - but this would support
		// adafruit to be attached to motors defined outside of
		// initialization
		MotorData d = motors.get(name);
		MotorControl mc = (MotorControl)Runtime.getService(name);
		
		float pwr = mc.getPowerLevel();
		int power = (int) (pwr * 127);
		
		// FIXME - optimization would be to have a "moveSendPacket" command which took
		// data from MotorData
		if (d.port.equals("m1")){
			if (pwr >= 0){
				driveForwardMotor1(power);
			} else {
				driveBackwardsMotor1(Math.abs(power));
			}
		} else if (d.port.equals("m2")){
			if (pwr >= 0){
				driveForwardMotor1(power);
			} else {
				driveBackwardsMotor1(Math.abs(power));
			}
		} else {
			error("invalid port %s", d.port);
		}
	}

	@Override
	public boolean motorDetach(String name) {
		if (motors.containsKey(name)){
			motors.remove(name);
			return true;
		}
		
		return false;
	}

	@Override
	public Object[] getMotorData(String motorName) {
		return new Object[] { motors.get(motorName).port };
	}

	// ----------MotorController Interface End --------------

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		SaberTooth sabertooth = new SaberTooth("sabertooth");
		sabertooth.startService();

		Motor m1 = (Motor) Runtime.createAndStart("m1", "Motor");
		Motor m2 = (Motor) Runtime.createAndStart("m2", "Motor");

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

}
