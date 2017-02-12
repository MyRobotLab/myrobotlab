package org.myrobotlab.service.interfaces;

public interface UltrasonicSensorControl extends RangingControl {
	
	public void attach(UltrasonicSensorController controller, Integer trigPin, Integer echoPin) throws Exception; 
	
	public Double onUltrasonicSensorData(Double us);

}
