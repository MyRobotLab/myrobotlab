package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
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

	// TODO - test blocking / non blocking / time-out blocking / reading an
	// array (or don't bother?) or do with length? num bytes to block or timeout

	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	static public final String FORMAT_DECIMAL = "decimal";
	static public final String FORMAT_HEX = "hex";
	static public final String FORMAT_ASCII = "ascii";
	String format = FORMAT_DECIMAL;
	String delimeter = " ";

	// there is a stream of data that is event driven
	// but somtimes a request / response is desired
	// the buffer supports this - the stream is forked
	// and a buffer
	transient int BUFFER_SIZE = 8192;
	// transient int[] buffer = new int[BUFFER_SIZE];
	transient BlockingQueue<Integer> blockingData = new LinkedBlockingQueue<Integer>();

	int recievedByteCount = 0;
	boolean blocking = true;
	boolean connected = false;
	String portName = null;
	int rate = 57600;

	/**
	 * FIXME FIXME FIXME FIXME use blocking only from lower level api
	 * InputStream !!! BUT ! - implement timeout similar to python serial
	 */

	// TODO use utility methods to help parse read data types
	// because we should not assume we know the details of ints longs etc nor
	// the endianess
	// utility methods - ascii

	// ====== file io begin ======
	String filenameRX;
	boolean isRXRecording = false;
	transient FileWriter fileWriterRX = null;
	transient BufferedWriter bufferedWriterRX = null;
	String rxFileFormat;

	String filenameTX;
	boolean isTXRecording = false;
	transient FileWriter fileWriterTX = null;
	transient BufferedWriter bufferedWriterTX = null;
	String txFileFormat;

	// ====== file io end ======
	// removed local listeners - in favor of pub/sub framework
	// ArrayList<SerialDataListener> listeners = new
	// ArrayList<SerialDataListener>();

	// display buffer for all RX data
	StringBuffer display = new StringBuffer();

	// pretty print = 3 chars e.g 'FF '
	// * number of bytes 8

	public Serial(String n) {
		super(n);
	}

	public void capacity(int size) {
		// buffer = new int[size];
		BUFFER_SIZE = size;
	}

	/**
	 * FIXME - implement - depending on file extention .asc .dec .bin parse and
	 * load into a byte[]
	 * 
	 * @param filename
	 * @return
	 */
	public byte[] loadFile(String filename) {
		return null;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean sendFile(String filename) {
		byte[] ret = loadFile(filename);
		return false;
	}

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
				filenameRX = String.format("rx.%s.%d.data", getName(), System.currentTimeMillis());
			} else {
				filenameTX = filename;
			}

			if (filenameTX != null && filenameTX.equals(filename)) {
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
				filenameTX = String.format("tx.%s.%d.data", getName(), System.currentTimeMillis());
			} else {
				filenameTX = filename;
			}

			if (filenameRX != null && filenameRX.equals(filename)) {
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

	public boolean record() {
		String filename = String.format("rxtx.%s.%d.data", getName(), System.currentTimeMillis());
		return record(filename);
	}

	public boolean stopRecording() {
		try {
			isRXRecording = false;
			isTXRecording = false;

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

					// FIXME - delay and read as much as possible (in loop)
					// versus
					// respond immediately and incur overhead + buffer overrun

					if (isRXRecording) {
						bufferedWriterRX.write(display.toString());
					}

					display.setLength(0);

					// send data to listeners - local callback
					// design decision - removing direct callback
					// in favor of pub/sub framework - to add remote
					// capability as well
					/*
					 * for (int i = 0; i < listeners.size(); ++i) {
					 * listeners.get(i).onByte(newByte); }
					 */
					// publish if desired - simplified to "always" publish
					// needs to fork the data to a buffer to support IOStream
					// like interface
					invoke("publishByte", newByte);

					// if (blocking) {
					if (blockingData.size() < BUFFER_SIZE) {
						blockingData.add(newByte);
					} else {
						warn(String.format("overrun data > %d", BUFFER_SIZE));
						// blockingData.clear(); // clears the buffer
					}
					// }

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

	public void addByteListener(SerialDataListener service) {
		addListener("publishByte", service.getName(), "onByte", Integer.class);
	}

	/***
	 * FIXME FIXME FIXME FIXME - WE NEED METHOD CACHE & BETTER
	 * FINDING/RESOLVING/UPCASTING OF METHODS - THE RESULT IS MAKING HORRIBLE
	 * ABOMINATIONS LIKE THIS
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
	 */

	/**
	 * FIXME - make like http://pyserial.sourceforge.net/pyserial_api.html with
	 * blocking & timeout - and for starters GET RID OF THE DUMB SIGNED BYTE
	 * ARRAY!
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

		// block - use pySerial as a guide of interface !

		// default - block forever - timeout configurable

		return serialDevice.read();
	}

	@Override
	public int read(byte[] data) throws IOException {
		// return serialDevice.read(data);
		return 0;
	}

	public int read(int[] data) throws InterruptedException {
		return read(data, 500);
	}

	public int read(int[] data, int timeoutMS) throws InterruptedException {
		int count = 0;
		Integer newByte = null;
		while (count < data.length) {
			newByte = blockingData.poll(timeoutMS, TimeUnit.MILLISECONDS);
			if (newByte == null) {
				error("expecting %d bytes got %d", data.length, count);
				return count;
			}
			data[count] = newByte;
			++count;
		}

		return count;
	}

	String readString(int length) throws InterruptedException{
		int[] bytes = new int[length];
		read(bytes);
		return new String(intArrayToByteArray(bytes));
	}

	
	/*
	public static int[] byteArrayToIntArray(byte[] src) {
		IntBuffer
		return intBuffer.array();
	}
	*/
	
	public static byte[] intArrayToByteArray(int[] src) {

		/*
		ByteBuffer byteBuffer = ByteBuffer.allocate(src.length * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(src);
		return byteBuffer.array();
		*/
		
		
		if (src == null) {
			return null;
		}
		
		byte[] ret = new byte[src.length];
		for (int i = 0; i < src.length; ++i) {
			ret[i] = (byte) src[i];
		}
		return ret;
	}

	/*
	 * 
	 * public String readString(int length) throws IOException,
	 * InterruptedException{ // FIXME - lower level does timeout non-blocking
	 * etc .. return new String(intArrayToByteArray(readByteArray(length))); }
	 * 
	 * 
	 * 
	 * public void clear(){ blockingData.clear(); }
	 * 
	 * //
	 * http://stackoverflow.com/questions/11805300/rxtx-java-inputstream-does-
	 * not-return-all-the-buffer // FIXME - more params lie pySerial public int
	 * readByte() throws InterruptedException { return blockingData.take(); }
	 * 
	 * public int[] readByteArray(int length) throws InterruptedException {
	 * return readByteArray(length, 500); }
	 * 
	 * public int[] readByteArray(int length, int timeout) throws
	 * InterruptedException { int count = 0; int[] value = new int[length];
	 * Integer newByte = null; //blockingData.clear(); <-- you can't clear
	 * because write came earlier & events have already proccessed // including
	 * unloading it on the blockingData // blocking = true; <-- always true GAH
	 * NO !! unless you round robin it while (count < length) { newByte =
	 * blockingData.poll(timeout, TimeUnit.MILLISECONDS); if (newByte == null){
	 * error("expecting %d bytes got %d", length, count); return
	 * Arrays.copyOfRange(value, 0, count); } value[count] = newByte; ++count; }
	 * //blocking = false; return value; }
	 */
	/*
	 * public String readString(char delimeter) throws InterruptedException {
	 * StringBuffer value = new StringBuffer(); byte newByte = -1; while
	 * ((newByte = blockingData.take().byteValue()) > 0 && newByte != delimeter)
	 * { value.append(newByte); // FIXME to ascii ? } return value.toString(); }
	 * 
	 * public String readString() throws InterruptedException { return
	 * readString('\n'); }
	 */

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

	/*
	 * 
	 * public byte[] writeAndRead(byte[] send) throws IOException{ return
	 * writeAndRead(send, -1, -1); }
	 * 
	 * // http://pyserial.sourceforge.net/pyserial_api.html
	 * 
	 * public byte[] writeAndRead(byte[] send, int size, int timeoutms) throws
	 * IOException{ blocking = true; write(send);
	 * 
	 * byte[] data = new byte[size]; // < does this work ? // TODO RTFM !!! int
	 * numBytes = read(data); <--- WTF this is int low level !!!! FIX
	 * 
	 * Arrays.copyOfRange(original, from, to) // i don't think gnu rxtx serial
	 * interface blocks "much" // FIXME - return a re-sized byte array // FIXME
	 * - static resize function blocking = false; }
	 */

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
		blockingData.clear();
		blocking = b;
	}

	public void clearBlocking() {
		blockingData.clear();
	}

	@Override
	@Test
	public Status test() {

		// non destructive tests
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done
		Status status = super.test();
		try {

			Runtime.start("gui", "GUIService");
			// Runtime.start("webgui", "WebGUI");

			Serial serial = (Serial) Runtime.start(getName(), "Serial");
			Serial uart = connectToVirtualUART();

			// TEST LATER
			// serial.record();
			// uart.record();

			// TEST LATER
			// ArrayList<String> portNames = serial.getPortNames();
			// info("reading portnames back %s",
			// Arrays.toString(portNames.toArray()));

			if (!serial.isConnected()) {
				throw new IOException(String.format("%s not connected", serial.getName()));
			}

			if (!uart.isConnected()) {
				throw new IOException(String.format("%s not connected", uart.getName()));
			}

			// mimic eddie control board
			// TODO add uart.ifThen("VER\r","OOOD");

			serial.write("VER\r");
			uart.write("000D\r");
			info(serial.readString(5));

			// TEST LATER
			// info("test blocking");
			// serial.setBlocking(false);
			// uart.setBlocking(false);

			uart.write("HELO");
			String helo = serial.readString(5);

			log.info(String.format("read back [%s]", helo));

			// TEST LATER PUB SUB
			info("test publish/subscribe nonblocking");

			for (int i = 255; i > -1; --i) {
				// serial.write(i);
				// serial.write(new int[]{ 1, 2, 3, 4, 5, 6, 7, 8 , 9 , 10, 127,
				// 128, 254, 255});
				serial.write(i);
				uart.readString(1);
				// echo back
				uart.write(i);
				serial.readString(1);
				// uart.write(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
				// 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 });
			}
			// bounds testing
			serial.write(-1);
			serial.write(0);
			serial.write(127);
			serial.write(128);
			serial.write(255);
			serial.write(256);
			serial.write(512);

			uart.write(-1);
			uart.write(0);
			uart.write(127);
			uart.write(128);
			uart.write(255);
			uart.write(256);
			uart.write(512);

			// test blocking

			// clean up.

			// uart.releaseService();
			// serial.releaseService();

		} catch (Exception e) {
			status.addError(e);
		}

		return status;

	}

	public Serial connectToVirtualUART() {
		String name = getName();
		String uartName = String.format("%s_uart", name);
		log.info(String.format("connectToVirtualUART - creating uart %s <--> %s <---> %s <-->", name, name.toUpperCase(), uartName.toUpperCase(), uartName));
		VirtualSerialPort.createNullModemCable(name.toUpperCase(), uartName.toUpperCase());
		Serial uart = (Serial) Runtime.start(uartName, "Serial");
		uart.connect(uartName.toUpperCase());
		connect(name.toUpperCase());
		return uart;
	}

	/*
	 * public void test2() throws IOException {
	 * 
	 * info("creating virtual null modem cable");
	 * 
	 * createNullModemCable("UART1", "VCOM1");
	 * 
	 * info("creating uart"); Serial uart1 = (Serial) Runtime.start("UART1",
	 * "Serial"); Arduino arduino = (Arduino) Runtime.start("arduino",
	 * "Arduino");
	 * 
	 * InMoov i01 = (InMoov) Runtime.start("i01", "InMOov");
	 * 
	 * info("connecting"); if (!uart1.connect("UART1")) { throw new
	 * IOException("cant connect"); } }
	 */
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
