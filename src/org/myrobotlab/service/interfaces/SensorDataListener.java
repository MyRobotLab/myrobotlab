package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.SensorData;

/**
 * A SensorDataListener Device which can read data
 *
 */
public interface SensorDataListener {  
    // PIR ??  OScope gui ? other listeners?

    /**
     * onSensorData is called when SensorData is published from the
     * SensorDataPublisher - this uses MRL queues, and is capable of
     * being published across process boundaries or over the network
     * 
     * @param data - the sensor data
     */
    public void onSensorData(SensorData data);
    
    public boolean isLocal();
    
}