package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.SensorData;

public interface SensorDataListener {  
    // PIR ??  OScope gui ? other listeners?
    public String getName();
    public void onSensorData(SensorData data);
    
}