package org.myrobotlab.service.interfaces;


public interface SensorDataSink {
	
	// FYI - these are NOT Arduino specific !!!
	public static final int SENSOR_TYPE_PIN          = 0;
	public static final int SENSOR_TYPE_ULTRASONIC   = 1;
	public static final int SENSOR_TYPE_PULSE        = 2;
	
	// return types
	public static final int DATA_SINK_TYPE_INTEGER   = 0; // 16 bit ?
	public static final int DATA_SINK_TYPE_PIN       = 1;
	
	/**
	 * callback method from controller - updates the Sensor with data
	 * can not throw as its the controller's thread
	 * @param data
	 */
	public void update(Object data);
	
	/**
	 * gets the IDL id type of data the sensor expects
	 * from the Object data callback
	 * @return
	 */
	// public Class<?> getDataSinkType(); - Gson won't serialize this
	// PUBLISH_SENSOR_DATA - return datatype - non Java IDL !!!
	public int getDataSinkType();
	
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
