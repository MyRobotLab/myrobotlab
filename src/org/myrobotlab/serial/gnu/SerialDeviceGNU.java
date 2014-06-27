package org.myrobotlab.serial.gnu;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.RXTXHack;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventListener;
import java.util.TooManyListenersException;

import javax.swing.event.EventListenerList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         A silly but necessary wrapper class for gnu.io.SerialPort, since
 *         RXTXComm driver initializes loadlibrary in a static block and the
 *         driver dynamically loaded is loaded with a hardcoded string :P
 * 
 */
public class SerialDeviceGNU implements SerialDevice, SerialPortEventListener  {

	public final static Logger log = LoggerFactory.getLogger(SerialDeviceGNU.class.getCanonicalName());

	private gnu.io.RXTXPort port;

	// defaults
	private int rate = 57600;
	private int databits = 8;
	private int stopbits = 1;
	private int parity = 0;

	transient InputStream input;
	transient OutputStream output;

	protected EventListenerList listenerList = new EventListenerList();
	private CommPortIdentifier commPortId;

	public SerialDeviceGNU(CommPortIdentifier portId) throws SerialDeviceException {
		this.commPortId = portId;
	}

	public SerialDeviceGNU(CommPortIdentifier portId, int rate, int databits, int stopbits, int parity) {
		this.commPortId = portId;
		this.rate = rate;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	public String getPortString() {
		return String.format(("%s/%d/%d/%d/%d"), port.getName(), port.getBaudRate(), port.getDataBits(), port.getParity(), port.getStopBits());
	}

	@Override
	public boolean isOpen() {
		return port != null;
	}

	@Override
	public void close() {
		log.debug(String.format("closing %s", commPortId.getName()));

		if (port == null) {
			log.warn(String.format("serial device %s already closed", commPortId.getName()));
			return;
		}
		// port.removeEventListener();
		// port.close();

		Object[] listeners = listenerList.getListenerList();
		for (int i = 1; i < listeners.length; i += 2) {
			listenerList.remove(EventListener.class, (EventListener) listeners[i]);
		}

		log.info(String.format("closing SerialDevice %s", getPortString()));
		try {
			// do io streams need to be closed first?
			if (input != null)
				input.close();
			if (output != null)
				output.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		input = null;
		output = null;
		if (port != null) {
			/*
			 * cheesy way to close serial port since it will sometimes hang
			 * forever Hangs on Ubuntu 12.04.1 LTS - IcedTea6 1.11.4
			 */
			new Thread() {

				@Override
				public void run() {
					// https://forums.oracle.com/thread/1294323
					// port.IOLocked = 0;
					RXTXHack.closeRxtxPort(port);
					SerialPort hangMe = port;
					hangMe.removeEventListener();
					hangMe.close();
					hangMe = null;
				}
			}.start();
			// port.close();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			port = null;
		}
		log.debug(String.format("closed %s", commPortId.getName()));
	}

	public InputStream getInputStream() {
		return port.getInputStream();
	}

	public OutputStream getOutputStream() {
		return port.getOutputStream();
	}

	@Override
	public String getName() {
		// return port.getName();
		return commPortId.getName();
	}

	@Override
	public void setParams(int rate, int databits, int stopbits, int parity) throws SerialDeviceException {
		try {
			log.debug(String.format("setSerialPortParams %d %d %d %d", rate, databits, stopbits, parity));
			port.setSerialPortParams(rate, databits, stopbits, parity);
		} catch (UnsupportedCommOperationException e) {
			throw new SerialDeviceException("unsupported comm operation " + e.getMessage());
		}
	}

	public int getBaudRate() {
		return port.getBaudRate();
	}

	public int getDataBits() {
		return port.getDataBits();
	}

	public int getStopBits() {
		return port.getStopBits();
	}

	public int getParity() {
		return port.getParity();
	}

	public boolean isDTR() {
		return port.isDTR();
	}

	@Override
	public void setDTR(boolean state) {
		port.setDTR(state);
	}

	@Override
	public void setRTS(boolean state) {
		port.setRTS(state);
	}

	public boolean isCTS() {
		return port.isCTS();
	}

	public boolean isDSR() {
		return port.isDSR();
	}

	public boolean isCD() {
		return port.isCD();
	}

	public boolean isRI() {
		return port.isRI();
	}

	public boolean isRTS() {
		return port.isRTS();
	}

	@Override
	public void notifyOnDataAvailable(boolean enable) {
		port.notifyOnDataAvailable(enable);
	}

	@Override
	public void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException {
		// proxy events
		listenerList.add(SerialDeviceEventListener.class, lsnr);
		port.addEventListener(this);
	}

	@Override
	public void serialEvent(SerialPortEvent spe) {

		fireSerialDeviceEvent(new SerialDeviceEvent(port, spe.getEventType(), spe.getOldValue(), spe.getNewValue()));
	}

	// This methods allows classes to unregister for MyEvents
	public void removeSerialDeviceEventListener(SerialDeviceEventListener listener) {
		log.debug("removeSerialDeviceEventListener");
		listenerList.remove(SerialDeviceEventListener.class, listener);
	}

	// This private class is used to fire MyEvents
	void fireSerialDeviceEvent(SerialDeviceEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == SerialDeviceEventListener.class) {
				((SerialDeviceEventListener) listeners[i + 1]).serialEvent(evt);
			}
		}
	}

	@Override
	public void write(int[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			output.write(data[i]);
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			output.write(data[i]);
		}
	}

	@Override
	public void write(String data) throws IOException {
		for (int i = 0; i < data.length(); ++i) {
			output.write(data.charAt(i));
		}
	}

	@Override
	public void write(int data) throws IOException {
		output.write(data);
	}

	@Override
	public void write(byte data) throws IOException {
		output.write(data);
	}

	@Override
	public void write(char data) throws IOException {
		output.write(data);
	}

	public String getCurrentOwner() {
		if (commPortId != null)
			return commPortId.getCurrentOwner();
		return null;
	}

	public int getPortType() {
		return commPortId.getPortType();
	}

	public boolean isCurrentlyOwned() {
		return commPortId.isCurrentlyOwned();
	}

	@Override
	public void open() throws SerialDeviceException {
		try {
			log.info(String.format("opening %s", commPortId.getName()));
			if (isOpen()) {
				log.warn("port already opened, you should fix calling code....");
				// FIXME !!! - the listener is busted at this point !!
				return;
			}

			port = (RXTXPort) commPortId.open(commPortId.getName(), 1000);
			port.setSerialPortParams(rate, databits, stopbits, parity);
			output = port.getOutputStream();
			input = port.getInputStream();
			log.info(String.format("opened %s", commPortId.getName()));
		} catch (PortInUseException e) {
			Logging.logException(e);
			throw new SerialDeviceException("port in use " + e.getMessage());
		} catch (UnsupportedCommOperationException e) {
			Logging.logException(e);
			throw new SerialDeviceException("UnsupportedCommOperationException " + e.getMessage());
		} /*catch (IOException e) {
			Logging.logException(e);
			throw new SerialDeviceException("IOException " + e.getMessage());
		}*/
	}

	@Override
	public int read() throws IOException {
		return input.read();
	}

	@Override
	public int read(byte[] data) throws IOException {
		return input.read(data);
	}

	
	@Override
	public int available() {
		if (input != null) {
			try {
				return input.available();
			} catch (IOException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}
		return 0;
	}

}
