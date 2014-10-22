package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.util.Map;
import org.slf4j.Logger;

public class EddieControlBoard extends Service {

	private static final long serialVersionUID = 1L;
	
	// Peers
	private transient Serial serial;
	private transient Keyboard keyboard;

	HashMap<String, Float> lastSensorValues = new HashMap<String, Float>();
	int sampleCount = 0;

	Map mapper = new Map(-1.0f, 1.0f, -127.0f, 127.0f);
	float leftMotorPower = 0.0f;
	float rightMotorPower = 0.0f;

	SensorPoller sensorPoller = null;

	class SensorPoller extends Thread {

		boolean isPolling = false;

		public void run() {
			isPolling = true;
			while (isPolling) {
				try {
					String dataString = getAnalogValues();
					if (dataString.length() == 32) {
						invoke("publishSensors", dataString);
					} else {
						error("invalid data string %s", dataString);
					}
					sleep(500);
				} catch (Exception e) {
					Logging.logException(e);
				}
			}

		}
	}
	
	public HashMap<String, Float> publishSensors(String dataString) {
		log.info(dataString);
		String[] values = dataString.split(" ");
		lastSensorValues.put("LEFT_IR", new Float(Integer.parseInt(values[0].trim(),16)));
		lastSensorValues.put("MIDDLE_IR", new Float(Integer.parseInt(values[1].trim(),16)));
		lastSensorValues.put("RIGHT_IR", new Float(Integer.parseInt(values[2].trim(),16)));
		lastSensorValues.put("BATTERY", new Float(0.00039f * Integer.parseInt(values[7].trim(),16)));
		++sampleCount;
		return lastSensorValues;
	}

	public boolean startSensors() {
		if (sensorPoller == null) {
			sensorPoller = new SensorPoller();
			sensorPoller.start();
			return true;
		}
		return false;
	}

