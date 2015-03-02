package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;

public interface Microcontroller {
	
	// reads
	public void digitalReadPollingStart(int pin) throws IOException;
	public void analogReadPollingStart(int pin) throws IOException;
	public int setSampleRate(int rate);

	// writes
	public void digitalWrite(int pin, int value) throws IOException;
	public void analogWrite(int pin, int value) throws IOException;
	
	// filtering
	public void digitalDebounceOn(int delay);
	
	// pin operations
	public List<Pin> getPinList();
	
	// call-back event
	public Pin publishPin(Pin p);
	
	// firmware operations
	public Integer getVersion();
	public void softReset();
	public String getBoardType();
	public String setBoardType();
	/**
	 * supported board types
	 * @return
	 */
	public List<String> getBoardTypes();

	public void compile(String sketchName, String sketch);
	public void upload(String sketch) throws Throwable;

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin);
	public void digitalReadPollingStop(Integer pin);

	// serial port
	public boolean connect(String port);
	public boolean isConnected();
	public boolean disconnect();
	public String getPortName();
	public ArrayList<String> getPortNames();

	// triggering
	public long pulseIn(int trigPin, int echoPin, int timeout, String highLow);

	public void addCustomMsgListener(Service service);

	public void addByteListener(SerialDataListener service);

}
