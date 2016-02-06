package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * 
 * MPU6050 - MPU-6050 sensor contains a MEMS accelerometer and a MEMS gyro in a single chip. 
 * It is very accurate, as it contains 16-bits analog to digital conversion hardware for each channel. 
 * Therefor it captures the x, y, and z channel at the same time.
 * http://playground.arduino.cc/Main/MPU-6050
 *
 */
public class Mpu6050 extends Service implements SerialDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Mpu6050.class);

	StringBuilder debugRX = new StringBuilder();

	private transient Serial serial;


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();

		try {

			Mpu6050 mpu6050 = (Mpu6050) Runtime.start("mpu6050", "MPU6050");

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Mpu6050(String n) {
		super(n);
	}

	public boolean connect(String port) {
		return serial.connect(port, this);
	}

	public String formatMRLCommMsg(String prefix, byte[] message, int size) {
		debugRX.setLength(0);
		if (prefix != null) {
			debugRX.append(prefix);
		}
		debugRX.append(String.format("MAGIC_NUMBER|SZ %d|FN %d", size, message[0]));
		for (int i = 1; i < size; ++i) {
			debugRX.append(String.format("|P%d %d", i, message[i]));
		}
		return debugRX.toString();
	}



	@Override
	public void startService() {
		super.startService();
		serial = (Serial) startPeer("serial");
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

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Mpu6050.class.getCanonicalName());
		meta.addDescription("General MPU 6050");
		meta.addCategory("microcontroller", "sensor");
		meta.addPeer("serial", "Serial", "Serial");
		return meta;
	}

}
