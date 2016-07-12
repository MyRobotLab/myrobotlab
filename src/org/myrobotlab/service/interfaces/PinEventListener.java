package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinEvent;

public interface PinEventListener extends Listener {
	
	public void onPinData(PinEvent pindata);
}
