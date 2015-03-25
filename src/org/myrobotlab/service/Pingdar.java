package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Pingdar extends Service {

	public static class Point {

		public float r;
		public float theta;

		public Point(float servoPos, float z) {
			this.theta = servoPos;
			this.r = z;
		}

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Pingdar.class);

	public int sweepMin = 0;
	public int sweepMax = 180;

	public int step = 1;
	transient private Arduino arduino;
	transient private Servo servo;

	transient private UltrasonicSensor sensor;
	// TODO - changed to XDar - make RangeSensor interface -> publishRange
	// TODO - set default sample rate
	private boolean isAttached = false;
	private Long lastRange;

	private Integer lastPos;

	private int rangeCount = 0;

	long rangeAvg = 0;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("arduino", "Arduino", "arduino");
		peers.put("sensor", "UltrasonicSensor", "sensor");
		peers.put("servo", "Servo", "servo");

		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Runtime.createAndStart("gui", "GUIService");
		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * Serial.createNullModemCable("uart", "COM10"); Serial serial =
		 * (Serial)Runtime.createAndStart("uart", "Serial");
		 * 
		 * serial.connect("uart");
		 */

		Pingdar pingdar = (Pingdar) Runtime.start("pingdar", "Pingdar");
		pingdar.test();

		// Runtime.createAndStart("gui", "GUIService");

		// pingdar.attach("COM15", 7, 8, 9);
		// pingdar.sweep();
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

	public Pingdar(String n) {
		super(n);
	}

	// ----------- interface begin ----------------

	public boolean attach(Arduino arduino, String port, UltrasonicSensor sensor, int trigPin, int echoPin, Servo servo, int servoPin) throws IOException {
		this.arduino = arduino;
		this.sensor = sensor;
		this.servo = servo;

		if (isAttached) {
			warn("already attached - detach first");
		}

		arduino.connect(port);

		if (!sensor.attach(port, trigPin, echoPin)) {
			error("could not attach sensor");
			return false;
		}

		// FIXME sensor.addRangeListener
		// publishRange --> onRange
		sensor.addRangeListener(this);
		servo.addServoEventListener(this);

		if (!servo.attach(arduino, servoPin)) {
			error("could not attach servo");
			return false;
		}

		isAttached = true;
		return true;
	}

	// UBER GOOD
	// UBER GOOD !!!! max & min complexity with service creation support with
	// Peers !!!!
	// attach (arduino port sensor trigPin echoPin servo servoPin ) <- max
	// complexity - no service creation
	// attach (port trigPin echoPin servoPin) <- min complexity - service
	// creation on peers
	public boolean attach(String port, int trigPin, int echoPin, int servoPin) throws IOException {
		return attach(arduino, port, sensor, trigPin, echoPin, servo, servoPin);
	}

	public Arduino getArduino() {
		return arduino;
	}

	// ----------- interface end ----------------

	@Override
	public String[] getCategories() {
		return new String[] { "sensor", "display" };
	}

	@Override
	public String getDescription() {
		return "used as a ultra sonic radar";
	}

	public UltrasonicSensor getSensor() {
		return sensor;
	}

	public Servo getServo() {
		return servo;
	}

	// sensor data has come in
	// grab the latest position
	public Long onRange(Long range) {
		info("range %d", range);
		// filter too low
		// TODO this should be done on the Arduino
		if (range < 10) {
			return range;
		}

		rangeAvg += range;

		lastRange = range;
		++rangeCount;

		/*
		 * Point p = new Point(lastPos, range); invoke("publishPingdar", p);
		 */
		return lastRange;
	}

	public Integer onServoEvent(Integer pos) {
		info("pos %d", pos);
		lastPos = pos;
		if (rangeCount > 0) {
			Point p = new Point(lastPos, rangeAvg / rangeCount);
			rangeAvg = 0;
			rangeCount = 0;
			invoke("publishPingdar", p);
		}

		return lastPos;
	}

	public Point publishPingdar(Point point) {
		return point;
	}

	@Override
	public void startService() {
		super.startService();
		arduino = (Arduino) startPeer("arduino");
		sensor = (UltrasonicSensor) startPeer("sensor");
		servo = (Servo) startPeer("servo");
	}

	public void stop() {
		super.stopService();
		sensor.stopRanging();
		servo.setEventsEnabled(false);
		servo.stop();
	}

	public boolean sweep() {
		return sweep(sweepMin, sweepMax);
	}

	public boolean sweep(int sweepMin, int sweepMax) {
		this.sweepMin = sweepMin;
		this.sweepMax = sweepMax;
		this.step = 1; // FIXME STEP

		if (!isAttached) {
			error("not attached");
			return false;
		}
		// TODO - configurable speed
		sensor = getSensor();
		servo = getServo();

		sensor.addRangeListener(this);
		servo.addServoEventListener(this);

		servo.setSpeed(0.20f);
		servo.setEventsEnabled(true);
		// STEP ???
		servo.sweep(sweepMin, sweepMax, 1, step);

		sensor.startRanging();
		return true;
	}

	@Override
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());

		try {

			Pingdar pingdar = (Pingdar) Runtime.start(getName(), "Pingdar");
			pingdar.attach("COM15", 7, 8, 4);

			pingdar.arduino.setSampleRate(5000); // <-- DOES THIS MAKE A
													// DIFFERENCE ???
			pingdar.sweep(20, 160);

			pingdar.stop();

			pingdar.sweep(80, 90);
			pingdar.stop();
		} catch (Exception e) {
			Logging.logError(e);
		}

		return status;
	}

}
