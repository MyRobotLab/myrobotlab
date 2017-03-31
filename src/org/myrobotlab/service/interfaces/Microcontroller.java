package org.myrobotlab.service.interfaces;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.PinDefinitions;

// FIXME add SensorController
public interface Microcontroller extends DeviceController {

	/**
	 * Connects the DeviceController to something 'real' so it can provide the
	 * control it needs
	 * 
	 * @param port
	 * @throws Exception
	 */
	public void connect(String port) throws Exception;

	public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception;

	public void disconnect();

	public boolean isConnected();

	public String getBoardType();

	public BoardInfo getBoardInfo();
	
	/**
	 * WOW - Java 8 static in Interface !!! GOODTIMES !!
	 * @param boardType
	 * @return
	 */
	static public PinDefinitions createPinList(String boardType){
	  return null;
	}

}
