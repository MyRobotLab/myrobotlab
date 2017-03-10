package org.myrobotlab.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.TooManyListenersException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

/**
 * 
 * Plantoid - Unknown state
 * 
 * A plantoid is a hypothetical robot or synthetic organism designed to look,
 * act and grow like a plant. The concept was first scientifically published in
 * 2010 More info at : http://www.plantoidrobotics.org/
 * http://myrobotlab.org/service/plantoid
 * 
 */
public class Plantoid extends Service {
	// video0 = rgbpilot cam
	// video1 = pink plant static NIR -
	// Imaged from there should be taken and put through the infrapix, then
	// opencv
	// Nope static camera view of the braaaains
	// video2 = NIR pilot cam
	public class Scanner extends Thread {
		int start = 0;
		int end = 180;
		int delay = 400;
		boolean isScanning = false;
		int pos = start;
		int increment = 1;
		Servo servo;

		public Scanner(Servo servo, int start, int end, int delay) {
			this.servo = servo;
			this.start = start;
			this.end = end;
			this.delay = delay;
			this.pos = start;
		}

		@Override
		public void run() {
			isScanning = true;
			while (isScanning) {
				servo.moveTo(pos);
				pos = pos + increment;
				Service.sleep(delay);
				if (pos >= end || pos <= start) {
					increment = increment * -1;
				}
			}

		}

	}

	class SendReport extends TimerTask {

		Plantoid plantoid;

		SendReport(Plantoid plantoid) {
			this.plantoid = plantoid;
		}

		@Override
		public void run() {
			sendReport();
		}

	}

	public final static Logger log = LoggerFactory.getLogger(Plantoid.class);

	private static final long serialVersionUID = 1L;

	transient private Arduino arduino;
	// transient private AudioFile audioFile;
	// transient private JFugue jFugue;
	// transient private Keyboard keyboard;
	transient private OpenCV opencv;
	transient private Servo leg1, leg2, leg3, leg4, pan, tilt;
	// transient private SpeechSynthesis speech;
	transient private Tracking tracking;
	transient private VideoStreamer streamer;

	// FIXME make part of ServoControl

	// transient private WebGui webgui;

	transient private Xmpp xmpp;

	transient public Scanner scanner = null;

	int everyNHours = 8;

	transient HashMap<String, Object> p = new HashMap<String, Object>();
	public String port = "/dev/ttyACM0";
	/**
	 * analog read pins
	 */
	public final int soildMoisture = 0;
	public final int tempHumidity = 2;
	public final int leftLight = 4;

	public final int rightLight = 6;

	public final int airQuality = 10;

	private int sampleRate = 8000;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Plantoid plantoid = (Plantoid) Runtime.create("plantoid", "Plantoid");
			plantoid.connect("COM12");
			plantoid.startService();
			// Runtime.createAndStart("python", "Python");
			// Runtime.createAndStart("webgui", "WebGui");
			/*
			 * SwingGui gui = new SwingGui("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * Plantoid Service - this service controls all peer services. It is a
	 * OrbousMundus Genus and flagship of the BEPSL Republic.
	 * 
	 * Its mission is to go forth explore and be one with nature in alien
	 * environments while reporting telemetry back to BEPSL control
	 * 
	 * @param n
	 */
	public Plantoid(String n) {
		super(n);

		arduino = (Arduino) createPeer("arduino");
		xmpp = (Xmpp) createPeer("xmpp");
		// webgui = (WebGui) createPeer("webgui");
		leg1 = (Servo) createPeer("leg1");
		leg2 = (Servo) createPeer("leg2");
		leg3 = (Servo) createPeer("leg3");
		leg4 = (Servo) createPeer("leg4");
		pan = (Servo) createPeer("pan");
		tilt = (Servo) createPeer("tilt");
		opencv = (OpenCV) createPeer("opencv");
		tracking = (Tracking) createPeer("tracking");

		// OLD WAY
		/*
		 * leg1.setPin(2); leg2.setPin(3); leg3.setPin(4); leg4.setPin(5);
		 * 
		 * pan.setPin(6); tilt.setPin(7);
		 * 
		 * leg1.setController(arduino); leg2.setController(arduino);
		 * leg3.setController(arduino); leg4.setController(arduino);
		 * 
		 * pan.setController(arduino); tilt.setController(arduino);
		 * 
		 */
		arduino.servoAttachPin(leg1, 2);
		arduino.servoAttachPin(leg2, 3);
		arduino.servoAttachPin(leg3, 4);
		arduino.servoAttachPin(leg4, 5);
		arduino.servoAttachPin(pan, 6);
		arduino.servoAttachPin(tilt, 7);

		pan.setRest(90);
		tilt.setRest(90);

		leg1.setRest(90);
		leg2.setRest(90);
		leg3.setRest(90);
		leg4.setRest(90);

		streamer = (VideoStreamer) createPeer("streamer");

	}

	/**
	 * attaches the legs only
	 */
	public void attachLegs() {
		leg1.attach();
		leg2.attach();
		leg3.attach();
		leg4.attach();
	}

