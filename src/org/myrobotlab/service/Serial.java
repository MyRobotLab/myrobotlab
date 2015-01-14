package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.serial.VirtualSerialPort;
import org.myrobotlab.serial.VirtualSerialPort.VirtualNullModemCable;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

public class Serial extends Service implements SerialDeviceService, SerialDeviceEventListener, SerialDataListener {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	static public final String DISPLAY_DECIMAL = "decimal";
	static public final String DISPLAY_HEX = "hex";
	static public final String DISPLAY_RAW = "ascii";
	static public final String DISPLAY_MRL = "mrl"; // TODO move into Arduino
													// when Serial moves out of
													// Arduino

	static public final String FORMAT_BINARY = "binary";
	static public final String FORMAT_DISPLAY = "display";

	String displayFormat = DISPLAY_DECIMAL;
	String displayDelimiter = " ";

	/**
	 * format can be either binary (default) or display (format which is
	 * currently being displayed)
	 */
	String format = FORMAT_BINARY;

	/**
	 * blocking and non-blocking publish/subscribe reading is possible at the
	 * same time. If blocking is not used then the internal buffer will fill to
	 * the BUFFER_SIZE and just be left - overrun data will be lost
	 */
	transient int BUFFER_SIZE = 1024;
	transient BlockingQueue<Integer> blockingData = new LinkedBlockingQueue<Integer>();

	boolean connected = false;
	String portName = null;
	int rate = 57600;

	// ====== recording file io begin ======
	FileOutputStream fileRX = null;
	FileOutputStream fileTX = null;

	// gui has its own counters
	int txCount = 0;
	int rxCount = 0;

	class Relay extends Thread {
		Serial myService;
		Socket socket;
		OutputStream out;
		InputStream in;
		boolean listening = false;

		public Relay(Serial serial, Socket socket) throws IOException {
			super(String.format("%s.%s", serial.getName(), socket.getRemoteSocketAddress().toString()));
			this.myService = serial;
			this.socket = socket;
			out = socket.getOutputStream();
			in = socket.getInputStream();
			start();
		}

		public void write(int b) throws IOException {
			out.write(b);
		}

