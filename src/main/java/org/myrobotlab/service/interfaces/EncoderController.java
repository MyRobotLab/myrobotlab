package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

public interface EncoderController extends Attachable {
	
	// > encoderAttach/deviceId/pin
	public void attach(EncoderControl control, Integer pin) throws Exception;

	// TODO: publish EncoderData
	public Float publishEncoderPosition(Integer deviceId, Float position);

}