	// public int scaleUp scaleDown

	/**
	 * attaches only the pan tilt
	 */
	public void attachPanTilt() {
		pan.attach();
		tilt.attach();
	}

	/**
	 * attaches all the servos legs and pan tilt kit
	 */
	public void attachServos() {
		attachPanTilt();
		attachLegs();
	}

	public boolean connect() throws IOException {
		arduino.connect(port);
		arduino.broadcastState();
		return arduino.isConnected();
	}

	/**
	 * Connects the plantoid server's Arduino service to the appropriate serial
	 * port. This is automatically called when the Plantoid service starts.
	 * Default is /dev/ttyACM0
	 * 
	 * @param port
	 * @return true if connected false otherwise
	 * @throws TooManyListenersException
	 * @throws IOException
	 */
	public boolean connect(String port) throws IOException {
		this.port = port;
		return connect();
	}

	/**
	 * detaches the legs only
	 */
	public void detachLegs() {
		leg1.detach();
		leg2.detach();
		leg3.detach();
		leg4.detach();
	}

	// ------- servos begin -----------

	/**
	 * detaches the pan tilt only
	 */
	public void detachPanTilt() {
		pan.detach();
		tilt.detach();
	}

	/**
	 * detaches all servos
	 */
	public void detachServos() {
		detachPanTilt();
		detachLegs();
	}

	/**
	 * current uptime of the plantoid server this represents the longevity and
	 * quality of our plantoid craft LONG LIVE BEPSL !!
	 * 
	 * @return the uptime
	 */
	public String getUptime() {
		return Runtime.getUptime();
	}

	public void initTelemetryPayload() {
		p.put("soildMoistureCurrent", 0);
		p.put("soildMoistureMin", 0);
		p.put("soildMoistureMax", 0);
		p.put("soildMoistureAvg", 0);

		p.put("tempHumidityCurrent", 0);
		p.put("tempHumidityMin", 0);
		p.put("tempHumidityMax", 0);
		p.put("tempHumidityAvg", 0);

		p.put("soildMoistureCurrent", 0);
		p.put("soildMoistureMin", 0);
		p.put("soildMoistureMax", 0);
		p.put("soildMoistureAvg", 0);

		p.put("soildMoistureCurrent", 0);
		p.put("soildMoistureMin", 0);
		p.put("soildMoistureMax", 0);
		p.put("soildMoistureAvg", 0);

		p.put("soildMoistureCurrent", 0);
		p.put("soildMoistureMin", 0);
		p.put("soildMoistureMax", 0);
		p.put("soildMoistureAvg", 0);
	}

	/**
	 * moveX moves the plantoid on the X axis
	 * 
	 * @param power
	 *            -90 - down the X axis 0 (stop) 90 - up the X axis
	 */
	public void moveX(Integer power) {
		leg1.moveTo(90 - power);
		leg2.moveTo(90);
		leg3.moveTo(90 + power);
		leg4.moveTo(90);
	}

	/**
	 * moveY moves the plantoid on the Y axis
	 * 
	 * @param power
	 *            -90 - down the Y axis 0 (stop) 90 - up the Y axis
	 */
	public void moveY(Integer power) {
		leg2.moveTo(90);
		leg3.moveTo(90 - power);
		leg4.moveTo(90);
		leg1.moveTo(90 + power);
	}

	public void onPin(Pin pin) {
		// if (log.isDebugEnabled())
		{
			log.info(String.format("pin %d value %d", pin.pin, pin.value));
		}
		/*
		 * switch(pin.pin) { case soildMoisture: //p.put("soildMoistureCurrent",
		 * value) break; }
		 */
	}

