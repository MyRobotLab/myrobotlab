package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.SensorEvent;

/**
 * A SensorDataListener Device which can read data
 *
 */
public interface SensorEventListener {  
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
    // public void update(SensorEvent data); NO !!! - only 1 method needed
	// the appropriate direct callback vs the pub/sub/queue done the the service
	// providing the data - (although this should be fixed so its done by framework)

    /**
     * onSensorData is called when SensorData is published from the
     * SensorDataPublisher - this uses MRL queues, and is capable of
     * being published across process boundaries or over the network
     * 
     * @param data - the sensor data
     */
    public void onSensorEvent(SensorEvent data);
    
}