package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.VirtualSerialPort;
import org.myrobotlab.service.interfaces.RangeListener;
import org.slf4j.Logger;

public class UltrasonicSensor extends Service implements RangeListener {

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
	private Long lastRange;

	private transient Arduino arduino;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("arduino", "Arduino", "arduino");
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
	
	public long getLastRange(){
		return lastRange;
	}

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

	public boolean attach(String arduino, String port, int trigPin, int echoPin) {
		if (arduino == null && this.arduino == null) {
			this.arduino = (Arduino) startPeer("arduino");
		}
		return attach(this.arduino, port, trigPin, echoPin);
	}

	public boolean attach(Arduino arduino, String port, int trigPin, int echoPin) {
		this.arduino = arduino;
		this.trigPin = trigPin;
		this.echoPin = echoPin;
		this.arduino.connect(port);

		return arduino.sensorAttach(this) != -1;
	}

	public long ping() {
		return ping(10);
	}

	public long ping(int timeout) {
		return arduino.pulseIn(trigPin, echoPin, timeout);
	}

	public long range() {
		return arduino.pulseIn(trigPin, echoPin) / 58;
	}

	public void startRanging() {
		startRanging(10); // 10000 uS = 10 ms
	}

	public void startRanging(int timeoutMS) {
		arduino.sensorPollingStart(getName(), timeoutMS);
	}

	public void stopRanging() {
		arduino.sensorPollingStop(getName());
	}
	
	/* FIXME !!! IMPORTANT PUT IN INTERFACE & REMOVE SELF FROM ARDUINO !!! */
	public Long publishRange(Long duration) {
		++pings;
		// if (log.isDebugEnabled()){
		// TODO - add TimeUnits - cm
		//long range = sd.duration / 58;
		lastRange = duration / 58;
		//if (log.isDebugEnabled()){
			//log.debug(String.format("publishRange name %s duration %d range %d cm", getName(), duration, lastRange));
		// }
		return lastRange;
	}

	// TODO - Virtual Serial test - do a record of tx & rx on a real sensor
	// then send the data - IT MUST BE INTERLEAVED
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		// FIXME - there has to be a properties method to configure localized
		// testing
		boolean useGUI = true;
		boolean useVirtualPorts = true;
		int triggerPin = 7;
		int echoPin = 8;
		
		String port = "COM15";
		
		UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start(getName(), "UltrasonicSensor");
		Servo servo = (Servo) Runtime.start("servo", "Servo");

		// && depending on headless
		if (useGUI) {
			Runtime.start("gui", "GUIService");
		}

		Serial uart = null;
		if (useVirtualPorts) {
			// virtual testing
			VirtualSerialPort.createNullModemCable(port, "UART");
			uart = (Serial) Runtime.start("uart", "Serial");
			uart.connect("UART");
			uart.setBinaryFileFormat(false);
			// uart.record("test/Servo/servo.test.1");
			uart.recordRX("test/Servo/servo.test.1.rx.dec");
		}

		// nice simple interface
		
		sr04.attach(port, triggerPin, echoPin);
		//arduino.re
		// TODO - VIRTUAL NULL MODEM WITH TEST DATA !!!!
		// RECORD FROM ACTUAL SENSOR !!!
		
		//sr04.arduino.setLoadTimingEnabled(true);
		
		sr04.addRangeListener(this);
		arduino.recordRX(null);
		
		sr04.startRanging();
		log.info("here");
		sr04.stopRanging();

		arduino.stopRecording();
		
		sr04.arduino.setLoadTimingEnabled(true);
		sr04.arduino.setLoadTimingEnabled(false);
		
		servo.attach("sr04.arduino", 4);
		servo.setSpeed(0.99f);
		servo.setEventsEnabled(true);
		servo.setEventsEnabled(false);
		servo.moveTo(30);
		
		/*

		for (int i = 0; i < 100; ++i) {
			log.info("ping 1");
			long duration = sr04.ping();
			log.info("duration {}", duration);
		}
		*/
		servo.setEventsEnabled(true);
		sr04.startRanging();
		log.info("here");
		servo.moveTo(130);
		sr04.stopRanging();

		// sensor.attach(arduino, "COM15", 7, 8);
		for (int i = 1; i < 200; i += 10) {
			sr04.startRanging(i);
			
			servo.setSpeed(0.8f);
			servo.moveTo(30);
			servo.moveTo(175);
			sr04.stopRanging();
		}

		sr04.startRanging(5);
		sr04.startRanging(10);

		sr04.startRanging();

		sr04.stopRanging();
		
		return status;

	}

	// Uber good - .. although this is "chained" versus star routing
	// Star routing would be routing from the Arduino directly to the Listener
	// The "chained" version takes 2 thread contexts :( .. but it has the benefit
	// of the "publishRange" method being affected by the Sensor service e.g.
	// change units, sample rate, etc
	public void addRangeListener(Service service) {
		addListener("publishRange", service.getName(), "onRange", Long.class);
	}
	
	@Override
	public void onRange(Long range) {
		log.info(String.format("RANGE: %d", range));
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

	@Override
	public String[] getCategories() {
		return new String[] {"sensor"};
	}

}
