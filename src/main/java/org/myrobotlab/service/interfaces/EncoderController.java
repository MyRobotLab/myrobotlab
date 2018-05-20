package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

public interface EncoderController extends Attachable {
	
	// > encoderAttach/deviceId/pin
	public void attach(EncoderControl control, Integer pin) throws Exception;

	// TODO: publish EncoderData
	public EncoderData publishEncoderPosition(Integer deviceId, Integer position);

}
