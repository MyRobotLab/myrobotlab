package org.myrobotlab.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.RangeListener;
import org.myrobotlab.service.interfaces.SensorDataSink;
import org.slf4j.Logger;

/**
 * 
 * UltrasonicSensor - This will read data from an ultrasonic sensor module
 * connected to an android.
 *
 */
public class UltrasonicSensor extends Service implements RangeListener, SensorDataSink {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);


	public final Set<String> types = new HashSet<String>(Arrays.asList("SR04"));
	private int pings;
	//private int max;

	//private int min;
	//private int sampleRate;

	// TODO - avg ?

	//private int sensorMinCM;
	//private int sensorMaxCM;
	private Integer trigPin = null;
	private Integer echoPin = null;
	private String type = "SR04";

	private Integer lastRaw;
	private Integer lastRange;

	// for blocking asynchronous data
	private boolean isBlocking = false;

	transient private BlockingQueue<Integer> data = new LinkedBlockingQueue<Integer>();

	private transient Arduino controller;

	String controllerName;

	public UltrasonicSensor(String n) {
		super(n);
	}

	// ---- part of interfaces begin -----

	// Uber good - .. although this is "chained" versus star routing
	// Star routing would be routing from the Arduino directly to the Listener
	// The "chained" version takes 2 thread contexts :( .. but it has the
	// benefit
	// of the "publishRange" method being affected by the Sensor service e.g.
	// change units, sample rate, etc
	public void addRangeListener(Service service) {
		addListener("publishRange", service.getName(), "onRange");
	}

	public void attach(String port, int pin) throws IOException {
		attach(port, pin, pin);
	}

	public boolean attach(String port, int trigPin, int echoPin) throws IOException {
		controller = (Arduino) startPeer("controller");
		controllerName = controller.getName();
		this.trigPin = trigPin;
		this.echoPin = echoPin;
		this.controller.connect(port); // THIS BETTER BLOCK UNTIL READY !
		return controller.sensorAttach(this);
	}


	// FIXME - should be MicroController Interface ..
	public Arduino getController() {
		return controller;
	}

	/**
	 * method for the controller to get the data type we want
	 */
	@Override
	public int getDataSinkType() {
		// we want an Integer
		//return Integer.class.getCanonicalName();
		return DATA_SINK_TYPE_INTEGER;
	}


	public int getEchoPin() {
		return echoPin;
	}

	public int getTriggerPin() {
		return trigPin;
	}

	@Override
	public void onRange(Long range) {
		log.info(String.format("RANGE: %d", range));
	}

	/* FIXME !!! IMPORTANT PUT IN INTERFACE & REMOVE SELF FROM ARDUINO !!! */
	public Integer publishRange(Integer duration) {
		
		++pings;

		lastRange = duration / 58;

		log.info("publishRange {}", lastRange);
		return lastRange;
	}

	public int range() {
		return range(10);
	}

	public Integer range(int timeout) {

		Integer ret = null;

		try {
			data.clear();
			startRanging(timeout);
			// sendMsg(GET_VERSION);
			ret = data.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			Logging.logError(e);
		}
		data.clear(); // double tap
		return ret;// controller.pulseIn(trigPin, echoPin, timeout);
	}

	public boolean setType(String type) {
		if (types.contains(type)) {
			this.type = type;
			return true;
		}
		return false;
	}

	// ---- part of interfaces end -----

	public void startRanging() {
		startRanging(10); // 10000 uS = 10 ms
	}

	public void startRanging(int timeoutMS) {
		controller.sensorPollingStart(getName(), timeoutMS);
	}

	public void stopRanging() {
		controller.sensorPollingStop(getName());
	}

	@Override
	public void update(Object raw) {
		++pings;
		lastRaw = (Integer) raw;
		if (isBlocking) {
			try {
				data.put(lastRaw);
			} catch (InterruptedException e) {
				Logging.logError(e);
			}
		}

		invoke("publishRange", lastRaw);
	}

	@Override
	public int getSensorType() {
		return SENSOR_TYPE_ULTRASONIC;
	}

	@Override
	public int[] getSensorConfig() {
		int[] config = new int[]{trigPin, echoPin};
		return config;
	}
	
	public void test(int x, int y){
		log.info("int %d %d", x, y);
	}
	
	public void test(double x, double y){
		log.info(String.format("double %f %f", x, y));
	}
	
	public void test(Double x, Double y){
		log.info("double object %d %d", x, y);
	}
	
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			//Runtime.start("gui", "GUIService");
			
			/*
			int [] config = new int[]{1,2};
			int [] payload = new int[config.length + 2];
			payload = Arrays.copyOfRange(config, 0, 2);
			*/
			
			Runtime.start("srf05", "UltrasonicSensor");
			Runtime.start("python", "Python");
			Runtime.start("gui", "GUIService");
			/*
			srf05.attach("COM9", 7);
			
			Runtime.start("webgui", "WebGui");

			Arduino arduino = srf05.getController();
			arduino.digitalWrite(13, 1);
			arduino.digitalWrite(13, 0);
			arduino.digitalWrite(13, 1);
			arduino.digitalWrite(13, 0);
			arduino.digitalWrite(13, 1);
			Integer version = arduino.getVersion();			
			log.info("version {}", version);
			version = arduino.getVersion();			
			log.info("version {}", version);
			version = arduino.getVersion();			
			log.info("version {}", version);
			
			srf05.startRanging();
			
			srf05.stopRanging();
			
			int x = srf05.range();
			*/
			
			log.info("here");

		} catch (Exception e) {
			Logging.logError(e);
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

		ServiceType meta = new ServiceType(UltrasonicSensor.class.getCanonicalName());
		meta.addDescription("Ranging sensor");
		meta.addCategory("sensor");		
		return meta;
	}

  public int getPings() {
    return pings;
  }

  public String getType() {
    return type;
  }
	
}
