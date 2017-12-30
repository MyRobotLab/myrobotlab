package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinData;

public interface PinListener extends Listener {
	
	public void onPin(PinData pindata);
}
