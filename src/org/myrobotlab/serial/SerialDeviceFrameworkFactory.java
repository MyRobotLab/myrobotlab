package org.myrobotlab.serial;

import java.util.ArrayList;

public interface SerialDeviceFrameworkFactory {

	public SerialDevice getSerialDevice(String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException;

	// public ArrayList<SerialDevice> getSerialDevices();
	public ArrayList<String> getSerialDeviceNames();

}
