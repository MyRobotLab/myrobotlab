package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PinArrayControl extends DeviceControl {

	public List<PinDefinition> getPinList();
	
	public Integer read(int address);
	
	public Integer read(String address);
	
	public void setMode(int address, String mode);
	
	public void setMode(String address, String mode);

	public void write(int address, Integer value);
	
	public void write(String address, Integer value);
}
