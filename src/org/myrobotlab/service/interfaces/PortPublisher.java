package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface PortPublisher extends NameProvider {
	
	public String publishConnect(String portName);
	
	public String publishDisconnect(String portName);
	
	public boolean isConnected();
	
	public String getPortName();
	
	public List<String> getPortNames();

}
