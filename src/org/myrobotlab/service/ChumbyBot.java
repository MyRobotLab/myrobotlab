package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.Trigger;
import org.slf4j.Logger;

public class ChumbyBot extends Service {

	public abstract class Behavior implements Runnable {

	}

	public class Explore extends Behavior {
		@Override
		public void run() {
			try {

				// execute non Runnable
				// start forward - some speed - DifferentialDrive?
				// no encoder - ? start timer ?
				right.move(0.5f);
				left.move(0.5f);

				Trigger alert = new Trigger();
				alert.threshold = 600;
				alert.pinData.pin = 0;
				sensors.addTrigger(alert);

				// wait on IR Event
				synchronized (lock) {
					lock.wait();
				}

				// stop
				right.stop();
				left.stop();

				// say something relevant e.g. "wall @ 25 cm"
				speech.speak("Excuse me. I believe something is in my way");

				// check left
				servo.moveTo(20);
				Thread.sleep(300);
				// wait for servo to stop
				// check IR -
				int leftRange = sensors.getLastValue(arduino.getName(), 0);

				// check right
				servo.moveTo(160);
				Thread.sleep(300);

				int rightRange = sensors.getLastValue(arduino.getName(), 0);

				// if both values are under - must backup or rotate base right

				if (rightRange > leftRange) {
					speech.speak("moving right");
					left.move(0.3f);
					Thread.sleep(400);

				} else {
					speech.speak("moving left");
					right.move(0.3f);
					Thread.sleep(400);
				}

				servo.moveTo(90);
				speech.speak("checking forward range");
				Thread.sleep(300);
				int forward = sensors.getLastValue(arduino.getName(), 0);
				if (forward < 100) {
					speech.speak("forward");
				} else {
					speech.speak("not safe to go forward");
				}

				while (true) {
					/*
					 * Thread.sleep(1000); servo.moveTo(10); Thread.sleep(4000);
					 * servo.moveTo(90); Thread.sleep(4000); servo.moveTo(170);
					 * Thread.sleep(4000); servo.moveTo(90);
					 */
					Thread.sleep(1000);

				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logException(e);
			}

		}

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ChumbyBot.class.getCanonicalName());
	OpenCV camera = new OpenCV("camera");
	Servo servo = new Servo("pan");
	Arduino arduino = new Arduino("uBotino");
	SensorMonitor sensors = new SensorMonitor("sensors");
	RemoteAdapter remote = new RemoteAdapter("remote");
	Speech speech = new Speech("speech");

	Motor left = new Motor("left");

	Motor right = new Motor("right");

	transient Thread behavior = null;

	private final Object lock = new Object();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			ChumbyBot chumbybot = new ChumbyBot("chumbybot");
			chumbybot.startService();
			chumbybot.startBot();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public ChumbyBot(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void publishPinAlert(Trigger alert) {
		synchronized (lock) {
			lock.notifyAll();
		}

	}

	public void startBot() throws Exception {
		speech.startService();
		// speech.cfg.set("isATT", true);
		// speech.speak("I am about to start");
		remote.startService();
		camera.startService();
		arduino.startService();
		sensors.startService();
		servo.startService();

		arduino.connect("/dev/ttyUSB0");

		// arduino to sensor monitor
		arduino.addListener("publishPin", sensors.getName(), "sensorInput", Pin.class);

		// sensor monitor to chumbybot
		sensors.addListener("publishPinAlert", this.getName(), "publishPinAlert", Trigger.class);

		arduino.analogReadPollingStart(0);

		behavior = new Thread(new ChumbyBot.Explore(), "behavior");
		behavior.start();
	}

}
