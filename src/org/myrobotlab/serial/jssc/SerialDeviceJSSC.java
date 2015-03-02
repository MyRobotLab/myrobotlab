package org.myrobotlab.serial.jssc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventListener;
import java.util.TooManyListenersException;

import javax.swing.event.EventListenerList;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

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
 *         A silly but necessary wrapper
 * 
 *         References :
 *         http://code.google.com/p/java-simple-serial-connector/wiki
 *         /jSSC_examples
 *         http://www.javaprogrammingforums.com/java-se-api-tutorials
 *         /5603-jssc-library-easy-work-serial-ports.html
 *         http://en.wikibooks.org/wiki/Serial_Programming/Serial_Java
 * 
 */
public class SerialDeviceJSSC implements SerialDevice, SerialPortEventListener {

	public final static Logger log = LoggerFactory.getLogger(SerialDeviceJSSC.class.getCanonicalName());

	transient private jssc.SerialPort port;

	// defaults
	private int rate = 57600;
	private int databits = 8;
	private int stopbits = 1;
	private int parity = 0;
	
	protected EventListenerList listenerList = new EventListenerList();

	public SerialDeviceJSSC(String portName) throws SerialDeviceException {
		try {
			port = new SerialPort(portName);
			port.openPort();// Open serial port
			port.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (SerialPortException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public SerialDeviceJSSC(String portName, int rate, int databits, int stopbits, int parity) {
		port = new SerialPort(portName);
		try {
			port.setParams(rate, databits, stopbits, parity);
		} catch (SerialPortException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		// set the state variables so
		// we can re-open again
		this.rate = rate;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	@Override
	public boolean isOpen() {
		return port != null && port.isOpened();
	}

	@Override
	public void close() {
		log.debug(String.format("closing %s", port.getPortName()));

		if (port == null) {
			log.warn(String.format("serial device %s already closed", port.getPortName()));
			return;
		}
		try {
			port.removeEventListener();
		} catch (SerialPortException e1) {
			log.error(e1.getMessage());
			e1.printStackTrace();
		}

		Object[] listeners = listenerList.getListenerList();
		for (int i = 1; i < listeners.length; i += 2) {
			listenerList.remove(EventListener.class, (EventListener) listeners[i]);
		}

		log.info(String.format("closing SerialDevice %s", port.getPortName()));
		try {
			// do io streams need to be closed first?
			if (port != null)
				port.closePort();

		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug(String.format("closed %s", port.getPortName()));
	}

	@Override
	public String getName() {
		// return port.getName();
		return port.getPortName();
	}

	@Override
	public void setParams(int rate, int databits, int stopbits, int parity) throws SerialDeviceException {
		try {
			log.debug(String.format("setSerialPortParams %d %d %d %d", rate, databits, stopbits, parity));
			port.setParams(rate, databits, stopbits, parity);
		} catch (Exception e) {
			throw new SerialDeviceException("unsupported comm operation " + e.getMessage());
		}
	}

	@Override
	public void setDTR(boolean state) {
		try {
			port.setDTR(state);
		} catch (SerialPortException e) {
			log.error("setDTR error");
			e.printStackTrace();
		}
	}

	@Override
	public void setRTS(boolean state) {
		try {
			port.setRTS(state);
		} catch (SerialPortException e) {
			log.error("setRTS error");
			e.printStackTrace();
		}
	}

	@Override
	// FIXME - need MASK ?
	public void notifyOnDataAvailable(boolean enable) {
		// port.notifyOnDataAvailable(enable);
	}

	@Override
	public void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException {
		// proxy events
		listenerList.add(SerialDeviceEventListener.class, lsnr);
		try {
			port.addEventListener(this);
		} catch (SerialPortException e) {
			log.error(String.format("addEventListener %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent spe) {

		fireSerialDeviceEvent(new SerialDeviceEvent(port, spe.getEventType()));
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

		try {
			port.writeIntArray(data);
		} catch (SerialPortException e) {
			log.error(e.getMessage());
		}

	}

	@Override
	public void write(int data) throws IOException {
		try {
			port.writeInt(data);
		} catch (SerialPortException e) {
			//throw new IOException(e);
			Logging.logException(e);
		}
	}

	@Override
	public void open() throws SerialDeviceException {
		try {
			log.info(String.format("opening %s", port.getPortName()));
			if (isOpen()) {
				log.warn("port already opened, you should fix calling code....");
				// FIXME !!! - the listener is busted at this point !!
				return;
			}

			port.openPort();
			port.setParams(rate, databits, stopbits, parity);
			log.info(String.format("opened %s", port.getPortName()));
		} catch (Exception e) {
			Logging.logException(e);
			throw new SerialDeviceException("port in use " + e.getMessage());
		}
	}

	@Override
	// FIXME - use the throw
	public int read() throws IOException {
		try {
			return port.readIntArray(1)[0];
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public int available() {
		// don't know how to implement this
		return -1;
	}

	@Override
	public int read(byte[] data) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}
}
