package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.service.data.PinData;

public interface PinArrayControl extends DeviceControl {

	public List<PinDefinition> getPinList();
	
	/**
	 * read pin based on index or address of the pin - this is always numeric
	 * follows InputStream spec
	 * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.
	 *  If no byte is available because the end of the stream has been reached, the value -1 is returned. 
	 *  This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
	 *  
	 * @param address
	 * @return
	 */
	public int read(int address);
	
	/**
	 * same as read(int address) except by name e.g. read("D5")
	 * @param pinName
	 * @return
	 */
	public int read(String pinName);
	
	public void pinMode(int address, String mode);
	
	public void write(int address, int value);
	
	public PinData publishPin(PinData pinData);
	
	public PinData[] publishPinArray(PinData[] pinData);
	
	// FIXME attach(String listener, null) -> listens to all pins - pin array comes back ?
	// public void attach(String listener, int pinAddress);
	
	public void attach(PinListener listener, int pinAddress);
	
	public void attach(PinArrayListener listener);
	
	public void enablePin(int address);
	
	public void disablePin(int address);
	
	public void disablePins();

	public void enablePin(int address, int rate);
	
}
