package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	static public final String FORMAT_DECIMAL = "decimal";
	static public final String FORMAT_HEX = "hex";
	static public final String FORMAT_ASCII = "ascii";
	String format = FORMAT_DECIMAL;
	String delimeter = " ";

	transient int BUFFER_SIZE = 8192;
	transient int[] buffer = new int[BUFFER_SIZE];
	transient BlockingQueue<Integer> blockingData = new LinkedBlockingQueue<Integer>();

	int recievedByteCount = 0;

	boolean publish = true;
	boolean blocking = false;

	boolean connected = false;
	String portName = null;
	
	int rate = 57600;

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
	 * FIXME - implement - depending on file extention
	 * .asc .dec .bin parse and load into a byte[]
	 * @param filename
	 * @return
	 */
	public byte[] loadFile(String filename){
		return null;
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean sendFile(String filename){
		byte[] ret = loadFile(filename);
		return false;
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
					

					// FIXME - delay and read as much as possible (in loop) versus
					// respond immediately and incur overhead + buffer overrun
					
					if (isRXRecording) {
						bufferedWriterRX.write(display.toString());
					}
					
					display.setLength(0);

					// send data to listeners
					for (int i = 0; i < listeners.size(); ++i) {
						listeners.get(i).onByte(newByte);
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
	
	/*** FIXME FIXME FIXME FIXME - WE NEED METHOD CACHE & BETTER FINDING/RESOLVING/UPCASTING OF METHODS -
	 * THE RESULT IS MAKING HORRIBLE ABOMINATIONS LIKE THIS
	 */
	
	public boolean connect(String name, Double rate, Double databits, Double stopbits, Double parity){
		return connect(name, rate.intValue(), databits.intValue(), stopbits.intValue(), parity.intValue());
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		if (name == null || name.length() == 0) {
			log.info("got emtpy connect name - disconnecting");
			return disconnect();
		}
		try {
			if (serialDevice != null && name.equals(portName) && isConnected()){
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
		return readByteArray(length, 500);
	}
	/**
	 * 
	 * @param length
	 * @param timeout
	 * @return
	 * @throws InterruptedException
	 */
	public int[] readByteArray(int length, int timeout) throws InterruptedException {
		int count = 0;
		int[] value = new int[length];
		Integer newByte = null;
		while (count < length) {
			newByte = blockingData.poll(timeout, TimeUnit.MILLISECONDS);
			if (newByte == null){
				// no "partial" returns
				// either return it all or nothing
				return null;
			}
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
		blockingData.clear();
		blocking = b;
	}
	
	public void clearBlocking(){
		blockingData.clear();
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
	
	public static byte[] intArrayToByteArray(int[] src){
		if (src == null) { return null; }
		byte[] ret = new byte[src.length];
		for (int i = 0; i < src.length; ++i){
			ret[i] = (byte)src[i];
		}
		return ret;
	}

	@Override
	@Test 
	public Status test() {

		// non destructive tests
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done
		Status status = super.test();
		try {
			
			//Runtime.start("gui", "GUIService");
			//Runtime.start("webgui", "WebGUI");
			
			Serial serial = (Serial) Runtime.start(getName(), "Serial");
			Serial uart = connectToVirtualUART();
			
			serial.record();
			uart.record();

			ArrayList<String> portNames = serial.getPortNames();
			info("reading portnames back %s", Arrays.toString(portNames.toArray()));

			if (!serial.isConnected()) {
				throw new IOException(String.format("%s not connected", serial.getName()));
			}

			if (!uart.isConnected()) {
				throw new IOException(String.format("%s not connected", uart.getName()));
			}
			
			info("test blocking");
			serial.setBlocking(true);
			
			uart.write("HELO");
			int[] bytes = serial.readByteArray(4);
			
			String helo = new String(intArrayToByteArray(bytes));
			log.info(String.format("read back [%s]", helo));
			
			info("test publish/subscribe nonblocking");
			info("writing from %s -----> uart", getName());
			
			for (int i = 255; i > -1; --i) {
				//serial.write(i);
				//serial.write(new int[]{ 1, 2, 3, 4, 5, 6, 7, 8 , 9 , 10, 127, 128, 254, 255});
				serial.write(i);
				
				// echo back
				uart.write(i);
				//uart.write(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 });
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
	
	public Serial connectToVirtualUART(){
		String name = getName();
		String uartName = String.format("%s_uart", name);
		log.info(String.format("connectToVirtualUART - creating uart %s <--> %s <---> %s <-->", name, name.toUpperCase(), uartName.toUpperCase(), uartName));
		createNullModemCable(name.toUpperCase(), uartName.toUpperCase());
		Serial uart = (Serial)Runtime.start(uartName, "Serial");
		uart.connect(uartName.toUpperCase());
		connect(name.toUpperCase());
		return uart;
	}

	/*
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
	
	public void releaseService(){
		super.releaseService();
		if (isOpen()){
			disconnect();
		}
		
		stopRecording();
		log.info(String.format("%s closed ports and files", getName()));
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);
			//Runtime.start("gui", "GUIService");
			Serial serial = (Serial) Runtime.start("serial", "Serial");
			// serial.connect("COM15");
			serial.test();
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
