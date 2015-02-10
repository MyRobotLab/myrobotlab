package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.OculusData;

public interface OculusDataListener {
	
	public OculusData onOculusData(OculusData data);
	
	public String getName();
}
