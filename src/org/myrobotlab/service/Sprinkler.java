package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * Sprinkler - This service waters Grogs front,back, and garden.
 * 
 */
public class Sprinkler extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sprinkler.class);

	WebGui webgui;
	Arduino arduino;
	Cron cron;

	String defaultPort = "/dev/ttyACM0";

	// TODO - memory appender
	ArrayList<String> history = new ArrayList<String>();

	public static void main(String args[]) throws InterruptedException, IOException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		Runtime.start("sprinkler", "Sprinkler");
	}

	public Sprinkler(String n) {
		super(n);
	}

	public void connect() throws IOException {
		arduino = (Arduino) startPeer("arduino");
		arduino.connect(defaultPort);
	}

	public void connect(String port) throws IOException {
		defaultPort = port;
		arduino.connect(defaultPort);
	}

	public ArrayList<String> getHistory() {
		return history;
	}

	public ArrayList<org.myrobotlab.service.Cron.Task> getCronTasks() {
		return cron.getCronTasks();
	}

	// TODO - fix add length of watering
	public void onTimeToWater() {
		log.info("onTimeToWater");
		history.add(String.format("onTimeToWater %s", new Date().toString()));
		arduino.digitalWrite(2, 0);
		arduino.digitalWrite(3, 0);
		arduino.digitalWrite(4, 0);
		arduino.digitalWrite(5, 0);
		arduino.digitalWrite(6, 0);
		arduino.digitalWrite(7, 0);
		arduino.digitalWrite(8, 0);
		arduino.digitalWrite(9, 0);
		arduino.digitalWrite(10, 0);
		arduino.digitalWrite(11, 0);
		arduino.digitalWrite(12, 0);
		arduino.digitalWrite(13, 0);

	}

	public void waterFront() {
		log.info("waterFront");
		history.add(String.format("onTimeToWater %s", new Date().toString()));
		arduino.digitalWrite(2, 1);
		arduino.digitalWrite(3, 1);
		arduino.digitalWrite(4, 1);
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 0);
		arduino.digitalWrite(7, 1);
		arduino.digitalWrite(8, 1);
		arduino.digitalWrite(9, 1);
		arduino.digitalWrite(10, 1);
		arduino.digitalWrite(11, 1);
		arduino.digitalWrite(12, 1);
		arduino.digitalWrite(13, 1);
	}

	public void waterBack() {
		log.info("waterBack");
		history.add(String.format("onTimeToWater %s", new Date().toString()));
		arduino.digitalWrite(2, 1);
		arduino.digitalWrite(3, 1);
		arduino.digitalWrite(4, 1);
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 1);
		arduino.digitalWrite(7, 0);
		arduino.digitalWrite(8, 0);
		arduino.digitalWrite(9, 0);
		arduino.digitalWrite(10, 0);
		arduino.digitalWrite(11, 0);
		arduino.digitalWrite(12, 0);
		arduino.digitalWrite(13, 0);
	}

	public void waterGarden() {
		log.info("waterBack");
		history.add(String.format("onTimeToWater %s", new Date().toString()));
		arduino.digitalWrite(2, 1);
		arduino.digitalWrite(3, 1);
		arduino.digitalWrite(4, 1);
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 1);
		arduino.digitalWrite(7, 1);
		arduino.digitalWrite(8, 1);
		arduino.digitalWrite(9, 1);
		arduino.digitalWrite(10, 1);
		arduino.digitalWrite(11, 1);
		arduino.digitalWrite(12, 1);
		arduino.digitalWrite(13, 0);
	}

	@Override
	public void startService() {

		super.startService();
		arduino = (Arduino) startPeer("arduino");
		try {
			connect();
		} catch (Exception e) {
			log.error("starting sprinkler threw", e);
		}
		stop();
		// FIXME - custom MRLComm.ino build to start with all digital pins =
		// 1
		// HIGH
		// for the funky stinky nature of the relay board
		cron = (Cron) startPeer("cron");

		// FIXME - start schedule
		// every 3 days
		// cron.addTask("0 6 */3 * *", this.getName(), "onTimeToWater");
		cron.addTask("0 6 */3 * *", this.getName(), "waterFront");
		cron.addTask("30 6 */3 * *", this.getName(), "waterBack");
		cron.addTask("0 7 */3 * *", this.getName(), "waterGarden");
		cron.addTask("29 * * * *", this.getName(), "stop");
		cron.addTask("59 * * * *", this.getName(), "stop");

		// cron.addTask("* * * * *", this.getName(), "onTimeToWater");
		// cron.addTask("*/2 * * * *", this.getName(), "stop");

		webgui = (WebGui) startPeer("webgui");

	}

	public void stop() {
		log.info("stop");
		history.add(String.format("stop %s", new Date().toString()));
		arduino.digitalWrite(2, 1);
		arduino.digitalWrite(3, 1);
		arduino.digitalWrite(4, 1);
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 1);
		arduino.digitalWrite(7, 1);
		arduino.digitalWrite(8, 1);
		arduino.digitalWrite(9, 1);
		arduino.digitalWrite(10, 1);
		arduino.digitalWrite(11, 1);
		arduino.digitalWrite(12, 1);
		arduino.digitalWrite(13, 1);
	}

	@Override
	public void stopService() {
		if (arduino != null) {
			arduino.disconnect();
			arduino.stopService();
		}
	}

	public void on(int pin, int minutes) {

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

		ServiceType meta = new ServiceType(Sprinkler.class.getCanonicalName());
		meta.addDescription("sprinkler system");
		meta.addCategory("control", "home automation");
		meta.addPeer("arduino", "Arduino", "Arduino for relay control");
		meta.addPeer("webgui", "WebGui", "web interface");
		meta.addPeer("cron", "Cron", "scheduler for sprinklers");
		return meta;
	}
}
