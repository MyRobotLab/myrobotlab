package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

public class Serial extends Service implements SerialDeviceService, SerialDeviceEventListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	static public final String FORMAT_DECIMAL = "decimal";
	static public final String FORMAT_HEX = "hex";
	static public final String FORMAT_ASCII = "ascii";
	String format = FORMAT_DECIMAL;
	String delimeter = " ";

	int BUFFER_SIZE = 8192;
	int[] buffer = new int[BUFFER_SIZE];
	transient BlockingQueue<Integer> blockingData = new LinkedBlockingQueue<Integer>();

	int recievedByteCount = 0;

	boolean publish = true;
	boolean blocking = false;

	boolean connected = false;
	String portName = null;

	// TODO use utility methods to help parse read data types
	// because we should not assume we know the details of ints longs etc nor
	// the endianess
	// utility methods - ascii

	// ====== file io begin ======
	String filenameRX;
	boolean isRXRecording = false;
	transient FileWriter fileWriterRX = null;
	transient BufferedWriter bufferedWriterRX = null;
	
	String filenameTX;
	boolean isTXRecording = false;
	transient FileWriter fileWriterTX = null;
	transient BufferedWriter bufferedWriterTX = null;


	// ====== file io end ======

	ArrayList<SerialDataListener> listeners = new ArrayList<SerialDataListener>();

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
		buffer = new int[size];
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

	public static long byteToLong(int[] bytes, int offset, int length) {

		long retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= ((long) bytes[offset + i] & 0xFF);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
	}

	public boolean recordRX(String filename) {
		try {

			if (filename == null) {
				filenameRX = String.format("rx.%s.data", getName(), System.currentTimeMillis());
			}
			
			if (filenameTX.equals(filename)){
				fileWriterRX = fileWriterTX;
				bufferedWriterRX = bufferedWriterTX;
			}

			if (fileWriterRX == null) {
				fileWriterRX = new FileWriter(filenameRX);
				bufferedWriterRX = new BufferedWriter(fileWriterRX);
			}

			isRXRecording = true;
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}
	
	public boolean recordTX(String filename) {
		try {

			if (filename == null) {
				filenameTX = String.format("tx.%s.data", getName(), System.currentTimeMillis());
			}
			
			if (filenameRX.equals(filename)){
				fileWriterTX = fileWriterRX;
				bufferedWriterTX = bufferedWriterRX;
			}

			if (fileWriterTX == null) {
				fileWriterTX = new FileWriter(filenameTX);
				bufferedWriterTX = new BufferedWriter(fileWriterTX);
			}

			isTXRecording = true;
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}


	public boolean record(String filename) {
		boolean ret = true;
		ret &= recordTX(filename);
		ret &= recordRX(filename);
		return ret;
	}
	
	public boolean record(){
		return record(null);
	}

	public boolean stopRecording() {
		try {
			isRXRecording = true;
			isTXRecording = true;
			
			if (fileWriterRX != null) {
				bufferedWriterRX.close();
				fileWriterRX.close();
				fileWriterRX = null;
				bufferedWriterRX = null;
			}
			
			if (fileWriterTX != null) {
				bufferedWriterTX.close();
				fileWriterTX.close();
				fileWriterTX = null;
				bufferedWriterTX = null;
			}

			return true;
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
				int newByte;
				// necessary Java unsigned byte in a signed int :(
				recievedByteCount = 0;

				// log.info("--------begin---------------");
				// jump into loop and process as much as there is
				// good implementation :) - the ---begin-- was useful to see
				// "messages"
				// come in groups of bytes

				// FIXME - use ints not bytes for gods sakes
				// does read() block ? if so - you want to publish byte by byte
				// (cuz we don't want to block)
				// if it does not block - then we can publish an array of ints

				while (serialDevice.isOpen() && (newByte = serialDevice.read()) > -1) {
					++recievedByteCount;

					// display / debug option ? - mrl message format ?
					// display.append(String.format("%02x ", newByte));

					display.append(String.format("%03d%s", newByte, delimeter));

					// send data to listeners
					for (int i = 0; i < listeners.size(); ++i) {
						listeners.get(i).onByte(newByte);
					}

					if (isRXRecording) {
						bufferedWriterRX.write(display.toString());
					}

					if (blocking) {
						if (blockingData.size() < BUFFER_SIZE) {
							blockingData.add(newByte);
						} else {
							warn(String.format("overrun data > %d", BUFFER_SIZE));
							blockingData.clear(); // clears the buffer
						}
					}

					// publish if desired
					if (publish) {
						invoke("publishByte", newByte);
					}

					display.setLength(0);
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
					serialDevice.addEventListener(this);
					serialDevice.notifyOnDataAvailable(true);
					sleep(500); // changed from 1000 ms to 500 ms
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

	public int publishByte(Integer data) {
		return data;
	}

	public int[] publishByteBuffer(int[] data) {
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
	public int readByte() throws InterruptedException {
		return blockingData.take().byteValue();
	}

	public int[] readByteArray(int length) throws InterruptedException {
		int count = 0;
		int[] value = new int[length];
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
			value.append(newByte); // FIXME to ascii ?
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
			portName = null;
			return false;
		}

		serialDevice.close();
		connected = false;
		portName = null;

		broadcastState();
		return true;

	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean b) {
		blocking = b;
	}

	/**
	 * virtual serial ports will be used in the future to connect to simulated
	 * environments or used in automated testing
	 * 
	 * @param port0
	 *            - name of virtual port device
	 */
	public static VirtualSerialPort createVirtualSerialPort(String port) {
		VirtualSerialPort vp0 = new VirtualSerialPort(port);
		SerialDeviceFactory.add(vp0);
		return vp0;
	}

	public static class VirtualNullModemCable {
		public VirtualSerialPort vp0;
		public VirtualSerialPort vp1;

		public VirtualNullModemCable(String port0, String port1) {
			// create 2 virtual ports
			vp0 = new VirtualSerialPort(port0);
			vp1 = new VirtualSerialPort(port1);
			// twist the cable
			vp1.tx = vp0.rx;
			vp1.rx = vp0.tx;

			// add virtual ports to the serial device factory
			SerialDeviceFactory.add(vp0);
			SerialDeviceFactory.add(vp1);
		}

		public void close() {
			// TODO Auto-generated method stub
			// vp1.rx.add(SHUTDOWN) SHUTDOWN = largest signed int value ??
			// vp1.release();
			// vp0.release();
			vp0.close();
			vp1.close();
			log.info("releasing virtual null modem cable");
		}

	}

	/**
	 * virtual null modem cable is used to connect to a simulated device or in
	 * automated testing{
	 * 
	 * @param port0
	 * @param port1
	 */
	public static VirtualNullModemCable createNullModemCable(String port0, String port1) {
		return new VirtualNullModemCable(port0, port1);
	}

	/**
	 * pass through to the serial device
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read() throws IOException {
		return serialDevice.read();
	}

	/**
	 * pass through to the serial device
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read(byte[] data) throws IOException {
		return serialDevice.read(data);
	}

	@Override
	public Status test(){

		// non destructive tests
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done
		Status status = super.test();
		try {

		info("testing null modem with %s", getName());
		Serial test = (Serial) Runtime.start(getName(), "Serial");
		test.record();

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
		} catch(Exception e){
			status.addError(e);
		}
		
		return status;

	}

	public void test2() throws IOException {

		info("creating virtual null modem cable");

		createNullModemCable("UART1", "VCOM1");

		info("creating uart");
		Serial uart1 = (Serial) Runtime.start("UART1", "Serial");
		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");

		InMoov i01 = (InMoov) Runtime.start("i01", "InMOov");

		info("connecting");
		if (!uart1.connect("UART1")) {
			throw new IOException("cant connect");
		}

	}

	public void stopService() {
		super.stopService();
		stopRecording();
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

	public boolean isOpen() {
		if (serialDevice != null && serialDevice.isOpen()) {
			return true;
		}
		return false;
	}

	
}
