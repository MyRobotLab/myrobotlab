package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PinArrayControl extends DeviceControl {

	public List<PinDefinition> getPinList();
	
	public void setMode(String address, String mode);
	
	public Integer read(String address);

	public void write(String address, Integer value);
}
