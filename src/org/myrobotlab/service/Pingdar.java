package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Pingdar extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Pingdar.class);

	public int sweepMin = 0;
	public int sweepMax = 180;
	public int step = 1;

	transient private Arduino arduino;
	transient private Servo servo;
	transient private UltrasonicSensor sensor;

	private boolean isAttached = false;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("arduino", "Arduino", "arduino");
		peers.put("sensor", "UltrasonicSensor", "sensor");
		peers.put("servo", "Servo", "servo");

		return peers;
	}

	public Pingdar(String n) {
		super(n);
	}

	public boolean sweep() {
		return sweep(sweepMin, sweepMax, step);
	}

	public boolean sweep(int sweepMin, int sweepMax, int step) {
		this.sweepMin = sweepMin;
		this.sweepMax = sweepMax;
		this.step = step;

		if (!isAttached) {
			error("not attached");
			return false;
		}
		// TODO - configurable speed
		servo.setSpeed(0.9f);
		servo.setEventsEnabled(true);
		servo.sweep(sweepMin, sweepMax, 1, 1);
		
		return true;
	}
	
	// ----------- interface begin ----------------

	public Servo getServo(){
		if (servo == null){
			servo = (Servo)startPeer("servo");
		}
		return servo;
	}
	
	public UltrasonicSensor getSensor(){
		if (sensor == null){
			sensor = (UltrasonicSensor)startPeer("sensor");
		}
		return sensor;
	}
	
	public Arduino getArduino(){
		if (arduino == null){
			arduino = (Arduino)startPeer("arduino");
		}
		return arduino;
	}

	// ----------- interface end  ----------------
	
	// UBER GOOD
	// UBER GOOD !!!! max & min complexity with service creation support with Peers !!!!
	// attach (arduino port sensor trigPin echoPin servo servoPin ) <- max complexity - no service creation
	// attach (port trigPin echoPin servoPin) <- min complexity - service creation on peers
	public boolean attach(String port, int trigPin, int echoPin, int servoPin) {
		arduino = (Arduino)startPeer("arduino");
		sensor = (UltrasonicSensor)startPeer("sensor");
		servo = (Servo)startPeer("servo");
		
		return attach(arduino, port, sensor, trigPin, echoPin, servo, servoPin);
	}
	
	public boolean attach(Arduino arduino, String port, UltrasonicSensor sensor, int trigPin, int echoPin, Servo servo, int servoPin) {
		this.arduino = arduino;
		this.sensor = sensor;
		this.servo = servo;

		if (isAttached) {
			warn("already attached - detach first");
		}

		if (!arduino.connect(port)) {
			error("could not connect arduino");
			return false;
		}

		sensor.attach(arduino, port, trigPin, echoPin);
		servo.attach(arduino, servoPin);

		if (!arduino.pingdarAttach(this)) {
			error("could not attach pingdar %s", getName());
			return false;
		}
		isAttached = true;
		return true;
	}

	@Override
	public String getDescription() {
		return "used as a ultra sonic radar";
	}
	
	public void test(){
		Pingdar pingdar = (Pingdar)Runtime.start(getName(), "Pingdar");
		pingdar.attach("COM15", 7, 8, 4);
		pingdar.servo.setSpeedControlOnUC(false);
		pingdar.sweep(10, 170, 1);
		pingdar.sensor.startRanging();
		pingdar.sensor.stopRanging();
		pingdar.stop();
	}

	public void stop() {
		sensor.stopRanging();
		servo.setEventsEnabled(false);
		servo.stop();
	}
	
	public void startService(){
		servo = (Servo)startPeer("servo");
		sensor = (UltrasonicSensor)startPeer("sensor");
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		Runtime.createAndStart("gui", "GUIService");
		//Runtime.createAndStart("webgui", "WebGUI");
		/*
		Serial.createNullModemCable("uart", "COM10");
		Serial serial = (Serial)Runtime.createAndStart("uart", "Serial");

		serial.connect("uart");
		*/
		
		Pingdar pingdar = (Pingdar) Runtime.start("pingdar", "Pingdar");
		pingdar.test();

		//Runtime.createAndStart("gui", "GUIService");

		//pingdar.attach("COM15", 7, 8, 9);
		//pingdar.sweep();
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

}
