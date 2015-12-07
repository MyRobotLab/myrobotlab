package org.myrobotlab.service.interfaces;


public interface SensorDataSink {
	
	// FYI - these are NOT Arduino specific !!!
	public static final int SENSOR_PIN = 0;
	public static final int SENSOR_ULTRASONIC = 1;
	public static final int SENSOR_PULSE = 2;
	
	/**
	 * callback method from controller - updates the Sensor with data
	 * can not throw as its the controller's thread
	 * @param data
	 */
	public void update(Object data);
	
	/**
	 * gets the type of data the sensor expects
	 * from the Object data callback
	 * @return
	 */
	// public Class<?> getDataSinkType(); - Gson won't serialize this
	public String getDataSinkType();
	
	/**
	 * high level identifier - for languages which handle
	 * strings nicely ;)
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * byte identifier - to be sent to lower level firmware in
	 * micro-controller to setup sensor
	 * @return
	 */
	public int getSensorType();
	
	/**
	 * this would be the low level config needed for the firmware
	 * tyically its the pins needed to support the sensor
	 * Ultrasonic utilizes 2 pins, encoders sometimes need 2, pir needs one...etc
	 * @return
	 */
	public int[] getSensorConfig();
	
}
