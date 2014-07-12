package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
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
import org.slf4j.Logger;

public class Serial extends Service implements SerialDeviceService, SerialDeviceEventListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	private transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	int BUFFER_SIZE = 8192;
	byte[] buffer = new byte[BUFFER_SIZE];
	transient BlockingQueue<Byte> blockingData = new LinkedBlockingQueue<Byte>();

	private int recievedByteCount = 0;

	boolean publish = true;
	boolean blocking = false;

	private boolean connected = false;
	private String portName = "";

	public static final int PUBLISH_BYTE = 0;
	public static final int PUBLISH_LONG = 1;
	public static final int PUBLISH_INT = 2;
	public static final int PUBLISH_BYTE_ARRAY = 3;
	public static final int PUBLISH_STRING = 4;
	public static final int PUBLISH_MESSAGE = 5;
	public static final int PUBLISH_MRL_MESSAGE = 6;

	public int publishType = PUBLISH_BYTE;

	// Arduino micro-controller specific at the moment
	public int BYTE_SIZE_LONG = 4;
	public int BYTE_SIZE_INT = 2;

	// ====== file io begin ======
	private int fileCnt = 0;
	private boolean useRXFile = false;
	transient private FileWriter fileWriterRX = null;
	transient private BufferedWriter bufferedWriterRX = null;

	// ====== file io end ======

	// display buffer for all RX data
	StringBuffer display = new StringBuffer();
	// pretty print = 3 chars e.g 'FF '
	// * number of bytes 8

	// decimal format will be 4 chars e.g. '127 '
	int displayWidth = 4 * 8;

	public Serial(String n) {
		super(n);
	}

	public void capacity(int size) {
		buffer = new byte[size];
	}

	/**
	 * No ByteBuffer do to referrence - AND the fact
	 * http://royontechnology.blogspot
	 * .com/2012/04/converting-byte-array-to-long.html
	 * 
	 * TODO - add Endianess switch TODO - add "padding" for length < 8 e.g.
	 * Arduino length is 4
	 * 
	 * @param bytes
	 * @param offset
	 * @return
	 */
	public static long byteToLong(byte[] bytes, int offset, int length) {

		long retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= ((long) bytes[offset + i] & 0xFF);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
	}

	public boolean useRXFile(boolean b) {
		try {
			useRXFile = b;
			if (useRXFile) {
				if (fileWriterRX == null) {
					++fileCnt;
					fileWriterRX = new FileWriter(String.format("rx.%d.data", fileCnt));
					bufferedWriterRX = new BufferedWriter(fileWriterRX);
				}
			} else {
				if (fileWriterRX != null) {
					fileWriterRX.flush();
					bufferedWriterRX.flush();
					fileWriterRX.close();
					bufferedWriterRX.close();
				}
			}

			return b;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	// @Override
	public String getDescription() {
		return "reads and writes serial data";
	}

	public String getPortName() {
		return portName;
	}

	public void publishType(Integer type) {
		publishType = type;
	}

	public void publish(Boolean b) {
		publish = b;
	}

	public void publishInt() {
		publishType = PUBLISH_INT;
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

				// stupid Java signed byte :(
				byte newByte;
				// necessary Java unsigned byte in a signed int :(
				int newInt;
				recievedByteCount = 0;

				// log.info("--------begin---------------");
				// jump into loop and process as much as there is
				// good implementation :) - the ---begin-- was useful to see
				// "messages"
				// come in groups of bytes

				// previously ->
				// while (serialDevice.isOpen() && serialDevice.available() > 0)
				// { << DOES NOT WORK
				// OUT OF SYNC ??

				while (serialDevice.isOpen() && (newInt = serialDevice.read()) > -1) {
					newByte = (byte) newInt;

					++recievedByteCount;

					// display / debug option ? - mrl message format ?
					// display.append(String.format("%02x ", newInt));
					display.append(String.format("%03d ", newInt));
					if (display.length() % displayWidth == 0) {
						// display.append("\n");
						log.info(display.toString());
						display.setLength(0);
					}
					// display.append(String.format(" %d ", (int) (newByte &
					// 0xFF)));

					if (useRXFile) {
						// TODO - why use this encoding stream thing?
						// what about just a buffered FileOutputStream ??
						fileWriterRX.write(newInt);
					}

					if (blocking) {
						if (blockingData.size() < BUFFER_SIZE) {
							blockingData.add(newByte);
						} else {
							warn(String.format("overrun data > %d", BUFFER_SIZE));
							blockingData.clear(); // clears the buffer
						}
					}

					if (publish) {
						switch (publishType) {
						
						// PUBLISH_BYTE_ARRAY - wouldnt be bad at all
						// but would require a fixed width OR NOT !!! 
						// JUST PUBLISH WHEN YOU BREAK OUT OF LOOP ???

						// FIXME - remove / deprecate -
						// "publishInt should be in PUBLISH_BYTE"
						case PUBLISH_INT: {
							buffer[recievedByteCount - 1] = newByte;
							invoke("publishInt", newInt);

							break;
						}

						case PUBLISH_BYTE: {
							invoke("publishByte", newByte);
							// log.info(String.format(" %d ", newInt));
							break;
						}

						}
					} // if publish
				}

				// Very useful debugging here
				// log.info("---out of loop----");
				// log.info("cnt {}", recievedByteCount);

			} catch (IOException e) {
				Logging.logException(e);
			}

			break;
		}

	}

	@Override
	public ArrayList<String> getPortNames() {
		return SerialDeviceFactory.getSerialDeviceNames();
	}

	// access to "lowest" level object
	@Override
	public SerialDevice getSerialDevice() {
		return serialDevice;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		if (name == null || name.length() == 0) {
			log.info("got emtpy connect name - disconnecting");
			return disconnect();
		}
		try {
			serialDevice = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (serialDevice != null) {
				if (!serialDevice.isOpen()) {
					serialDevice.open();
					serialDevice.addEventListener(this); // TODO - only add if
															// "publishing" ?
					serialDevice.notifyOnDataAvailable(true);
					sleep(1000);
				}

				serialDevice.setParams(rate, databits, stopbits, parity);
				portName = serialDevice.getName();
				connected = true;
				save(); // successfully bound to port - saving
				broadcastState(); // state has changed let everyone know
				return true;

			} else {
				log.error("could not get serial device");
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

	// FIXME - fixed width and message delimeter
	// FIXME - block read(until block size)

	public byte publishByte(Byte data) {
		// log.info(String.format(" %02x ", (int) (data.intValue() & 0xFF)));
		return data;
	}

	public int publishInt(Integer data) {
		return data;
	}

	public byte[] publishByteArray(byte[] data) {
		return data;
	}

	/**
	 * ------ publishing points end -------
	 */

	/**
	 * -------- blocking reads begin --------
	 * 
	 * @throws IOException
	 * 
	 * @throws InterruptedException
	 */

	// http://stackoverflow.com/questions/11805300/rxtx-java-inputstream-does-not-return-all-the-buffer
	public byte readByte() throws InterruptedException {
		return blockingData.take().byteValue();
	}

	public char readChar() throws InterruptedException {
		return (char) blockingData.take().byteValue();
	}

	public int readInt() throws InterruptedException {
		int count = 0;
		int value = 0;
		byte newByte = -1;
		while ((newByte = blockingData.take().byteValue()) > 0 && count < BYTE_SIZE_INT) {
			++count;
			value = (value << 8) + (newByte & 0xff);
		}
		return value;
	}

	public long readLong() throws InterruptedException {
		int count = 0;
		long value = -1;
		byte newByte = -1;
		while ((newByte = blockingData.take().byteValue()) > 0 && count < BYTE_SIZE_LONG) {
			++count;
			value = (value << 8) + (newByte & 0xff);
		}
		return value;
	}

	public byte[] readByteArray(int length) throws InterruptedException {
		int count = 0;
		byte[] value = new byte[length];
		byte newByte = -1;
		while (count < length && (newByte = blockingData.take().byteValue()) > 0) {
			value[count] = newByte;
			++count;
		}
		return value;
	}

	public String readString(char delimeter) throws InterruptedException {
		StringBuffer value = new StringBuffer();
		byte newByte = -1;
		while ((newByte = blockingData.take().byteValue()) > 0 && newByte != delimeter) {
			value.append(newByte);
		}
		return value.toString();
	}

	public String readString() throws InterruptedException {
		return readString('\n');
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

	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	@Override
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			serialDevice.write(data[i] & 0xff);
		}
	}

	@Override
	public void write(char data) throws IOException {
		serialDevice.write(data);
	}

	@Override
	public void write(int data) throws IOException {

		// log.error("NOT IMPLEMENTED");
		/*
		 * Since the SEAR LIDARsimulator (and other serial stuffs) sends bytes
		 * stored as Integers this only needs to spit out a byte at a time for
		 * SEAR.
		 */

		serialDevice.write(data);
		// log.error("NOT IMPLEMENTED");
		// FIXME bit shift send 4 bytes
	}

	public void write(char[] cs) throws IOException {
		for (int i = 0; i < cs.length; ++i)
			write(cs[i]);
	}

	@Override
	public boolean disconnect() {
		if (serialDevice == null) {
			connected = false;
			portName = "";
			return false;
		}

		serialDevice.close();
		connected = false;
		portName = "";

		broadcastState();
		return true;

	}

	public boolean isBlocking() {
		return blocking;
	}

	public void blocking(boolean b) {
		blocking = b;
	}

	public static VirtualSerialPort createVirtualSerialPort(String port) {
		VirtualSerialPort vp0 = new VirtualSerialPort(port);
		SerialDeviceFactory.add(vp0);
		return vp0;
	}

	public static void createNullModemCable(String port0, String port1) {
		// create 2 virtual ports
		VirtualSerialPort vp0 = new VirtualSerialPort(port0);
		VirtualSerialPort vp1 = new VirtualSerialPort(port1);
		vp1.tx = vp0.rx;
		vp1.rx = vp0.tx;

		// add virtual ports to the serial device factory
		SerialDeviceFactory.add(vp0);
		SerialDeviceFactory.add(vp1);
	}

	@Override
	public int read() throws IOException {
		return serialDevice.read();
	}

	@Override
	public int read(byte[] data) throws IOException {
		return serialDevice.read(data);
	}

	@Override
	public void test() throws IOException {

		// non destructive tests
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done

		info("testing null modem with %s", getName());

		info("creating virtual null modem cable");
		String UART = "UART";
		String COM = "COM";

		createNullModemCable(UART, COM);

		info("creating uart");
		Serial uart = (Serial) Runtime.start(UART, "Serial");

		Runtime.start("gui", "GUIService");

		ArrayList<String> portNames = getPortNames();
		info("reading portnames back %s", Arrays.toString(portNames.toArray()));

		boolean found1 = false;
		boolean found2 = false;
		for (int i = 0; i < portNames.size(); ++i) {
			if (portNames.get(i).equals(UART)) {
				found1 = true;
			}
			if (portNames.get(i).equals(COM)) {
				found2 = true;
			}
		}

		if (found1 && found2) {
			info("found both ports");
		} else {
			throw new IOException("ports not found");
		}

		info("connecting");
		if (!connect(COM) || !uart.connect(UART)) {
			throw new IOException("cant connect");
		}

		info("test blocking");
		info("test publish/subscribe nonblocking");

		info("writing from %s -----> uart", getName());

		// how a byte should be
		write(new byte[] { (byte) ((int) 128 & 0xff), (byte) ((int) 243 & 0xff), (byte) ((int) 127 & 0xff), -128, -128 });

		for (int i = 255; i > 0; --i) {
			write(i);

			write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 });
			write(new byte[] { 5, 5, 5, 5, 5 });
			write(new byte[] { 6, 6, 6, 6, 6 });
			write(new byte[] { 7, 7, 7, 7, 7 });
			write(new byte[] { 8, 8, 8, 8, 8 });
			write(new byte[] { 9, 9, 9, 9, 9 });
			write(new byte[] { 5, 5, 5, 5, 5 });
			write(new byte[] { 6, 6, 6, 6, 6 });
			write(new byte[] { 7, 7, 7, 7, 7 });
			write(new byte[] { 8, 8, 8, 8, 8 });
			write(new byte[] { 9, 9, 9, 9, 9 });
			write(new byte[] { (byte) (128 & 0xff), 9, 9, 9, 9 });

			write(new byte[] { (byte) (128 & 0xff), (byte) (127 & 0xff) });
		}

		info("writing from %s <----- uart", getName());
		uart.write(new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });
		uart.write(new byte[] { 6, 6, 6, 6, 6, 6 });

		write(new byte[] { 16 });

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);
			Runtime.start("gui", "GUIService");
			Serial serial = (Serial) Runtime.start("serial", "Serial");
			// serial.connect("COM15");
			serial.test();
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
