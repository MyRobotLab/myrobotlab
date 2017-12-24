package org.myrobotlab.service.interfaces;

import java.io.IOException;

import org.myrobotlab.framework.interfaces.Attachable;

public interface SerialDevice extends PortPublisher, Attachable {

	void open(String portname) throws IOException;
	
	void open(String portname, int rate, int dataBits, int stopBits, int parity)  throws IOException;

	/*
	 * FIXME - make like http://pyserial.sourceforge.net/pyserial_api.html with
	 * blocking &amp; timeout InputStream like interface - but regrettably
	 * InputStream IS NOT A F#(@!! INTERFACE !!!!
	 * 
	 * WORTHLESS INPUTSTREAM FUNCTION !! -- because if the size of the buffer is
	 * ever bigger than the read and no end of stream has occurred it will block
	 * forever :P
	 * 
	 * pass through to the serial device
	 * @throws Exception
	 */
	int read() throws Exception;

	// write(byte[] b) IOException
	void write(byte[] data) throws Exception;

	void write(int data) throws Exception;
	
	void write(String data) throws Exception;

	void clear();

	void setTimeout(int timeoutMs);

	void flush();

	int available();
	
	void close() throws IOException;

  public int getRate();
  
  public int getDataBits();
  
  public int getStopBits();
  
  public int parity();

}