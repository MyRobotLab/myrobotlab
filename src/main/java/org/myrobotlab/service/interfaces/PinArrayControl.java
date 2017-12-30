package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.PinData;

public interface PinArrayControl extends NameProvider {

	public List<PinDefinition> getPinList();
	
  public PinDefinition getPin(String pinName);
  
  public PinDefinition getPin(Integer address);
  
	/*
	 * read pin based on index or address of the pin - this is always numeric
	 * follows InputStream spec
	 * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.
	 *  If no byte is available because the end of the stream has been reached, the value -1 is returned. 
	 *  This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
	 *  
	 */
	public int read(Integer address);
	
	/*
	 * same as read(int address) except by name e.g. read("D5")
	 */
	public int read(String pinName);
	
	// FIXME - DEPRECATE - this is "very" Arduino specific
	public void pinMode(Integer address, String mode);
	
	public void write(Integer address, Integer value);
	
	public PinData publishPin(PinData pinData);
	
	public PinData[] publishPinArray(PinData[] pinData);
	
	// FIXME attach(String listener, null) -> listens to all pins - pin array comes back ?
	// public void attach(String listener, int pinAddress);
	
	public void attach(PinListener listener, Integer pinAddress);
	
	public void attach(PinArrayListener listener);
	
	public void enablePin(Integer address);
	
	public void disablePin(Integer address);
	
	public void disablePins();

	public void enablePin(Integer address, Integer rate);
	
}
