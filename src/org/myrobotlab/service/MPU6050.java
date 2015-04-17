package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

public class MPU6050 extends Service implements SerialDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MPU6050.class);

	StringBuilder debugRX = new StringBuilder();

	private transient Serial serial;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("serial", "Serial", "Serial");
		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();

		try {

			MPU6050 mpu6050 = (MPU6050) Runtime.start("mpu6050", "MPU6050");
			mpu6050.test();

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public MPU6050(String n) {
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
	public String[] getCategories() {
		return new String[] { "microcontroller" };
	}

	@Override
	public String getDescription() {
		return "used as a general mpu6050";
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

}
