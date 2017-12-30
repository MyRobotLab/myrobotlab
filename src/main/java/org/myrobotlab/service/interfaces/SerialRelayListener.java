package org.myrobotlab.service.interfaces;

import java.io.IOException;

import org.myrobotlab.service.Arduino;

public interface SerialRelayListener {  
	public void connect(Arduino controller, String serialPort) throws IOException;
}

