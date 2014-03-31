package org.myrobotlab.service;

import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;


// video0 = rgbpilot cam
// video1 = pink plant static NIR - 
// Imaged from there should be taken and put through the infrapix, then opencv Nope static camera view of the braaaains
// video2 = NIR pilot cam

public class Plantoid extends Service {
	private static final long serialVersionUID = 1L;

	transient private Arduino arduino;
	transient private AudioFile audioFile;
	transient private JFugue jFugue;
	transient private Keyboard keyboard;
	transient private OpenCV opencv;
	transient private Servo leg1, leg2, leg3, leg4, pan, tilt;
	transient private Speech speech;
	transient private Tracking tracking;
	transient private VideoStreamer streamer;
	transient private WebGUI webgui;
	transient private XMPP xmpp;
	
	// FIXME make part of ServoControl
	
	public Scanner scanner = null;
	
	int everyNHours = 8;
	
	HashMap<String, Object> p = new HashMap<String, Object>();
	
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

	public final static Logger log = LoggerFactory.getLogger(Plantoid.class.getCanonicalName());
	
	class SendReport extends TimerTask {

		Plantoid plantoid;
		
		SendReport(Plantoid plantoid)
		{
			this.plantoid = plantoid;
		}
		@Override
		public void run() {
			sendReport();
		}
		
	}
	
	public class Scanner extends Thread
	{
		int start = 0;
		int end = 180;
		int delay = 400;
		boolean isScanning = false;
		int pos = start;
		int increment = 1;
		Servo servo;
		
		public Scanner(Servo servo, int start, int end, int delay)
		{
			this.servo = servo;
			this.start = start;
			this.end = end;
			this.delay = delay;
			this.pos = start;
		}
		
		public void run() {
			isScanning = true;
			while(isScanning) {
				servo.moveTo(pos);
				pos = pos + increment;
				Service.sleep(delay);
				if (pos >= end || pos <= start){
					increment = increment * -1;
				}
			}
			
			
		}
		
	}
	
	public void startHSVScanning(){
		opencv.removeFilters();
		opencv.addFilter("PyramidDown");
		opencv.addFilter("HSV");
		opencv.setDisplayFilter("HSV");
		opencv.capture();
		startScanning();
	}
	
	public void startVisualScanning(){
		opencv.addFilter("PyramidDown");
		opencv.setDisplayFilter("PyramidDown");
		opencv.removeFilters();
		opencv.capture();
		startScanning();
	}
	
	public void startCannyScanning(){
		opencv.addFilter("Canny");
		opencv.setDisplayFilter("Canny");
		opencv.capture();
		startScanning();
	}
	
	//public int scaleUp scaleDown
	
	public void startScanning() {
		startScanning(pan);
	}
	
	public void startScanning(Servo servo) {
		if (scanner != null){
			stopScanning();
		}
		scanner = new Scanner(servo, 10, 160, 300);
		scanner.start();
	}
	
	public void startScanning(String name, int begin, int end, int delay) {
		Servo servo = (Servo)Runtime.getService(name);
		if (scanner != null){
			stopScanning();
		}
		scanner = new Scanner(servo, 10, 160, 300);
		scanner.start();
	}
	
	// FIXME FIXME FIXME
	// setScannerDelay(int delay) - can not be found (must upcast) - so null is found ???
	public int setScannerDelay(Integer delay)
	{
		if (scanner != null)
		{
			scanner.delay = delay;
			return delay;
		}
		
		return -1;
	}
	
	public void stopScanning() {
		if (scanner != null)
		{
			scanner.isScanning = false;
			scanner.interrupt();
			scanner = null;
		}
	}
	
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		
		// merge
		peers.suggestAs("tracking.x", "pan", "Servo", "shared x");
		peers.suggestAs("tracking.y", "tilt", "Servo", "shared y");
		peers.suggestAs("tracking.opencv", "opencv", "OpenCV", "shared opencv");
	
