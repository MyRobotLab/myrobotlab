package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.OculusData;

public interface OculusDataPublisher {

	public String getName();

	public OculusData publishOculusData(OculusData data);
}
