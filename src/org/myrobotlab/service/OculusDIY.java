package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.OculusData;
import org.myrobotlab.service.interfaces.CustomMsgListener;
import org.myrobotlab.service.interfaces.OculusDataListener;
import org.myrobotlab.service.interfaces.OculusDataPublisher;
import org.slf4j.Logger;

import com.leapmotion.leap.Frame;

public class OculusDIY extends Service implements CustomMsgListener, OculusDataPublisher, OculusDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OculusDIY.class);

	transient public Arduino arduino;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("arduino", "Arduino", "Arduino for DIYOculus and Myo");
		return peers;
	}

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
	
	public OculusDIY(String n) {
		super(n);
		arduino = (Arduino) createPeer("arduino");
	}

	@Override
	public String getDescription() {
		return "Service to receive and compute data from a DIY Oculus";
	}

	// public void onCustomMsg(Integer ay, Integer mx, Integer headingint) {
	@Override
	public void onCustomMsg(Object[] data) {
		Integer ay = (Integer) data[0];
		Integer mx = (Integer) data[1];
		Integer headingint = (Integer) data[2];
		this.computeAngles(mx, headingint,ay);
		OculusData oculus = new OculusData();
		oculus.yaw = Double.valueOf(rothead);
		oculus.pitch = Double.valueOf(head);
		oculus.roll = Double.valueOf(bicep);
		invoke("publishOculusData", oculus);

		System.out.println(head + "," + rothead);

	}

	public void calibrate() {
		resetValue = lastValue;
		offSet = (90 - lastValue);
		
		centerValue = lastValue2;
		minHead = centerValue - 300;
		maxHead = centerValue + 300;
	}

	public void computeAngles(Integer mx, Integer headingint , Integer ay) {
		
		lastValue2 = mx;
        double y = mx;
		double x = (20 + (((y - minHead) / (maxHead - minHead)) * (160 - 20)));
		head = (int)x;
		
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
		}
		else { rothead = lastrotheadvalue;
		}
		
	    y = ay;
	    x = (85 +(((y - 20)/(-16000 - 20))*(5 - 85)));
	    bicep = (int)x;
	    
	}

	public OculusData publishOculusData(OculusData oculus) {
		return oculus;

	}

	public void addOculusDataListener(Service service) {
		addListener("publishOculusData", service.getName(), "onOculusData", Frame.class);
	}

	public OculusData onOculusData(OculusData oculus) {
		return oculus;
	}

	@Override
	public void startService() {
		super.startService();
		arduino = (Arduino) startPeer("arduino");
		arduino.addCustomMsgListener(this);
		return;
	}

	public Arduino getArduino() {
		return arduino;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "video", "control", "sensor" };
	}

	public boolean connect(String port) {
		return arduino.connect(port);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			OculusDIY oculus = (OculusDIY) Runtime.start("oculus", "OculusDIY");
			Runtime.start("python", "Python");
			Runtime.start("gui", "GUIService");
			oculus.connect("COM15");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
