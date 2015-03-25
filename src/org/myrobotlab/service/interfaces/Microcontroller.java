package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;

public interface Microcontroller {

	public void addByteListener(SerialDataListener service);

	public void addCustomMsgListener(Service service);

	public void analogReadPollingStart(int pin) throws IOException;

	public void analogWrite(int pin, int value) throws IOException;

	public void compile(String sketchName, String sketch);

	// serial port
	public boolean connect(String port);

	// filtering
	public void digitalDebounceOn(int delay);

	// reads
	public void digitalReadPollingStart(int pin) throws IOException;

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin);

	public void digitalReadPollingStop(Integer pin);

	// writes
	public void digitalWrite(int pin, int value) throws IOException;

	public boolean disconnect();

	public String getBoardType();

	/**
	 * supported board types
	 * 
	 * @return
	 */
	public List<String> getBoardTypes();

	// pin operations
	public List<Pin> getPinList();

	public String getPortName();

	public ArrayList<String> getPortNames();

	// firmware operations
	public Integer getVersion();

	public boolean isConnected();

	// call-back event
	public Pin publishPin(Pin p);

	// triggering
	public long pulseIn(int trigPin, int echoPin, int timeout, String highLow);

	public String setBoardType();

	public int setSampleRate(int rate);

	public void softReset();

	public void upload(String sketch) throws Throwable;

}
