package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface PortListener extends NameProvider {

	  public void onConnect(String portName);

	  public void onDisconnect(String portName);
	  
}
