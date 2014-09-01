package org.myrobotlab.roomba;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceFactory;

/**
 * The serial-port based implementation of RoombaComm. Handles both physical
 * RS-232 ports, USB adapter ports like Keyspan USA-19HS, and Bluetooth serial
 * port profiles.
 * 
 * <p>
 * Some code taken from processing.serial.Serial. Thanks guys!
 * </p>
 * 
 * The interaction model for setting the port and protocol and WaitForDSR
 * parameters is as follows.
 * <p>
 * On creation, the class initializes the parameters, then tries to read
 * .roomba_config. If it can read the config file and parse out the parameters,
 * it sets the parameters to the values in the config file. Apps can read the
 * current settings for display using methods on the class. Apps can override
 * the settings by accepting user input and setting the parameters using methods
 * on the class, or the connect() method. Parameters that are changed by the app
 * are re-written in the config file, for use as defaults next run. Command-line
 * apps can make these parameters optional, by using the defaults if the user
 * doesn't specify them.
 */
public class RoombaCommSerialDevice extends RoombaComm implements SerialDeviceEventListener {
	private int rate = 57600;
	static final int databits = 8;
	static final int parity = SerialDevice.PARITY_NONE;
	static final int stopbits = SerialDevice.STOPBITS_1;
	private String protocol = "SCI";

	/**
	 * contains a list of all the ports keys are port names (e.g.
	 * "/dev/usbserial1") values are Boolean in-use indicator
	 */
	static Map ports = null;

	/**
	 * The time to wait in milliseconds after sending sensors command before
	 * attempting to read
	 */
	public static int updateSensorsPause = 400;

	/** The RXTX port object, normally you don't need access to this */
	public SerialDevice port = null;
	private String portname = null; // "/dev/cu.KeySerial1" for instance

	/** the serial input stream, normally you don't need access to this */
	// public InputStream input;
	/** the serial output stream, normally you don't need access to this */
	// public OutputStream output;

	/**
	 * RXTX bombs when flushing output sometimes, so by default do not flush the
	 * output stream. If the output is too buffered to be useful, do:
	 * roombacomm.comm.flushOutput = true; before using it and see if it works.
	 */
	public boolean flushOutput = false;
	/**
	 * Some "virtual" serial ports like Bluetooth serial on Windows return weird
	 * errors deep inside RXTX if an opened port is used before the virtual COM
	 * port is ready. One way to check that it is ready is to look for the DSR
	 * line going high. However, most simple, real serial ports do not do
	 * hardware handshaking so never set DSR high. Thus, if using Bluetooth
	 * serial on Windows, do: roombacomm.waitForDSR = true; before using it and
	 * see if it works.
	 */
	public boolean waitForDSR = false; // Warning: public attribute - setting
										// won't trigger config file write

	byte buffer[] = new byte[32768];
	int bufferLast;

	// int bufferSize = 26; // how big before reset or event firing
	// boolean bufferUntil;
	// int bufferUntilByte;

	/**
	 * Let you check to see if a port is in use by another Rooomba before trying
	 * to use it.
	 */
	public static boolean isPortInUse(String pname) {
		Boolean inuse = (Boolean) ports.get(pname);
		if (inuse != null) {
			return inuse.booleanValue();
		}
		return false;
	}

	// constructor
	public RoombaCommSerialDevice() {
		super();
		makePorts();
		readConfigFile();
	}

	public RoombaCommSerialDevice(boolean autoupdate) {
		super(autoupdate);
		makePorts();
		readConfigFile();
	}

	public RoombaCommSerialDevice(boolean autoupdate, int updateTime) {
		super(autoupdate, updateTime);
		makePorts();
		readConfigFile();
	}

