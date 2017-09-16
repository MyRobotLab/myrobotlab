package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

public interface UltrasonicSensorControl extends RangingControl, Attachable {
	
	public void attach(UltrasonicSensorController controller, Integer trigPin, Integer echoPin) throws Exception; 
	
	public Double onUltrasonicSensorData(Double us);

}
