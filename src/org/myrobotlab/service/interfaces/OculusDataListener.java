package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.OculusData;

public interface OculusDataListener {

	public String getName();

	public OculusData onOculusData(OculusData data);
}
