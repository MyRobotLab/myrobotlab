package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PinArrayController extends DeviceController {

	public List<PinDefinition> pinArrayGetPinList();
	
	public Integer pinArrayRead(int address);
	
	public Integer pinArrayRead(String address);
	
	public void pinArraySetMode(int address, String mode);
	
	public void pinArraySetMode(String address, String mode);

	public void pinArrayWrite(int address, Integer value);
	
	public void pinArrayWrite(String address, Integer value);
}
