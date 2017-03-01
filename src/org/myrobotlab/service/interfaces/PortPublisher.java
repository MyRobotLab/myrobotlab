package org.myrobotlab.service.interfaces;

public interface PortPublisher extends NameProvider {
	
	public String publishConnect(String portName);
	
	public String publishDisconnect(String portName);

}
