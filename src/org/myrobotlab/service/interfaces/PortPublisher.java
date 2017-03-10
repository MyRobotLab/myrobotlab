package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PortPublisher extends NameProvider {
	
	public String publishConnect(String portName);
	
	public String publishDisconnect(String portName);
	
	public boolean isConnected();
	
	public String getPortName();
	
	public List<String> getPortNames();

}
