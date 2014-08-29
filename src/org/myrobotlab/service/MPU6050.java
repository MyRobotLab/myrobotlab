package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MPU6050 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MPU6050.class);
	
	private transient Serial serial;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("serial", "Serial", "Serial");
		return peers;
	}

	public MPU6050(String n) {
		super(n);
	}
	
	public boolean connect(String port){
		if (serial == null){
			serial = (Serial)Runtime.start("serial", "Serial");
		}
		
		return serial.connect(port);
	}

	@Override
	public String getDescription() {
		return "used as a general mpu6050";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();

		try {

			MPU6050 mpu6050 = (MPU6050)Runtime.start("mpu6050", "MPU6050");
			mpu6050.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
