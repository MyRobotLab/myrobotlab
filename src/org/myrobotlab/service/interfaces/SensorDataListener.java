package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.SensorData;

/**
 * A SensorDataListener is a (microcontroller) Device which can read data
 *
 */
public interface SensorDataListener extends Device {  
    // PIR ??  OScope gui ? other listeners?
    
    /**
     * When sensor data is published by the SensorDataPublisher
     * it arrives here from a callback
     * 
     * This method does not use MRL queues and is a direct callback
     * from the publisher
     * 
     * @param data - the sensor data
     */
    public void update(SensorData data);

    /**
     * onSensorData is called when SensorData is published from the
     * SensorDataPublisher - this uses MRL queues, and is capable of
     * being published across process bounderies or over the network
     * 
     * @param data - the sensor data
     */
    public void onSensorData(SensorData data);
    
   
    /**
     * Sensors types should be handled as static string values
     * in Sensors (they are microcontroller agnostic)
     * 
     * @return
     */
    /*  DEPRECATED by getDeviceType()
    public int getSensorType();
    
    */
    
}