	void makePorts() {
		if (ports == null)
			ports = Collections.synchronizedMap(new TreeMap());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#connect(java.lang.String)
	 */
	@Override
	public boolean connect(String portid) {
		logmsg("connecting to port '" + portid + "'");
		portname = portid;
		writeConfigFile(portname, protocol, waitForDSR ? 'Y' : 'N');

		if (isPortInUse(portid)) {
			logmsg("port is in use");
			return false;
		}

		connected = open_port();

		if (connected) {
			// log in the global ports hash if the port is in use now or not
			ports.put(portname, new Boolean(connected));
			sensorsValid = false;
		} else {
			disconnect();
		}

		return connected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#disconnect()
	 */
	@Override
	public void disconnect() {
		connected = false;

		// log in the global ports hash if the port is in use now or not
		ports.put(portname, new Boolean(connected));

		/*
		 * try { // do io streams need to be closed first? if (input != null)
		 * input.close(); if (output != null) output.close(); } catch (Exception
		 * e) { e.printStackTrace(); } input = null; output = null;
		 */

		try {
			if (port != null)
				port.close(); // close the port
		} catch (Exception e) {
			e.printStackTrace();
		}
		port = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#send(byte[])
	 */
	// FIXME - IS THIS RIGHT ?!?!? - why would you send real Java bytes - you'd have to 
	// "load" them incorrectly send them
	// 
	@Override
	public boolean send(byte[] bytes) {
		try {
			// BLECH - conversion to support silly send(byte[] bytes)
			int[] ints = new int[bytes.length];
			for (int i = 0; i < ints.length; ++i){
				ints[i] = bytes[i];
			}
			port.write(ints);
			// if( flushOutput ) port.flush(); // hmm, not sure if a good idea
		} catch (Exception e) { // null pointer or serial port dead
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#send(int)
	 */
	@Override
	public boolean send(int b) { // will also cover char or byte
		try {
			port.write(b & 0xff); // for good measure do the &
			// if( flushOutput ) output.flush(); // hmm, not sure if a good idea
		} catch (Exception e) { // null pointer or serial port dead
			// errorMessage("send", e);
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#wakeup()
	 */
	@Override
	public void wakeup() {
		port.setDTR(false);
		pause(500);
		port.setDTR(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#updateSensors()
	 */
	@Override
	public boolean updateSensors() {
		sensorsValid = false;
		sensors();
		for (int i = 0; i < 20; i++) {
			if (sensorsValid) {
				logmsg("updateSensors: sensorsValid!");
				break;
			}
			logmsg("updateSensors: pausing...");
			pause(50);
		}

		return sensorsValid;
	}

	public boolean updateSensors(int packetcode) {
		sensorsValid = false;
		sensors(packetcode);
		for (int i = 0; i < 20; i++) {
			if (sensorsValid) {
				logmsg("updateSensors: sensorsValid!");
				break;
			}
			logmsg("updateSensors: pausing...");
			pause(50);
		}

		return sensorsValid;
	}

	public void computeSensors() {
		sensorsValid = true;
		sensorsLastUpdateTime = System.currentTimeMillis();
		computeSafetyFault();
	}

	/*
	 * pause(updateSensorsPause); // take a breather to let data come back
	 * sensorsValid = false; // assume the worst, we're gothy int n =
	 * available(); //logmsg("updateSensors:n="+n); if( n >= 26) { // there are
	 * enough bytes to read n = readBytes(sensor_bytes); if( n==26 ) { // did we
	 * get enough? sensorsValid = true; // then everything's good, otherwise bad
	 * computeSafetyFault(); } } else {
	 * logmsg("updateSensors:only "+n+" bytes available, not updating sensors");
	 * }
	 * 
	 * //logmsg("buffer contains: "+ buffer ); return sensorsValid;
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.roomba.Z#listPorts()
	 */
	@Override
	public String[] listPorts() {
		String[] ret = null;
		try {
			ArrayList<String> names = SerialDeviceFactory.getSerialDeviceNames();
			ret = new String[names.size()];
			return names.toArray(ret);
		} catch (UnsatisfiedLinkError e) {
			// System.err.println("1");
			errorMessage("listPorts", e);
		} catch (Exception e) {
			// System.err.println("2");
			errorMessage("listPorts", e);
		}

		return null;

	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		if (protocol.equals("SCI")) {
			rate = 57600;
		} else if (protocol.equals("OI")) {
			rate = 115200;
		}
		this.protocol = protocol;
		logmsg("Protocol: " + protocol);
		// writeConfigFile(portname, protocol, waitForDSR?'Y':'N'); FIXME -
		// remove use Service.save() !
	}

	public boolean isWaitForDSR() {
		return waitForDSR;
	}

	public void setWaitForDSR(boolean waitForDSR) {
		this.waitForDSR = waitForDSR;
		writeConfigFile(portname, protocol, waitForDSR ? 'Y' : 'N');
	}

	public String getPortname() {
		return portname;
	}

	public void setPortname(String p) {
		portname = p;
		logmsg("Port: " + portname);
		// writeConfigFile(portname, protocol, waitForDSR?'Y':'N'); fixme - use
		// Service.save()

	}

	// -------------------------------------------------------------
	// below only used internally to this class
	// -------------------------------------------------------------

	/**
	 * internal method, used by connect() FIXME: make it faile more gracefully,
	 * recognize bad port
	 */
	private boolean open_port() {
		boolean success = false;
		try {/*
			 * Enumeration portList = CommPortIdentifier.getPortIdentifiers();
			 * while (portList.hasMoreElements()) { CommPortIdentifier portId =
			 * (CommPortIdentifier) portList.nextElement();
			 * 
			 * if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
			 * System.out.println("found " + portId.getName()); if
			 * (portId.getName().equals(portname)) { logmsg("open_port:"+
			 * portId.getName()); port =
			 * (SerialDevice)portId.open("roomba serial", 2000);
			 * //port.setFlowControlMode(SerialDevice.FLOWCONTROL_NONE); input =
			 * port.getInputStream(); output = port.getOutputStream();
			 * port.setSerialDeviceParams(rate,databits,stopbits,parity);
			 * port.addEventListener(this); port.notifyOnDataAvailable(true);
			 * logmsg("port "+portname+" opened successfully");
			 * 
			 * if( waitForDSR ) { int i=40; while( !port.isDSR() && i-- != 0) {
			 * logmsg("DSR not ready yet"); pause(150); // 150*40 = 6 seconds }
			 * success = port.isDSR(); } else { success = true; } } } }
			 */
			port = SerialDeviceFactory.getSerialDevice(portname, rate, databits, stopbits, parity);
			if (port != null) {
				port.open();
				// input = port.getInputStream();
				// output = port.getOutputStream();
				// port.setSerialDeviceParams(rate,databits,stopbits,parity);
				port.addEventListener(this);
				port.notifyOnDataAvailable(true);
				logmsg("port " + portname + " opened successfully");

				return true;
			}

		} catch (Exception e) {
			logmsg("connect failed: " + e);
			port = null;
			// input = null;
			// output = null;
		}

		return success;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.myrobotlab.roomba.Z#serialEvent(org.myrobotlab.serial.SerialDeviceEvent
	 * )
	 */
	@Override
	// public void serialEvent(SerialDeviceEvent ev) {
	synchronized public void serialEvent(SerialDeviceEvent serialEvent) {
		try {
			// logmsg("serialEvent:"+serialEvent+", nvailable:"+port.available());
			if (serialEvent.getEventType() == SerialDeviceEvent.DATA_AVAILABLE) {
				while (port.available() > 0) { // FIXME !!!! - JSSC DOES NOT
												// SUPPORT AVAILAVLE !!!!
					// logmsg("serialEvent: available="+input.available());
					buffer[bufferLast++] = (byte) port.read();
					if (bufferLast == 26) {
						bufferLast = 0;
						System.arraycopy(buffer, 0, sensor_bytes, 0, 26);
						computeSensors();
					}
					/*
					 * synchronized (buffer) { if (bufferLast == buffer.length)
					 * { byte temp[] = new byte[bufferLast << 1];
					 * System.arraycopy(buffer, 0, temp, 0, bufferLast); buffer
					 * = temp; } buffer[bufferLast++] = (byte) input.read(); }
					 */
				} // while
			}
		} catch (IOException e) {
			errorMessage("serialEvent", e);
		}
	}

	/**
	 * Write a config file with current settings
	 */
	private void writeConfigFile(String port, String protocol, char waitForDSR) {
		try {
			FileWriter f = new FileWriter(".roomba_config", false);
			BufferedWriter w = new BufferedWriter(f); // create file
			w.write(port);
			w.newLine();
			w.write(protocol);
			w.newLine();
			w.write(waitForDSR);
			w.newLine();
			w.close();
			f.close();
		} catch (IOException e) {
			logmsg("unable to write .roomba_config " + e);
		}
	}

	// FIXME - use XML config
	private void readConfigFile() {
		try {
			FileReader f = new FileReader(".roomba_config");
			BufferedReader r = new BufferedReader(f);
			portname = r.readLine();
			protocol = r.readLine();
			String wait = r.readLine();
			if ("OI".equals(protocol)) {
				rate = 115200;
			}

			if (wait != null)
				waitForDSR = wait.equals("Y") ? true : false;
			logmsg("read config port: " + port + " protocol: " + protocol + " waitDSR: " + waitForDSR);
		} catch (IOException e) {
			logmsg("unable to read .roomba_config " + e);
		}
	}

	/**
	 * Returns the number of bytes that have been read from serial and are
	 * waiting to be dealt with by the user. (from processing.serial.Serial)
	 * 
	 * private int available() { return (bufferLast - bufferIndex); }
	 * 
	 * /** Return a byte array of anything that's in the serial buffer. Not
	 * particularly memory/speed efficient, because it creates a byte array on
	 * each read, but it's easier to use than readBytes(byte b[]) (see below).
	 * (from processing.serial.Serial)
	 * 
	 * private byte[] readBytes() { if (bufferIndex == bufferLast) return null;
	 * 
	 * synchronized (buffer) { int length = bufferLast - bufferIndex; byte
	 * outgoing[] = new byte[length]; System.arraycopy(buffer, bufferIndex,
	 * outgoing, 0, length);
	 * 
	 * bufferIndex = 0; // rewind bufferLast = 0; return outgoing; } }
	 * 
	 * /** Grab whatever is in the serial buffer, and stuff it into a byte
	 * buffer passed in by the user. This is more memory/time efficient than
	 * readBytes() returning a byte[] array.
	 * 
	 * Returns an int for how many bytes were read. If more bytes are available
	 * than can fit into the byte array, only those that will fit are read.
	 * (from processing.serial.Serial)
	 * 
	 * public int readBytes(byte outgoing[]) { if (bufferIndex == bufferLast)
	 * return 0;
	 * 
	 * synchronized (buffer) { int length = bufferLast - bufferIndex; if (length
	 * > outgoing.length) length = outgoing.length; System.arraycopy(buffer,
	 * bufferIndex, outgoing, 0, length);
	 * 
	 * bufferIndex += length; if (bufferIndex == bufferLast) { bufferIndex = 0;
	 * // rewind bufferLast = 0; } return length; } }
	 */

}
