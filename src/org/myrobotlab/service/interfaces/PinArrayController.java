package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PinArrayController extends DeviceController {

	public List<PinDefinition> pinArrayGetPinList();
	
	public int pinArrayRead(int address);
	
	public int pinArrayRead(String address);
	
	public void pinArraySetMode(int address, String mode);
	
	public void pinArraySetMode(String address, String mode);

	public void pinArrayWrite(int address, int value);
	
	public void pinArrayWrite(String address, int value);
}
