package org.myrobotlab.service.interfaces;

public interface PortListener extends NameProvider {

	  public void onConnect(String portName);

	  public void onDisconnect(String portName);
	  
}
