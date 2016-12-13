package org.myrobotlab.service.interfaces;

public interface UltrasonicSensorControl extends DeviceControl {
	
	public void attach(UltrasonicSensorController controller, Integer trigPin, Integer echoPin) throws Exception; 
	
	public void startRanging();
	
	public void stopRanging();
	
	public Integer onUltrasonicSensorData(Integer us);

}
