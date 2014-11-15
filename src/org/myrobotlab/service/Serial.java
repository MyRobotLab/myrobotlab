package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
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
	// but sometimes a request / response is desired
	// the buffer supports this - the stream is forked
	// and a buffer
	transient int BUFFER_SIZE = 8192;
	// transient int[] buffer = new int[BUFFER_SIZE];
	transient BlockingQueue<Integer> blockingData = new LinkedBlockingQueue<Integer>();
	transient BlockingQueue<Integer> onByte = new LinkedBlockingQueue<Integer>();

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
	String rxFileName;
	boolean isRXRecording = false;
	transient FileWriter fileWriterRX = null;
	transient BufferedWriter bufferedWriterRX = null;
	String rxFileFormat;

	String txFileName;
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

	// gui has its own counters
	int txCount = 0;
	int rxCount = 0;

	// pretty print = 3 chars e.g 'FF '
	// * number of bytes 8

	public Serial(String n) {
		super(n);
	}

	public void capacity(int size) {
		// buffer = new int[size];
		BUFFER_SIZE = size;
	}

	// ============ conversion begin ========
	/**
	 * converts part of a byte array to a long FIXME - remove this
	 * implementation for the int[] FIXME - support for endianess
	 * 
	 * @param bytes - input
	 * @param offset - offset to begin
	 * @param length - size
	 * @return - converted long
	 */
	public static long bytesToLong(byte[] bytes, int offset, int length) {

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
	public boolean recordRX() {
		return recordRX(null);
	}

	public boolean recordRX(String filename) {
		try {
			info(String.format("record RX %s", filename));

			if (isRXRecording) {
				log.info("already recording");
				return true;
			}

			if (filename == null) {
				rxFileName = String.format("rx.%s.%d.data", getName(), System.currentTimeMillis());
			} else {
				rxFileName = filename;
			}

			if (fileWriterRX == null) {
				fileWriterRX = new FileWriter(rxFileName);
				bufferedWriterRX = new BufferedWriter(fileWriterRX);
			}

			isRXRecording = true;
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean isRXRecording() {
		return isRXRecording;
	}

	public boolean isTXRecording() {
		return isTXRecording;
	}

	public boolean recordTX() {
		return recordTX(null);
	}

	public boolean recordTX(String filename) {
		try {

			info(String.format("record TX %s", filename));

			if (isTXRecording) {
				log.info("already recording");
				return true;
			}

			if (filename == null) {
				txFileName = String.format("tx.%s.%d.data", getName(), System.currentTimeMillis());
			} else {
				txFileName = filename;
			}

			if (rxFileName != null && rxFileName.equals(filename)) {
				fileWriterTX = fileWriterRX;
				bufferedWriterTX = bufferedWriterRX;
			}

			if (fileWriterTX == null) {
				fileWriterTX = new FileWriter(txFileName);
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

	// DEPRECATE ???
	public boolean record() {
		String filename = String.format("rxtx.%s.%d.data", getName(), System.currentTimeMillis());
		return record(filename);
	}
	
	public boolean stopRXRecording(){
		try {
			isRXRecording = false;

			if (fileWriterRX != null) {
				bufferedWriterRX.close();
				fileWriterRX.close();
				fileWriterRX = null;
				bufferedWriterRX = null;
			}
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean stopTXRecording(){
		try {
			isTXRecording = false;

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

	public boolean stopRecording() {
		boolean ret = true;
		ret &= stopRXRecording();
		ret &= stopTXRecording();
		return ret;
	}

	// ============ recording end ========

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

				// log.info("--------begin---------------");
				// jump into loop and process as much as there is
				// good implementation :) - the ---begin-- was useful to see
				// "messages"
				// come in groups of bytes

				// FIXME - use ints not bytes for gods sakes
				// does read() block ? if so - you want to publish byte by byte
				// (cuz we don't want to block)
				// if it does not block - then we can publish an array of ints

				while (serialDevice.isOpen() && (newByte = (serialDevice.read() & 0xff)) > -1) {
					++rxCount;

					// display / debug option ? - mrl message format ?
					// display.append(String.format("%02x ", newByte));

					display.append(String.format("%03d%s", newByte, delimeter));

					// FIXME - delay and read as much as possible (in loop)
					// versus
					// respond immediately and incur overhead + buffer overrun

					if (isRXRecording) {
						// allow change of format 
						//bufferedWriterRX.write(display.toString());
						bufferedWriterRX.write(newByte);
					}

					display.setLength(0);

					// send data to listeners - local callback
					// design decision - removing direct callback
					// in favor of pub/sub framework - to add remote
					// capability as well
					/*
					 * local version
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
	 * blocking & timeout  
	 * InputStream like interface - but regrettably InputStream IS NOT A F#(@!! INTERFACE !!!!
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
		Integer newByte = blockingData.poll();
		return newByte;
	}

	@Override
	public int read(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i){
			data[i] = (byte)read();
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
			if (timeoutMS < 1){
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
	
	// forever blocking function - personally I prefer timeouts
	String readString(int length) throws InterruptedException{
		return readString(length, 0);
	}

	String readString(int length, int timeoutMS) throws InterruptedException {
		int[] bytes = new int[length];
		read(bytes, timeoutMS);
		return new String(intArrayToByteArray(bytes));
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

	@Override
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			++txCount;
			serialDevice.write(data[i] & 0xff);
		}
		
		// optional do not broadcast tx count		
	}

	@Override
	public void write(int data) throws IOException {
		serialDevice.write(data);
	}
	
	@Override
	public void write(int[] data) throws IOException {
		serialDevice.write(data);
	}

	public void writeFile(String filename) {
		try {
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
			
			int timeout = 500;//500 ms serial timeout

			Runtime.start("gui", "GUIService");
			// Runtime.start("webgui", "WebGUI");

			Serial serial = (Serial) Runtime.start(getName(), "Serial");
			Serial uart = createVirtualUART();

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
			String helo = serial.readString(5, timeout);

			log.info(String.format("read back [%s]", helo));

			info("testing blocking");
			
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

			// FIXME - TESTING IS TIME SENSITIVE
			// clear()'ing and counting the queue is done on
			// a seperate thread than the publishing / reading
			// look at the Clock tests
			info("test publish/subscribe nonblocking");
			onByte.clear();
			addByteListener(this);
			uart.write(1);
			/*
			if (onByte.size() != 1){
				throw new IOException("pub / sub did not find byte");
			}
			*/
			log.info("clear");
			sleep(10);
			onByte.clear();
			
			for (int i = 0; i < 256; ++i) {
				uart.write(i);
				//serial.readString(1);
			}
			sleep(100);
			if (onByte.size() != 256){
				error("blah");
			}
			log.info(String.format("size %s", onByte.size()));
			// clean up.

			// uart.releaseService();
			// serial.releaseService();

		} catch (Exception e) {
			status.addError(e);
		}

		return status;

	}
	
	public VirtualNullModemCable createNullModemCable(String port0, String port1){
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
	
	public String getRXFileName(){
		return rxFileName;
	}
	
	public String getTXFileName(){
		return txFileName;	
	}
	
	/**
	 * TODO - blocking at 0 ms does not block forever
	 */

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

	@Override // for testing
	public void onByte(Integer b) {
		log.info(String.format("%d", b));
		onByte.add(b);
	}
}
