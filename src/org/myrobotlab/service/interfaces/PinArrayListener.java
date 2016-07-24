package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinData;

public interface PinArrayListener extends Listener {
	
	public void onPinArray(PinData[] pindata);
}
