package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.OculusData;

public interface OculusDataPublisher {
	
	public OculusData publishOculusData(OculusData data);
	
	public String getName();
}
