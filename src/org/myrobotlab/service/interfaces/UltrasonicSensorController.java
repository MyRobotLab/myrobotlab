package org.myrobotlab.service.interfaces;

public interface UltrasonicSensorController extends DeviceController {
	
	// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
	public void attach(UltrasonicSensorControl control, Integer triggerPin, Integer echoPin) throws Exception;

	// > ultrasonicSensorStartRanging/deviceId/b32 timeout
	public void ultrasonicSensorStartRanging(UltrasonicSensorControl sensor);
	
	// > ultrasonicSensorStopRanging/deviceId
	public void ultrasonicSensorStopRanging(UltrasonicSensorControl sensor);

	// FIXME - is the controller or MicroController ?
	public boolean isConnected();

	public void connect(String port);

}