	public String sendReport() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("report from orbous on the Idahosian landing site, I am still alive after %s- all is well - *HAIL BEPSL* !", Runtime.getUptime()));
		xmpp.broadcast(sb.toString());
		return sb.toString();
	}

	// ------- servos begin -----------

	// FIXME FIXME FIXME
	// setScannerDelay(int delay) - can not be found (must upcast) - so null is
	// found ???
	public int setScannerDelay(Integer delay) {
		if (scanner != null) {
			scanner.delay = delay;
			return delay;
		}

		return -1;
	}

	/**
	 * shuts down the planoid server
	 */
	public void shutdown() {
		if (xmpp != null) {
			xmpp.broadcast("MY LIFE FOR BEPSL !");
		}
		detachServos();
		Runtime.releaseAll();
	}

	/**
	 * Spin spins the plantoid server
	 * 
	 * @param power
	 *            power range is from -90 (spin full clock wise) 0 (stop) 90
	 *            (spin full counter clockwise)
	 */
	public void spin(Integer power) {
		int s = 90 - power;
		leg2.moveTo(s);
		leg3.moveTo(s);
		leg4.moveTo(s);
		leg1.moveTo(s);
	}

	/**
	 * square dance "should" make a square by moving the plantoid up and down
	 * the X and Y axis's
	 * 
	 * @param power
	 *            power applied to legs
	 * @param time
	 *            run time on each axis
	 */
	public void squareDance(Integer power, Integer time) {
		int s = 90 - power;
		moveX(s);
		sleep(time);
		moveY(s);
		sleep(time);
		moveX(-s);
		sleep(time);
		moveX(-s);
		sleep(time);
		stop();
	}

	public void startCannyScanning() {
		opencv.addFilter("Canny");
		opencv.setDisplayFilter("Canny");
		opencv.capture();
		startScanning();
	}

	public void startHSVScanning() {
		opencv.removeFilters();
		opencv.addFilter("PyramidDown");
		opencv.addFilter("HSV");
		opencv.setDisplayFilter("HSV");
		opencv.capture();
		startScanning();
	}

	/**
	 * This begins polling of the various analog senesors of the Plantoid
	 * server. It is automatically started when the Plantoid service is started.
	 * Soil, temperature, left and right light sensors and air quality are all
	 * polled
	 */
	public void startPolling() {
		// arduino.setSampleRate(sampleRate);
		arduino.enablePin(soildMoisture);
		arduino.enablePin(tempHumidity);
		arduino.enablePin(leftLight);
		arduino.enablePin(rightLight);
		arduino.enablePin(airQuality);
	}

	public void startScanning() {
		startScanning(pan);
	}

	public void startScanning(Servo servo) {
		if (scanner != null) {
			stopScanning();
		}
		scanner = new Scanner(servo, 10, 160, 300);
		scanner.start();
	}

	public void startScanning(String name, int begin, int end, int delay) {
		Servo servo = (Servo) Runtime.getService(name);
		if (scanner != null) {
			stopScanning();
		}
		scanner = new Scanner(servo, 10, 160, 300);
		scanner.start();
	}

	// FIXME FIXME FIXME - Service.isValidForStart() !!!!
	@Override
	public void startService() {
		try {
			super.startService();
			xmpp.startService();

			xmpp.connect("talk.google.com", 5222, "orbous@myrobotlab.org", "mrlRocks!");

			// gets all users it can send messages to
			xmpp.setStatus(true, String.format("online all the time - %s", new Date()));
			// xmpp.addAuditor("incubator incubator");
			// xmpp.addAuditor("David Ultis");
			// xmpp.addAuditor("Greg Perry");
			// xmpp.broadcast("reporting for duty *SIR* !");

			if (tracking != null) {
				tracking.startService();
			}

			arduino.connect(port);
			// the BEPSL report
			// timer.scheduleAtFixedRate(new SendReport(this), 0, 1000 * 60 * 60
			// *
			// everyNHours);

			leg1.attach();
			leg2.attach();
			leg3.attach();
			leg4.attach();
			pan.attach();
			tilt.attach();

			subscribe(arduino.getName(), "publishPin");

			// startPolling();
			attachPanTilt();

			tracking.startService();
			opencv.startService();

			// attachServos();
			detachLegs(); // at the moment detach legs
			// stop();
			streamer.startService();
			streamer.attach(opencv);

		} catch (Exception e) {
			log.error("starting plantoid error", e);
		}

	}

	public void startVisualScanning() {
		opencv.addFilter("PyramidDown");
		opencv.setDisplayFilter("PyramidDown");
		opencv.removeFilters();
		opencv.capture();
		startScanning();
	}

	/**
	 * stops all legs
	 */
	public void stop() {
		leg1.moveTo(90);
		leg2.moveTo(90);
		leg3.moveTo(90);
		leg4.moveTo(90);
	}

	/**
	 * shut down polling of analog sensors
	 */
	public void stopPolling() {
		arduino.disablePin(soildMoisture);
		arduino.enablePin(tempHumidity);
		arduino.enablePin(leftLight);
		arduino.enablePin(rightLight);
		arduino.enablePin(airQuality);
	}

	public void stopScanning() {
		if (scanner != null) {
			scanner.isScanning = false;
			scanner.interrupt();
			scanner = null;
		}
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

		ServiceType meta = new ServiceType(Plantoid.class.getCanonicalName());
		meta.addDescription("The Plantoid Service");
		meta.addCategory("robot");
		// put peer definitions in
		meta.addPeer("arduino", "Arduino", "arduino service");
		meta.addPeer("audioFile", "AudioFile", "audio file service");
		meta.addPeer("jFugue", "JFugue", "jfugue service");
		// TODO: removed this, why is webgui a peer here?
		// meta.addPeer("webgui", "WebGui", "WebGui service");
		meta.addPeer("xmpp", "Xmpp", "Xmpp service");
		meta.addPeer("leg1", "Servo", "leg1");
		meta.addPeer("leg2", "Servo", "leg2");
		meta.addPeer("leg3", "Servo", "leg3");
		meta.addPeer("leg4", "Servo", "leg4");
		meta.addPeer("pan", "Servo", "pan");
		meta.addPeer("tilt", "Servo", "tilt");
		meta.addPeer("tracking", "Tracking", "tracking service");
		meta.addPeer("opencv", "OpenCV", "pilot camera");
		meta.addPeer("streamer", "VideoStreamer", "video streamer");

		return meta;
	}

}