	public boolean stopSensors() {
		if (sensorPoller != null) {
			sensorPoller.isPolling = false;
			sensorPoller = null;
			return true;
		}
		return false;
	}

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("serial", "Serial", "serial");
		peers.put("keyboard", "Keyboard", "serial");
		return peers;
	}

	public final static Logger log = LoggerFactory.getLogger(EddieControlBoard.class);

	public EddieControlBoard(String n) {
		super(n);
	}

	public void startService() {
		super.startService();
		if (serial == null) {
			serial = (Serial) startPeer("serial");
		}
		if (keyboard == null) {
			keyboard = (Keyboard) startPeer("keyboard");
		}

		keyboard.addKeyListener(this);
	}

	public boolean connect(String port) throws IOException {
		boolean ret = serial.connect(port, 115200, 8, 1, 0);
		if (ret) {
			stop();
		}
		return ret;
	}

	public void stop() throws IOException {
		go(0.0f, 0.0f);
	}

	// read commands begin ---
	public String getHwVersion() throws InterruptedException, IOException {
		serial.write("HWVER\r");
		String ret = serial.readString(5);
		return ret;
	}

	public String getVersion() throws InterruptedException, IOException {
		serial.write("VER\r");
		String ret = serial.readString(5);
		return ret;
	}

	public String getPingValues() throws InterruptedException, IOException {
		// depends
		serial.write("PING\r");
		String ret = serial.readString(5);
		return ret;
	}

	public String getAnalogValues() throws InterruptedException, IOException {
		//serial.clear();
		serial.write("ADC\r");
		String ret = serial.readString(32);
		return ret;
	}

	public String getGPIOInputs() throws InterruptedException, IOException {
		serial.write("INS\r");
		String ret = serial.readString(9);
		return ret;
	}

	public String getGPIOOutputs() throws InterruptedException, IOException {
		serial.write("OUTS\r");
		String ret = serial.readString(9);
		return ret;
	}

	public String getGPIOLowValues() throws InterruptedException, IOException {
		serial.write("LOWS\r");
		String ret = serial.readString(9);
		return ret;
	}

	public String getGPIOHighValues() throws InterruptedException, IOException {
		serial.write("HIGHS\r");
		String ret = serial.readString(9);
		return ret;
	}

	public String read() throws InterruptedException, IOException {
		return sendCommand("READ");
	}

	// read commands end ---

	public void onKey(String cmd) throws IOException {
		switch (cmd) {

		// left begin ---
		case "NumPad-7": {
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-4": {
			leftMotorPower -= 0.01;
			if (leftMotorPower < -1.0) {
				leftMotorPower = -1.0f;
			}
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-1": {
			leftMotorPower -= 0.01;
			if (leftMotorPower < -1.0) {
				leftMotorPower = -1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		// left end ---

		// right begin --
		case "NumPad-9": {
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-6": {
			rightMotorPower -= 0.01;
			if (rightMotorPower < -1.0) {
				rightMotorPower = -1.0f;
			}
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-3": {
			rightMotorPower -= 0.01;
			if (rightMotorPower < -1.0) {
				rightMotorPower = -1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		// right end --

		// center
		case "NumPad-8": {
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}

		case "NumPad-2": {
			leftMotorPower -= 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			rightMotorPower -= 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(rightMotorPower, rightMotorPower);
			break;
		}

		// stop all
		case "NumPad-5":
		case "Space": {
			leftMotorPower = 0.0f;
			rightMotorPower = 0.0f;
			go(rightMotorPower, rightMotorPower);
			break;
		}

		default: {
			warn("key command - [%s] - not defined", cmd);
			break;
		}

		}
	}

	public String sendCmd(String cmd, int expectedResponseLength) throws IOException, InterruptedException {
		log.info(String.format("sendCommand %s", cmd));
		String ret = null;

		serial.write(String.format("%s\r", cmd));
		ret = serial.readString(expectedResponseLength);

		return ret;
	}

	/**
	 * sending a command when expecting a string response in the context of
	 * blocking for response
	 * 
	 * @param cmd
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String sendCommand(String cmd) throws InterruptedException, IOException {
		log.info(String.format("sendCommand %s", cmd));
		String ret = null;

		// serial.setBlocking(true);
		serial.write(String.format("%s\r", cmd));
		// ret = serial.readString();
		// serial.setBlocking(false);

		return ret;
	}

	public void go(float left, float right) throws IOException {
		log.info(String.format("go %f %f", left, right));
		int l = mapper.calc(left);
		if (l > 127) {
			l = 128 - l;
		}
		int r = mapper.calc(right);
		if (r > 127) {
			r = 128 - r;
		}
		String cmd = String.format("GO %s %s\r", Integer.toHexString(l & 0xFF), Integer.toHexString(r & 0xFF)).toUpperCase();
		info("%s", cmd);
		serial.write(cmd);
	}

	public void setMotorSpeed(float left, float right) {
		// The left and right speeds have units of positions per second and are
		// entered as
		// signed (two's complement) 16-bit hex values. The range of allowed
		// values is
		// from 8000 to 7FFF.
		int myleft = (int) (left * 1000);
		int myright = (int) (right * 1000);
		String l = Integer.toHexString(myleft);
		String r = Integer.toHexString(myright);
		// Long.parseLong("ffff8000", 16);
		// serial.write(String.format("%s\r",cmd));
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Status test() {
		Status status = super.test();
		try {
			EddieControlBoard ecb = (EddieControlBoard) Runtime.start(getName(), "EddieControlBoard");
			Runtime.start("gui", "GUIService");
			Serial uart = ecb.serial.connectToVirtualUART();
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 00A CCE\r");
			ecb.startSensors();
			// ecb.connect(port)
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			uart.write("011 011 011 004 004 004 004 CBB\r");
			// ecb.go(1, 1);
		} catch (Exception e) {
			Logging.logException(e);
		}

		return status;
	}
	
	class Simulator extends Thread {
		public void run(){
			while (isRunning()){
				// how to auto correct & read the various parts
				// you know how to do this - ORIGINAL InputStream API Argggg !
				
			}
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			EddieControlBoard ecb = (EddieControlBoard) Runtime.start("ecb", "EddieControlBoard");
			ecb.test();

			// 129 -> 81
			// 128 -> 80 (full reverse)
			// 127 -> 7F (full forward)
			// 255 -> (little reverse)

			// 81 FF 0 1 7F
			// 128 --- 255 0 1 --- 127
			/*
			 * float i = 0.94f;
			 * 
			 * Map mapper = new Map(-1.0f, 1.0f, -127.0f, 127.0f); int x =
			 * mapper.calc(i); if (x > 127) { x = 128 - x; }
			 * 
			 * log.info("{}", Integer.toHexString(x & 0xFF));
			 * 
			 * String hex = Integer.toHexString(256 & 0xFF);
			 * log.info(hex.toUpperCase()); hex = Integer.toHexString(255 &
			 * 0xFF); log.info(hex.toUpperCase()); hex = Integer.toHexString(230
			 * & 0xFF); // slow reverse log.info(hex.toUpperCase());
			 */

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
