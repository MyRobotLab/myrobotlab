package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.service.data.Pin;

public interface Microcontroller extends NameProvider, DeviceController {

	/**
	 * Connects the DeviceController to something 'real' so it can provide the
	 * control it needs
	 * 
	 * @param port
	 * @throws Exception
	 */
	public void connect(String port) throws Exception;

	public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception;
	// public void connect(String port, Object...config) throws Exception;

	public void disconnect();

	/**
	 * tests if this controller is connected & ready
	 * 
	 * @return
	 */
	public boolean isConnected();

	// metadata about the controller
	public String getBoardType();

	public Integer getVersion();

	public List<Pin> getPinList();

	public void sensorPollingStart(String name);

	public void sensorPollingStop(String name);

}
