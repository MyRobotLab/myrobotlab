package org.myrobotlab.serial;

import java.util.EventListener;

public interface SerialDeviceEventListener extends EventListener {

	public abstract void serialEvent(SerialDeviceEvent ev);
}