		public void run() {
			listening = true;
			try {
				while (listening) {
					int x = in.read();
					log.info(String.format("recvd %d", x));
					myService.write(x);
				}
			} catch (Exception e) {
				log.info("terminating socket");
				Logging.logException(e);
			} finally {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}

	ArrayList<Relay> relays = null;

	public Serial(String n) {
		super(n);
	}

	public void setBufferSize(int size) {
		BUFFER_SIZE = size;
	}

	// ============ conversion begin ========
	/**
	 * converts part of a byte array to a long FIXME - remove this
	 * implementation for the int[] FIXME - support for endianess
	 * 
	 * @param bytes
	 *            - input
	 * @param offset
	 *            - offset to begin
	 * @param length
	 *            - size
	 * @return - converted long
	 */
	public static long bytesToUnsignedInt(byte[] bytes, int offset, int length) {

		long retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= ((long) bytes[offset + i] & 0xFF);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
	}

	public static long bytesToLong(int[] bytes, int offset, int length) {

		long retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= ((long) bytes[offset + i] & 0xFF);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
	}

	// ============ conversion end ========

	// ============ recording begin ========

	public boolean recordRX(String filename) {
		try {
			info(String.format("record RX %s", filename));

			if (fileRX != null) {
				log.info("already recording");
				return true;
			}

			if (filename == null) {
				filename = String.format("rx.%s.%d.data", getName(), System.currentTimeMillis());
			}

			fileRX = new FileOutputStream(filename);

			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean recordTX(String filename) {
		try {

			info(String.format("record TX %s", filename));

			if (fileTX != null) {
				log.info("already recording");
				return true;
			}

			if (filename == null) {
				filename = String.format("tx.%s.%d.data", getName(), System.currentTimeMillis());
			}

			fileTX = new FileOutputStream(filename);
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean record(String filename) {
		boolean ret = true;
		String ext = getExtention();
		ret &= recordTX(String.format("%s.tx.%s", filename, ext));
		ret &= recordRX(String.format("%s.rx.%s", filename, ext));
		return ret;
	}

	public boolean record() {
		String filename = String.format("rxtx.%s.%d.data", getName(), System.currentTimeMillis());
		return record(filename);
	}

	public boolean stopRecording() {
		try {
			if (fileRX != null) {
				fileRX.close();
				fileRX = null;
			}
			if (fileTX != null) {
				fileTX.close();
				fileTX = null;
			}
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean isRecording() {
		if (fileRX != null || fileTX != null) {
			return true;
		}
		return false;
	}

	// ============ recording end ========

	// @Override
	public String getDescription() {
		return "reads and writes serial data";
	}

	/**
	 * get the port name this serial service is currently attached to
	 * 
	 * @return
	 */
	public String getPortName() {
		return portName;
	}

	final String format(final int newByte) {
		if (displayFormat.equals(DISPLAY_DECIMAL)) {
			return (String.format("%03d%s", newByte, displayDelimiter));
		} else if (displayFormat.equals(DISPLAY_HEX)) {
			return (String.format("%02x%s", newByte & 0xff, displayDelimiter));
		} else if (displayFormat.equals(DISPLAY_RAW)) {
			//return (String.format("%c%s", newByte & 0xff, displayDelimiter));
			return (String.format("%c", newByte & 0xff));
		} else if (displayFormat.equals(DISPLAY_MRL)) {
			return (String.format("%c%s", newByte & 0xff, displayDelimiter));
		} else {
			error("unknown format %s", displayFormat);
			return null;
		}
	}

	String getExtention() {
		if (format.equals(FORMAT_DISPLAY)) {
			if (displayFormat.equals(DISPLAY_DECIMAL)) {
				return "dec";
			} else if (displayFormat.equals(DISPLAY_HEX)) {
				return "hex";
			} else if (displayFormat.equals(DISPLAY_RAW)) {
				return "asc";
			} else if (displayFormat.equals(DISPLAY_MRL)) {
				return "mrl";
			} else {
				error("unknown format %s", displayFormat);
				return null;
			}
		} else {
			return "bin";
		}
	}

	/**
	 * If serialEvents are used - thread management is simplified for the
	 * consumer as it uses the underlying serial event management thread.
	 */
	@Override
	public void serialEvent(SerialDeviceEvent event) {
		switch (event.getEventType()) {
		case SerialDeviceEvent.BI:
		case SerialDeviceEvent.OE:
		case SerialDeviceEvent.FE:
		case SerialDeviceEvent.PE:
		case SerialDeviceEvent.CD:
		case SerialDeviceEvent.CTS:
		case SerialDeviceEvent.DSR:
		case SerialDeviceEvent.RI:
		case SerialDeviceEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialDeviceEvent.DATA_AVAILABLE:
			try {

				// Java's signed bytes are not very fun,
				// so we are going to use an int :)
				int newByte;

				// log.info("--------begin---------------");
				// jump into loop and process as much as there is
				// good implementation :) - the ---begin-- was useful to see
				// "messages"
				// come in groups of bytes

				while (serialDevice.isOpen() && (newByte = (serialDevice.read())) > -1) {
					newByte = newByte & 0xff;
					if (relays != null) {
						for (int i = 0; i < relays.size(); ++i) {
							relays.get(i).write(newByte);
						}
					}
					++rxCount;

					// publish the byte !
					invoke("publishByte", newByte);

					// display / debug option ? - mrl message format ?
					// display.append(String.format("%02x ", newByte));
					String display = format(newByte);

					if (fileRX != null) {
						if (format.equals(FORMAT_DISPLAY)) {
							fileRX.write(display.toString().getBytes());
						} else {
							fileRX.write(newByte);
						}
					}

					// publish the display
					invoke("publishDisplay", display);

					if (blockingData.size() < BUFFER_SIZE) {
						blockingData.add(newByte);
					}

				}

				// Very useful debugging here
				// log.info("---out of loop----");
				// log.info("cnt {}", recievedByteCount);

			} catch (Exception e) {
				Logging.logException(e);
			}

			break;
		}

	}

	@Override
	public ArrayList<String> getPortNames() {
		return SerialDeviceFactory.getSerialDeviceNames();
	}

	public void addByteListener(SerialDataListener service) {
		addListener("publishByte", service.getName(), "onByte", Integer.class);
	}

	/***
	 * FIXME FIXME FIXME FIXME - WE NEED METHOD CACHE & BETTER
	 * FINDING/RESOLVING/UPCASTING OF METHODS - THE RESULT IS MAKING HORRIBLE
	 * ABOMINATIONS LIKE THIS - (all gson comes in doubles)
	 */

	public boolean connect(String name, Double rate, Double databits, Double stopbits, Double parity) {
		return connect(name, rate.intValue(), databits.intValue(), stopbits.intValue(), parity.intValue());
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		if (name == null || name.length() == 0) {
			log.info("got emtpy connect name - disconnecting");
			return disconnect();
		}
		try {
			if (serialDevice != null && name.equals(portName) && isConnected()) {
				log.info(String.format("connect to already connected port - setting parameters rate=%d", rate));
				serialDevice.setParams(rate, databits, stopbits, parity);
				save();
				this.rate = rate;
				broadcastState();
				return true;
			}
			serialDevice = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (serialDevice != null) {
				if (!serialDevice.isOpen()) {
					serialDevice.open();
					serialDevice.addEventListener(this);
					serialDevice.notifyOnDataAvailable(true);
					sleep(500); // changed from 1000 ms to 500 ms
				}

				serialDevice.setParams(rate, databits, stopbits, parity);
				this.rate = rate;
				portName = serialDevice.getName();
				connected = true;
				save(); // successfully bound to port - saving
				broadcastState(); // state has changed let everyone know
				return true;

			} else {
				error("could not get serial device");
			}
		} catch (Exception e) {
			logException(e);
		}
		return false;
	}

	@Override
	public boolean connect(String name) {
		return connect(name, 57600, 8, 1, 0);
	}

	/**
	 * ------ publishing points begin -------
	 */

	/**
	 * this is the raw binary byte value stored in the last byte location of the
	 * integer
	 * 
	 * @param data
	 * @return
	 */
	public int publishByte(Integer data) {
		return data;
	}

	/**
	 * this is the interpreted byte to be displayed e.g. if format is decimal
	 * then its a 3 character numeric sequence 000 <-> 255(space) hex 00 <->
	 * FF(space)
	 * 
	 * @param display
	 * @return
	 */
	public String publishDisplay(String display) {
		return display;
	}

	/**
	 * ------ publishing points end -------
	 */

	/**
	 * -------- blocking reads begin --------
	 */

	/**
	 * FIXME - make like http://pyserial.sourceforge.net/pyserial_api.html with
	 * blocking & timeout InputStream like interface - but regrettably
	 * InputStream IS NOT A F#(@!! INTERFACE !!!!
	 * 
	 * WORTHLESS INPUTSTREAM FUNCTION !! -- because if the size of the buffer is
	 * ever bigger than the read and no end of stream has occurred it will block
	 * forever :P
	 * 
	 * pass through to the serial device
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */

	@Override
	public int read() throws IOException {
		try {
			Integer newByte = blockingData.take();
			return newByte;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return -1; // EOF ??
	}

	@Override
	public int read(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			data[i] = (byte) read();
		}
		return data.length;
	}

	public int read(int[] data) throws InterruptedException {
		return read(data, 0);
	}

	public int read(int[] data, int timeoutMS) throws InterruptedException {
		int count = 0;
		Integer newByte = null;
		while (count < data.length) {
			if (timeoutMS < 1) {
				newByte = blockingData.take();
			} else {
				newByte = blockingData.poll(timeoutMS, TimeUnit.MILLISECONDS);
			}
			if (newByte == null) {
				error("expecting %d bytes got %d", data.length, count);
				return count;
			}
			data[count] = newByte;
			++count;
		}
		return count;
	}

	/**
	 * reads the data back from the serial port in string form will potentially
	 * block forever - if a timeout is needed use readString(length, timeout)
	 * 
	 * @param length
	 * @return
	 * @throws InterruptedException
	 */
	String readString(int length) throws InterruptedException {
		return readString(length, 0);
	}

	/**
	 * read a string back from the serial port
	 * 
	 * @param length
	 *            - the number of bytes to read back
	 * @param timeoutMS
	 *            - the amount of time to wait blocking until we return. 0 ms
	 *            means the reading thread will potentially block forever.
	 * @return String form of the bytes read
	 * @throws InterruptedException
	 */
	String readString(int length, int timeoutMS) throws InterruptedException {
		//int[] bytes = new int[length];
		//int count = read(bytes, timeoutMS);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int count = 0;
		Integer newByte = null;
		while (count < length) {
			if (timeoutMS < 1) {
				newByte = blockingData.take();
			} else {
				newByte = blockingData.poll(timeoutMS, TimeUnit.MILLISECONDS);
			}
			if (newByte == null) {
				if (count == 0){
					error("got nothing!");
					return null;
				} else {
					error("expecting %d bytes got %d", length, count);
					break;
				}
			}
			//data[count] = newByte;
			bytes.write((byte)newByte.intValue());
			++count;
		}
		//bytes.close();
		return new String(bytes.toByteArray());
	}

	public static byte[] intArrayToByteArray(int[] src) {

		if (src == null) {
			return null;
		}

		byte[] ret = new byte[src.length];
		for (int i = 0; i < src.length; ++i) {
			ret[i] = (byte) src[i];
		}
		return ret;
	}

	/**
	 * -------- blocking reads begin --------
	 */

	public boolean isConnected() {
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected;
	}

	// FIXME remove blocking public
	// FIXME overload with timeouts etc - remove exposed blocking
	// FIXME - implement
	public byte[] readToDelimiter(String delimeter) {
		return null;
	}

	// ============= write methods begin ====================
	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	// FIXME - your not recording tx !
	@Override
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			++txCount;
			serialDevice.write(data[i] & 0xff);
			if (fileTX != null) {
				// recording
				if (format.equals(FORMAT_DISPLAY)) {
					String display = format(data[i] & 0xff);
					fileTX.write(display.toString().getBytes());
				} else {
					fileTX.write(data);
				}
			}
		}

		// optional do not broadcast tx count
	}

	@Override
	public void write(int data) throws IOException {
		int newByte = data & 0xFF;
		serialDevice.write(newByte);
		if (fileTX != null) {
			if (format.equals(FORMAT_DISPLAY)) {
				String display = format(newByte);
				fileTX.write(display.toString().getBytes());
			} else {
				fileTX.write(newByte);
			}
		}
	}

	public void writeFile(String filename) {
		try {
			// TODO PARSE DEPENDING ON FILE EXTENSION
			write(FileIO.fileToByteArray(new File(filename)));
		} catch (Exception e) {
			error(e);
		}
	}

	// ============= write methods begin ====================

	@Override
	public boolean disconnect() {
		if (serialDevice == null) {
			connected = false;
			portName = null;
			return false;
		}

		serialDevice.close();
		connected = false;
		portName = null;

		broadcastState();
		return true;

	}

	public void clear() {
		blockingData.clear();
	}

	static public VirtualNullModemCable createNullModemCable(String port0, String port1) {
		return VirtualSerialPort.createNullModemCable(port0, port1);
	}

	public Serial createVirtualUART() {
		String name = getName();
		String uartName = String.format("%s_uart", name);
		log.info(String.format("connectToVirtualUART - creating uart %s <--> %s <---> %s <-->", name, name.toUpperCase(), uartName.toUpperCase(), uartName));
		VirtualSerialPort.createNullModemCable(name.toUpperCase(), uartName.toUpperCase());
		// broadcast the new serial ports
		invoke("getPortNames");
		Serial uart = (Serial) Runtime.start(uartName, "Serial");
		uart.connect(name.toUpperCase());
		connect(uartName.toUpperCase());
		return uart;
	}

	public void stopService() {
		super.stopService();
		stopRecording();
	}

	public boolean isOpen() {
		if (serialDevice != null && serialDevice.isOpen()) {
			return true;
		}
		return false;
	}

	public void releaseService() {
		super.releaseService();
		if (isOpen()) {
			disconnect();
		}

		stopRecording();
		log.info(String.format("%s closed ports and files", getName()));
	}

	public void refresh() {
		invoke("getPortNames");
	}

	/**
	 * create a TCP/IP socket which relays (sends and receives) serial data
	 * 
	 * @param host
	 * @param port
	 */
	public void addRelay(String host, int port) {
		try {
			Socket socket = new Socket(host, port);
			if (relays == null) {
				relays = new ArrayList<Relay>();
			}
			relays.add(new Relay(this, socket));
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	/**
	 * onByte is typically the functions clients of the Serial service use when
	 * they want to consume serial data.
	 * 
	 * The serial service implements this function primarily so it can test
	 * itself
	 */
	ArrayList<Integer> readFromPublishedByte = new ArrayList<Integer>();

	@Override
	public void onByte(Integer b) {
		log.info(String.format("%d", b));
		synchronized (readFromPublishedByte) {
			readFromPublishedByte.add(b);
			readFromPublishedByte.notify();
		}
	}

	public boolean setBinaryFileFormat(boolean b) {
		if (b) {
			format = FORMAT_BINARY;
		} else {
			format = FORMAT_DISPLAY;
		}

		return b;
	}

	@Override
	public Status test() {

		// non destructive tests
		// TODO - test blocking / non blocking / time-out blocking / reading an
		// array (or don't bother?) or do with length? num bytes to block or
		// timeout
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done
		// FIXME - very little functionality for a combined tx rx file
		// TODO - test sendFile & record
		// TODO - speed test
		// TODO use utility methods to help parse read data types
		// because we should not assume we know the details of ints longs etc
		// nor
		// the endianess
		// utility methods - ascii
		// FIXME - // test case write(-1) as display becomes -1 ! - file is
		// different than gui !?!?!

		Status status = super.test();
		try {

			int timeout = 500;// 500 ms serial timeout

			Runtime.start("gui", "GUIService");
			// Runtime.start("webgui", "WebGUI");

			// get serial handle and creates a uart & virtual null modem cable
			Serial serial = (Serial) Runtime.start(getName(), "Serial");
			Serial uart = serial.createVirtualUART();

			// verify the null modem cable is connected
			if (!serial.isConnected()) {
				throw new IOException(String.format("%s not connected", serial.getName()));
			}

			if (!uart.isConnected()) {
				throw new IOException(String.format("%s not connected", uart.getName()));
			}

			// start binary recording
			serial.record("test/Serial/serial.1");
			uart.record("test/Serial/uart.1");

			// test blocking on exact size
			serial.write("VER\r");
			uart.write("000D\r");
			// read back
			log.info(serial.readString(5));

			// blocking read with timeout
			uart.write("HELO");
			String helo = serial.readString(5, timeout);

			if ("HELO".equals(helo)) {
				status.addError("did not read back!");
			}

			info("read back [%s]", helo);

			info("array write");
			serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 127, (byte) 128, (byte) 254, (byte) 255 });

			// FIXME !!! - bug - we wrote a big array to serial -
			// then immediately cleared the uart buffer
			// in fact we cleared it so fast - that the serial data ---going
			// to----> uart
			// has not reached uart (because there is some overhead in moving,
			// reading and formatting the incoming data)
			// then we start checking values in "test blocking" this by that
			// time the serial data above has hit the
			// uart

			// with a virtual null modem cable I could "cheat" and flush() could
			// look at the serial's tx buffer size
			// and block until its cleared - but this would not be typical of
			// "real" serial ports
			// but it could stabilize the test

			// in the real world we don't know when the sender to
			// our receiver is done - so we'll sleep here
			sleep(300);
			info("clear buffers");
			serial.clear();
			uart.clear();

			if (serial.available() != 0) {
				status.addError("available data after clear");
			}

			info("testing blocking");
			for (int i = 257; i > -2; --i) {
				serial.write(i);
				int readBack = uart.read();
				log.info(String.format("written %d read back %d", i, readBack));
				if (i < 256 && i > -1) {
					if (readBack != i) {
						status.addError("read back not the same as written for value %d %d !", i, readBack);
					}
				}
			}

			// in the real world we don't know when the sender to
			// our receiver is done - so we'll sleep here
			sleep(300);
			info("clear buffers");
			serial.clear();
			uart.clear();

			// test publish/subscribe nonblocking
			addByteListener(this);
			uart.write(64);

			synchronized (readFromPublishedByte) {
				log.info("started wait");
				readFromPublishedByte.wait(1000);
				log.info("finished wait");
			}

			if (readFromPublishedByte.size() != 1) {
				status.addError("wrong size %d returned", readFromPublishedByte.size());
			}

			if (readFromPublishedByte.get(0) != 64) {
				status.addError("wrong size returned published");
			}

			serial.stopRecording();
			uart.stopRecording();

			// ======= decimal format begin ===========
			serial.setBinaryFileFormat(false);
			uart.setBinaryFileFormat(false);

			// default non-binary format is ascii decimal
			serial.record("test/Serial/serial.2");
			// uart.record("test/Serial/uart.2");
			serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
			// we have to pause here momentarily
			// so the data can be written and read from the virtual null modem
			// cable (on different threads)
			// before we close the file streams
			sleep(30);
			// uart.stopRecording();
			serial.stopRecording();
			// ======= decimal format end ===========

			// ======= hex format begin ===========
			serial.setDisplayFormat(DISPLAY_HEX);
			serial.record("test/Serial/serial.3");
			// uart.record("test/Serial/uart.3");
			serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
			sleep(30);
			serial.stopRecording();
			// uart.stopRecording();
			// ======= hex format begin ===========

			// parsing of files based on extension check

			// TODO flush & close tests ?
//			serial.disconnect();
//			uart.disconnect();

			if (status.hasError()) {
				log.error("we have an error!");
			}

			log.info(status.flatten().toString());

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}

	public byte[] parse(byte[] data, String format) {
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		if (DISPLAY_HEX.equals(format)) {
			int charCount = 0;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < data.length; ++i) {
				byte b = data[i];
				if (b == ' ') {
					continue;
				}

				sb.append((char) data[i]);
				++i;
				sb.append((char) data[i]);

				// Integer.parseInt(b.toString());

				sb.setLength(0);
			}
		}

		// return bytes.toArray(byte[]);
		return data;
	}

	public String setDisplayFormat(String display) {
		displayFormat = display;
		return display;
	}
	
	public String getDisplayFormat(){
		return displayFormat;
	}

	@Override
	public int available() {
		return blockingData.size();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			// Runtime.start("gui", "GUIService");
			Serial serial = (Serial) Runtime.start("serial", "Serial");
			// serial.connect("COM15");
			serial.test();
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
