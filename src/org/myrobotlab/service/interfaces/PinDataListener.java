package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinData;

public interface PinDataListener extends NameProvider {
	
	public void onPinData(PinData pindata);
}
