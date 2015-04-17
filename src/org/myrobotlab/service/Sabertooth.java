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
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServiceInterface;
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
public class Sabertooth extends Service implements SerialDataListener, MotorController {

	class MotorData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient MotorControl motor = null;
		int PWMPin = -1;
		int dirPin0 = -1;
		int dirPin1 = -1;
		int motorPort = -1;
		String port = null;
	}

	private static final long serialVersionUID = 1L;

	public final static int PACKETIZED_SERIAL_MODE = 4;

	int mode = PACKETIZED_SERIAL_MODE;

	public static final int PINMODE = 4;

	public final static Logger log = LoggerFactory.getLogger(Sabertooth.class);

	transient Serial serial;

	// range mapping

	private Integer address = 128;

	private float minX = 0;

	private float maxX = 180;

	private float minY = 0;

	private float maxY = 180;
	
	public static final int INPUT = 0x0;

	public static final int OUTPUT = 0x1;

	boolean setSaberToothBaud = false;

	// Motor name to its Data
	HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

	public final static int MOTOR1_FORWARD = 0;

	public final static int MOTOR1_BACKWARD = 1;

	public final static int SET_MIN_VOLTAGE = 2;

	public final static int SET_MAX_VOLTAGE = 3;

	public final static int MOTOR2_FORWARD = 4;

	public final static int MOTOR2_BACKWARD = 5;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		// put peer definitions in
		peers.put("serial", "Serial", "Serial Port");

		return peers;

	}

	// ----------Sabertooth Packetized Serial Mode Interface Begin
	// --------------


	public Sabertooth(String n) {
		super(n);
	}

	public boolean connect(String port) {
		// The valid baud rates are 2400, 9600, 19200 and 38400 baud
		return serial.connect(port, 38400, 8, 1, 0);
	}

	public void disconnect() {
		if (serial != null) {
			serial.disconnect();
		}
	}

	public void driveBackwardsMotor1(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR1_BACKWARD, speed);
	}

	public void driveBackwardsMotor2(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR2_BACKWARD, speed);
	}

	public void driveForwardMotor1(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR1_FORWARD, speed);
	}

	public void driveForwardMotor2(int speed) {
		if (speed < 0 || speed > 127) {
			error("invalid speed", speed);
			return;
		}
		sendPacket(MOTOR2_FORWARD, speed);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "motor", "control" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Object[] getMotorData(String motorName) {
		return new Object[] { motors.get(motorName).port };
	}

	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
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

	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin) {
		return motorAttach(motorName, pwrPin, dirPin, null);
	}

	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin, Integer encoderPin) {
		ServiceInterface sw = Runtime.getService(motorName);
		if (!sw.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		ServiceInterface service = sw;
		MotorControl motor = (MotorControl) service;

		MotorData md = new MotorData();
		md.motor = motor;
		md.PWMPin = pwrPin;
		md.dirPin0 = dirPin;
		motors.put(motor.getName(), md);
		// FIXME motor.setController(this);
		// sendMsg(PINMODE, md.PWMPin, OUTPUT);
		// sendMsg(PINMODE, md.dirPin0, OUTPUT);
		return true;
	}

	// FIXME - this seems very Arduino specific?

	public boolean motorDetach(String name) {
		if (motors.containsKey(name)) {
			motors.remove(name);
			return true;
		}

		return false;
	}

	public void motorMove(String name) {
		// a bit weird indirection - but this would support
		// adafruit to be attached to motors defined outside of
		// initialization
		MotorData d = motors.get(name);
		MotorControl mc = (MotorControl) Runtime.getService(name);

		double pwr = mc.getPowerLevel();
		int power = (int) (pwr * 127);

		// FIXME - optimization would be to have a "moveSendPacket" command
		// which took
		// data from MotorData
		if (d.port.equals("m1")) {
			if (pwr >= 0) {
				driveForwardMotor1(power);
			} else {
				driveBackwardsMotor1(Math.abs(power));
			}
		} else if (d.port.equals("m2")) {
			if (pwr >= 0) {
				driveForwardMotor1(power);
			} else {
				driveBackwardsMotor1(Math.abs(power));
			}
		} else {
			error("invalid port %s", d.port);
		}
	}

	public void motorMoveTo(String name, Integer position) {
		error("not implemented");
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
			serial.write((address + 0 + data) & 0x7F);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	public void setMaxVoltage(int maxVolts) {
		int actualValue = (int) Math.round(maxVolts / 5.12);
		info("setting max voltage to %d volts - actual value %f", actualValue);
		sendPacket(SET_MAX_VOLTAGE, actualValue);
	}

	// ----------MotorController Interface End --------------

	public void setMinVoltage(int min) {
		int actualValue = (min - 6) * 5;
		info("setting max voltage to %d volts - actual value %d", actualValue);
		if (actualValue < 0 || actualValue > 120) {
			error("invalid value must be between 0 and 120 %d", actualValue);
			return;
		}
		sendPacket(SET_MIN_VOLTAGE, actualValue);
	}

	@Override
	public void startService() {
		super.startService();
		serial = (Serial) startPeer("serial");
		serial.addByteListener(this);
	}

	@Override
	public final Integer onByte(Integer newByte) throws IOException {
		info("%s onByte %s", getName(), newByte);
		return newByte;
	}

	@Override
	public String onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		return portName;
	}

	@Override
	public String onDisconnect(String portName) {
		info("%s disconnected from %s", getName(), portName);
		return portName;
	}
	
	public Serial getSerial(){
		return serial;
	}
	

	// --- MotorController interface begin ----
	@Override
	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin, Integer encoderPin) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void motorMoveTo(String name, double position) {
		// TODO Auto-generated method stub
		
	}

	// --- MotorController interface end ----
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		try {
			
			String port = "COM15";
			
			// ---- Virtual Begin -----
			VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
			virtual.createVirtualPort(port);
			Serial uart = virtual.getUART();
			uart.setTimeout(300);
			// ---- Virtual End -----

			Sabertooth sabertooth = (Sabertooth)Runtime.start("sabertooth", "Sabertooth");
			sabertooth.connect(port);
			
			Motor m1 = (Motor) Runtime.start("m1", "Motor");
			
			// Motor m2 = (Motor) Runtime.createAndStart("m2", "Motor");

			Runtime.start("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}
}
