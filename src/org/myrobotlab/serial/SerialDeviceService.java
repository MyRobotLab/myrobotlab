package org.myrobotlab.serial;

import java.util.ArrayList;

import org.myrobotlab.service.interfaces.SerialDataListener;

public interface SerialDeviceService {

	/**
	 * methods to return read-only information regarding a serialDevice
	 */
	public ArrayList<String> getPortNames();

	/**
	 * @return a read-only copy of the SerialDevice if it has been serialized
	 *         over the network - the InputStream & OutputStream are transient
	 */

	// FIXME - why "connect" versus InputStrea/OutputStream open ? 
	public boolean connect(String name); 

	public boolean connect(String name, int rate, int databits, int stopbits, int parity);

	public void addByteListener(SerialDataListener service);

	public void write(String data) throws Exception;

	public void write(byte[] data) throws Exception;

	public void write(int data) throws Exception;

	public int read(byte[] data) throws Exception;

	public int read() throws Exception;
	
	// FYI - PREVIOUS METHODS SUPPORT THE PARTIALLY GOOFY OUTPUTSTREAM INTERFACE
	// - THIS ONE IS NOT PART OF OUTPUTSTREAM
	public void write(int[] data) throws Exception;

	// symmetric with "connect" - assumes flush & close
	public void disconnect();

	public boolean isConnected();

	/**
	 * Returns an estimate of the number of bytes that can be read (or skipped
	 * over) from this input stream without blocking
	 * 
	 * @return
	 */
	public int available();

}