		peers.put("arduino", "Arduino", "arduino service");
		peers.put("audioFile", "AudioFile", "audio file service");
		peers.put("jFugue", "JFugue", "jfugue service");
		
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("xmpp", "XMPP", "xmpp service");
		peers.put("leg1", "Servo", "leg1");
		peers.put("leg2", "Servo", "leg2");
		peers.put("leg3", "Servo", "leg3");
		peers.put("leg4", "Servo", "leg4");
		peers.put("pan",  "Servo", "pan");
		peers.put("tilt", "Servo", "tilt");
		peers.put("tracking", "Tracking", "tracking service");
		peers.put("opencv", "OpenCV", "pilot camera");
		peers.put("streamer", "VideoStreamer", "video streamer");
		
		return peers;
	}

	/**
	 * Plantoid Service - this service controls all peer services.  It is a OrbousMundus Genus and
	 * flagship of the BEPSL Republic.
	 * 
	 *  Its mission is to go forth explore and be one with nature in alien environments while reporting
	 *  telemetry back to BEPSL control
	 * @param n
	 */
	public Plantoid(String n) {
		super(n);

		arduino = (Arduino) createPeer("arduino");
		xmpp = (XMPP) createPeer("xmpp");
		webgui = (WebGUI) createPeer("webgui");
		leg1 = (Servo) createPeer("leg1");
		leg2 = (Servo) createPeer("leg2");
		leg3 = (Servo) createPeer("leg3");
		leg4 = (Servo) createPeer("leg4");
		pan = (Servo) createPeer("pan");
		tilt = (Servo) createPeer("tilt");
		opencv = (OpenCV)createPeer("opencv");
		tracking = (Tracking) createPeer("tracking");
		
		leg1.setPin(2);
		leg2.setPin(3);
		leg3.setPin(4);
		leg4.setPin(5);
		
		pan.setPin(6);
		tilt.setPin(7);
		
		leg1.setController(arduino);
		leg2.setController(arduino);
		leg3.setController(arduino);
		leg4.setController(arduino);

		pan.setController(arduino);
		tilt.setController(arduino);

		pan.setRest(90);
		tilt.setRest(90);
		
		leg1.setRest(90);
		leg2.setRest(90);
		leg3.setRest(90);
		leg4.setRest(90);
		
		streamer = (VideoStreamer) createPeer("streamer");

	}

	public String sendReport()
	{	
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("report from orbous on the Idahosian landing site, I am still alive after %s- all is well - *HAIL BEPSL* !", Runtime.getUptime()));
		xmpp.broadcast(sb.toString());
		return sb.toString();
	}
	 
	@Override
	public String getDescription() {
		return "the plantoid service";
	}

	// FIXME FIXME FIXME -  Service.isValidForStart() !!!!
	@Override
	public void startService() {
		super.startService();

		try {
			
			xmpp.startService();
			xmpp.connect("talk.google.com", 5222, "orbous@myrobotlab.org", "mrlRocks!");
			// gets all users it can send messages to
			xmpp.setStatus(true, String.format("online all the time - %s", new Date()));
			xmpp.addAuditor("incubator incubator");
			xmpp.addAuditor("David Ultis");
			xmpp.addAuditor("Greg Perry");
			xmpp.broadcast("reporting for duty *SIR* !");

			if (tracking != null){
				tracking.startService();
			}
			
			arduino.connect(port);
			// the BEPSL report
			//timer.scheduleAtFixedRate(new SendReport(this), 0, 1000 * 60 * 60 * everyNHours);

			leg1.attach();
			leg2.attach();
			leg3.attach();
			leg4.attach();
			pan.attach();
			tilt.attach();
			
			arduino.addListener(getName(), "publishPin");
			
			
//			startPolling();
			attachPanTilt();
			
			tracking.startService();
			opencv.startService();
			
//			attachServos();
			detachLegs(); // at the moment detach legs
//			stop();
			streamer.startService();
			streamer.attach(opencv);
			
		} catch (Exception e) {
			error(e);
		}
	}
	
	@Override
	public void stopService() {
		super.stopService();
		// nice !
		Runtime.releaseAll();
	}

	public void initTelemetryPayload()
	{
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
	
	public void publishPin(Pin pin)
	{
		//if (log.isDebugEnabled())
		{
			log.info(String.format("pin %d value %d", pin.pin, pin.value));
		}
		/*
		switch(pin.pin)
		{
		case soildMoisture:
			//p.put("soildMoistureCurrent", value)
			break;
		}
		*/
	}
	
	/**
	 * Connects the plantoid server's Arduino service to the appropriate serial port.
	 * This is automatically called when the Plantoid service starts.
	 * Default is /dev/ttyACM0
	 * @param port
	 * @return
	 * true if connected false otherwise
	 */
	public boolean connect(String port) {
		this.port = port;
		return connect();
	}
	
	public boolean connect() {
		arduino.connect(port);
		arduino.broadcastState();
		return arduino.isConnected();
	}
	/**
	 * This begins polling of the various analog senesors of the 
	 * Plantoid server.  It is automatically started when the Plantoid
	 * service is started. Soil, temperature, left and right light sensors
	 * and air quality are all polled
	 */
	public void startPolling()
	{
		arduino.setSampleRate(sampleRate);
		arduino.analogReadPollingStart(soildMoisture);
		arduino.analogReadPollingStart(tempHumidity);
		arduino.analogReadPollingStart(leftLight);
		arduino.analogReadPollingStart(rightLight);
		arduino.analogReadPollingStart(airQuality);
	}
	
	/**
	 * shut down polling of analog sensors
	 */
	public void stopPolling(){
		arduino.analogReadPollingStop(soildMoisture);
		arduino.analogReadPollingStop(tempHumidity);
		arduino.analogReadPollingStop(leftLight);
		arduino.analogReadPollingStop(rightLight);
		arduino.analogReadPollingStop(airQuality);
	}

	// ------- servos begin -----------
	
	/**
	 * Spin spins the plantoid server
	 * @param power
	 * power range is from 
	 *         -90 (spin full clock wise)
	 *         0 (stop)
	 *         90 (spin full counter clockwise)
	 */
	public void spin(Integer power) {
		int s = 90 - power;
		leg2.moveTo(s);
		leg3.moveTo(s);
		leg4.moveTo(s);
		leg1.moveTo(s);
	}
	
	/**
	 * moveY moves the plantoid on the Y axis
	 * @param power
	 *         -90 - down the Y axis
	 *         0 (stop)
	 *         90 - up the Y axis
	 */
	public void moveY(Integer power){
		leg2.moveTo(90);
		leg3.moveTo(90 - power);
		leg4.moveTo(90);
		leg1.moveTo(90 + power);
	}

	/**
	 * moveX moves the plantoid on the X axis
	 * @param power
	 *         -90 - down the X axis
	 *         0 (stop)
	 *         90 - up the X axis
	 */	public void moveX(Integer power){
		leg1.moveTo(90 - power);
		leg2.moveTo(90);
		leg3.moveTo(90 + power);
		leg4.moveTo(90);
	}
	
	/**
	 * square dance "should" make a square by moving the plantoid
	 * up and down the X and Y axis's
	 * @param power
	 * power applied to legs
	 * @param time
	 * run time on each axis
	 */
	public void squareDance(Integer power, Integer time)
	{
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
	 * attaches all the servos 
	 * legs and pan tilt kit
	 */
	public void attachServos()
	{
		attachPanTilt();
		attachLegs();
	}
	
	/**
	 * detaches all servos
	 */
	public void detachServos()
	{
		detachPanTilt();
		detachLegs();
	}
	
	/**
	 * attaches only the pan tilt
	 */
	public void attachPanTilt() {
		pan.attach();
		tilt.attach();
	}	
	
	/**
	 * detaches the pan tilt only
	 */
	public void detachPanTilt() {
		pan.detach();
		tilt.detach();
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
	 * shuts down the planoid server
	 */
	public void shutdown() {
		if (xmpp != null)
		{
			xmpp.broadcast("MY LIFE FOR BEPSL !");
		}
		detachServos();
		Runtime.releaseAll();
	}
	
	/**
	 * current uptime of the plantoid server
	 * this represents the longevity and quality
	 * of our plantoid craft
	 * LONG LIVE BEPSL !!
	 * @return
	 * the uptime
	 */
	public String getUptime()
	{
		return Runtime.getUptime();
	}



	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Plantoid plantoid = (Plantoid) Runtime.create("plantoid", "Plantoid");
		plantoid.connect("COM12");
		plantoid.startService();
		//Runtime.createAndStart("python", "Python");
		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}

}
