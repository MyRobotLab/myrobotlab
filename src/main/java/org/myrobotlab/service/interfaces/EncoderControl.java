package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

public interface EncoderControl extends Attachable {
	
	public void attach(EncoderController controller, Integer pin) throws Exception; 
	
	// TODO: publish encoder data that includes the name/id of the device.
	public Float onEncoderData(Float position);
	
	// the cable select pin that enables this encoder.  
	public int getPin();

}
