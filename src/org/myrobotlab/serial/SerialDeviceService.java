package org.myrobotlab.serial;

import java.io.IOException;
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

	// FIXME - should probably throw general Exception - and allow
	// implementation a specific exception
	// FIXME - connect versus open ? why its just another char dev ?
	public boolean connect(String name); // left to the service to determine
											// parameters

	public boolean connect(String name, int rate, int databits, int stopbits, int parity);

	public void addByteListener(SerialDataListener service);

	public void write(String data) throws IOException;

	public void write(byte[] data) throws IOException;

	public void write(int data) throws IOException;

	public int read(byte[] data) throws IOException;

	public int read() throws IOException;

	// symmetric with "connect" - assumes flush & close
	public boolean disconnect();

	public boolean isConnected();

	/**
	 * Returns an estimate of the number of bytes that can be read (or skipped
	 * over) from this input stream without blocking
	 * 
	 * @return
	 */
	public int available();

}
