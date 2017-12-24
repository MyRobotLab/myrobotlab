package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.data.Orientation;
import org.myrobotlab.service.interfaces.OrientationListener;
import org.slf4j.Logger;

/**
 * 
 * OculusDIY - This service is the DIY oculus service. It might need a custom
 * build of MRLComm to work. Check with \@Alessandruino for questions.
 *
 */
public class OculusDiy extends Service implements OrientationListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OculusDiy.class);

	transient public Arduino arduino;
	transient public Mpu6050 mpu6050;

	transient Orientation oculus = new Orientation();
	Mapper mapperPitch = new Mapper(-180, 0, 0, 180);
	Mapper mapperYaw = new Mapper(-180, 180, 0, 360);

	Integer lastrotheadvalue = 90;
	Integer lastValue = 30;
	Integer resetValue = 30;
	Integer head = 90;
	Integer rothead = 90;
	Integer offSet = 0;
	Integer centerValue = 200;
	Integer minHead = -50;
	Integer maxHead = 500;
	Integer lastValue2 = 200;
	Integer bicep = 5;
	Integer headingint = 90;

	public OculusDiy(String n) {
		super(n);
		arduino = (Arduino) createPeer("arduino");
		mpu6050 = (Mpu6050) createPeer("mpu6050");
	}

	public void calibrate() {
		resetValue = lastValue;
		offSet = (90 - lastValue);

		centerValue = lastValue2;
		minHead = centerValue - 300;
		maxHead = centerValue + 300;
	}

	public void computeAngles(Double roll, Double pitch, Double yaw) {
	// public void computeAngles(Integer mx, Integer headingint, Integer ay) {

		lastValue2 = roll.intValue();
		double y = pitch;
		double x = (20 + (((y - minHead) / (maxHead - minHead)) * (160 - 20)));
		head = (int) x;

		lastValue = headingint;
		if (resetValue > 90 && lastValue < 0) {
			rothead = (offSet + headingint + 360);
		} else if (resetValue < -90 && lastValue > 0) {
			rothead = (offSet + headingint - 360);
		} else {
			rothead = (offSet + headingint);
		}
		System.out.println("difference is" + Math.abs(rothead - lastrotheadvalue));
		if (Math.abs(rothead - lastrotheadvalue) > 2) {
			lastrotheadvalue = rothead;
		} else {
			rothead = lastrotheadvalue;
		}

		y = yaw;
		x = (85 + (((y - 20) / (-16000 - 20)) * (5 - 85)));
		bicep = (int) x;

	}

	public void computeAnglesAndroid(float yaw, float roll, float pitch) {

		// head = (int) (180.0 +(((az - 9.82)/(-9.82 - 9.82))*(0.0 - 180.0)));
		head = (int) mapperPitch.calcOutput(pitch);
		// headingint = (int) mapperYaw.calc(yaw);
		headingint = (int) yaw;

		lastValue = headingint;
		if (resetValue > 90 && lastValue < 0) {
			rothead = (offSet + headingint + 360);
		} else if (resetValue < -90 && lastValue > 0) {
			rothead = (offSet + headingint - 360);
		} else {
			rothead = (offSet + headingint);
		}
		System.out.println("difference is" + Math.abs(rothead - lastrotheadvalue));
		if (Math.abs(rothead - lastrotheadvalue) > 2) {
			lastrotheadvalue = rothead;
		} else {
			rothead = lastrotheadvalue;
		}

		oculus.pitch = Double.valueOf(head);
		oculus.yaw = Double.valueOf(rothead);
		oculus.roll = Double.valueOf(roll);
		invoke("publishOrientation", oculus);

		System.out.println("pitch is " + head + "yaw is " + rothead);

	}

	public Orientation Orientation(Orientation oculus) {
		return oculus;

	}

	public void addOrientationListener(Service service) {
		addListener("publishOrientation", service.getName(), "onOrientation");
	}


	@Override
	public void startService() {
		super.startService();
		arduino = (Arduino) startPeer("arduino");
		mpu6050 = (Mpu6050) startPeer("mpu6050");
		return;
	}

	public Arduino getArduino() {
		return arduino;
	}

	public void connect(String port) throws IOException {
		arduino.connect(port);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {
			OculusDiy oculus = (OculusDiy) Runtime.start("oculus","OculusDiy");
			Runtime.start("python", "Python");
			Runtime.start("gui", "SwingGui");
			VirtualArduino virtual = (VirtualArduino)Runtime.start("virtual", "VirtualArduino");
			virtual.connect("COM15");
			oculus.connect("COM15");
			oculus.mpu6050.attach(oculus.arduino, "0", "0x68");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

  @Override
  public void releaseService() {
    mpu6050.releaseService(); 
    arduino.releaseService();
    arduino.serial.releaseService();
    super.releaseService();
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
		ServiceType meta = new ServiceType(OculusDiy.class.getCanonicalName());
		meta.addDescription("Service to receive and compute data from a DIY Oculus");
		meta.addCategory("video", "control", "sensor");
		meta.addPeer("arduino", "Arduino", "Arduino for DIYOculus and Myo");
		meta.addPeer("mpu6050", "Mpu6050", "mpu6050");
		return meta;
	}

	@Override
	public Orientation onOrientation(Orientation orientation) {
		// int[] data = (int[])event.getData();
		// Integer ay = (Integer) data[0];
		// Integer mx = (Integer) data[1];
		// Integer headingint = (Integer) data[2];
		this.computeAngles(orientation.roll, orientation.pitch, orientation.yaw);

		oculus.yaw = Double.valueOf(rothead);
		oculus.pitch = Double.valueOf(head);
		oculus.roll = Double.valueOf(bicep);
		invoke("publishOrientation", oculus);

		System.out.println(head + "," + rothead);
		return orientation;
	}

}