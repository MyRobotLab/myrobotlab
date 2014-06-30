package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;

public class MagaBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MagaBot.class.getCanonicalName());

	public MagaBot(String n) {
		super(n);
	}

	SerialDevice serialDevice = null;
	private boolean isInitialized = false;

	public void init(String serialPortName) {
		if (!isInitialized) {
			try {
				serialDevice = SerialDeviceFactory.getSerialDevice(serialPortName, 9600, 8, 1, 0);
				serialDevice.open();
				isInitialized = true;
			} catch (SerialDeviceException e) {
				logException(e);
			}
		}

	}

	/*
	 * xicombd: - '1' to Assisted Navigation - 'w' to go forward - 's' to go
	 * backward - 'a' to go left - 'd' to go right - 'p' to stop - '2' to
	 * Obstacle Avoidance - '3' to start Line Following
	 * 
	 * 'i' if the ir sensors are activated
	 */
	/*
	public void sendOrder(String o) {
		try {
			serialDevice.write(o);
		} catch (IOException e) {
			logException(e);
		}
	}
	*/

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		MagaBot template = new MagaBot("template");
		template.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		
	}

}
