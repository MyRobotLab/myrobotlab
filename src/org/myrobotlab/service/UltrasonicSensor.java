package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.SensorData;
import org.slf4j.Logger;

public class UltrasonicSensor extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);

	public final Set<String> types = new HashSet<String>(Arrays.asList("SR04"));

	private int pings;
	private long max;
	private long min;
	private int sampleRate;
	private int sensorMinCM;
	private int sensorMaxCM;

	// TODO - avg ?

	private Integer trigPin = null;
	private Integer echoPin = null;
	private String type = "SR04";

	private transient Arduino controller;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("controller", "Arduino", "controller");
		return peers;
	}

	public UltrasonicSensor(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	// ---- part of interfaces begin -----

	public boolean setType(String type) {
		if (types.contains(type)) {
			this.type = type;
			return true;
		}
		return false;
	}

	public int getTriggerPin() {
		return trigPin;
	}

	public int getEchoPin() {
		return echoPin;
	}

	public boolean attach(String port, int trigPin, int echoPin) {
		return attach((String) null, port, trigPin, echoPin);
	}

	public boolean attach(String controller, String port, int trigPin, int echoPin) {
		if (controller == null && this.controller == null) {
			this.controller = (Arduino) startPeer("controller");
		}
		return attach(this.controller, port, trigPin, echoPin);
	}

	public boolean attach(Arduino controller, String port, int trigPin, int echoPin) {
		this.controller = controller;
		this.trigPin = trigPin;
		this.echoPin = echoPin;
		this.controller.connect(port);

		return controller.sensorAttach(this) != -1;
	}

	public long ping() {
		return ping(10);
	}

	public long ping(int timeout) {
		return controller.pulseIn(trigPin, echoPin, timeout);
	}

	public long range() {
		return controller.pulseIn(trigPin, echoPin) / 58;
	}

	public void startRanging() {
		startRanging(10); // 10000 uS = 10 ms
	}

	public void startRanging(int timeoutMS) {
		controller.sensorPollingStart(getName(), timeoutMS);
	}

	public void stopRanging() {
		controller.sensorPollingStop(getName());
	}

	/* FIXME !!! IMPORTANT PUT IN INTERFACE & REMOVE SELF FROM ARDUINO !!! */
	public long publishRange(SensorData sd) {
		++pings;
		// if (log.isDebugEnabled()){
		// TODO - add TimeUnits - cm
		long range = sd.duration / 58;
		log.info(String.format("publishRange name %s index %d duration %d range %d cm", sd.sensor.getName(), sd.sensorIndex, sd.duration, range));
		// }
		return range;
	}

	public void test() {
		// FIXME - there has to be a properties method to configure localized
		// testing
		boolean useGUI = true;

		UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start(getName(), "UltrasonicSensor");

		Servo servo = (Servo) Runtime.start("servo", "Servo");

		if (useGUI) {
			Runtime.createAndStart("gui", "GUIService");
		}

		// nice simple interface
		sr04.attach("COM15", 7, 8);
		
		servo.attach("sr04.controller", 4);
		servo.setSpeed(0.99f);
		servo.setEventsEnabled(true);
		servo.moveTo(30);

		for (int i = 0; i < 100; ++i) {
			log.info("ping 1");
			long duration = sr04.ping();
			log.info("duration {}", duration);
		}

		sr04.startRanging();
		log.info("here");
		servo.moveTo(130);
		sr04.stopRanging();

		// sensor.attach(arduino, "COM15", 7, 8);
		for (int i = 1; i < 200; i += 10) {
			sr04.startRanging(i);
			sr04.stopRanging();
			//servo.setSpeed(0.8f);
			//servo.moveTo(30);
			//servo.moveTo(175);
		}

		

		sr04.startRanging(5);
		sr04.startRanging(10);

		sr04.startRanging();

		sr04.stopRanging();

	}

	// ---- part of interfaces end -----

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start("sr04", "UltrasonicSensor");
		sr04.test();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

}
