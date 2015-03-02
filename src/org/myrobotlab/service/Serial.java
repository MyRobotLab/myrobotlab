package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecException;
import org.myrobotlab.codec.DecimalCodec;
import org.myrobotlab.codec.CodecFactory;
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

public class Serial extends Service implements SerialDeviceService, SerialDataListener {

	static class Port implements Runnable, SerialDeviceEventListener {
		String type; // serialDevice, socket, android bluetooth,
		String portName;
		transient Serial myService;
		transient OutputStream out;
		transient InputStream in;
		transient Thread listeningThread = null;
		boolean listening = false;

		// hardware serial port details
		Integer rate;
		Integer databits;
		Integer stopbits;
		Integer parity;

		public Port(Serial serial, String portName, InputStream in, OutputStream out) throws IOException {
			this.portName = portName;
			this.myService = serial;
			this.out = out;
			this.in = in;
			listeningThread = new Thread(this, portName);
			listeningThread.start();
		}

		public Port(Serial serial, String name, InputStream inputStream, OutputStream outputStream, int rate, int databits, int stopbits, int parity) throws IOException {
			this(serial, name, inputStream, outputStream);
			this.rate = rate;
			this.databits = databits;
			this.stopbits = stopbits;
			this.parity = parity;
		}

		public void close() {
			listening = false;
			if (listeningThread != null) {
				listeningThread.interrupt();
			}
			listeningThread = null;
			myService.info("closing port %s", portName);
			FileIO.close(in, out);

		}

		/**
		 * reads from Ports input stream and puts it on the Serials main RX line
		 * - to be published and buffered
		 */
		public void run() {
			listening = true;
			try {
				int newByte;
				while (listening && ((newByte = in.read()) != -1)) {
					myService.processRxByte(newByte);
				}
			} catch (Exception e) {
				Logging.logException(e);
			} finally {
				close();
			}
		}

		@Override
		public void serialEvent(SerialDeviceEvent ev) {
			// TODO Auto-generated method stub

		}

		public void write(int b) throws IOException {
			out.write(b);
		}
	}

	public static int bytesToInt(int[] bytes, int offset, int length) {

		int retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= ((int) bytes[offset + i] & 0xFF);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
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

	static public VirtualNullModemCable createNullModemCable(String port0, String port1) {
		return VirtualSerialPort.createNullModemCable(port0, port1);
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

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	/**
	 * list of portnames on the system
	 */
	ArrayList<String> portNames = new ArrayList<String>();

	/**
	 * rx data format - to be written to file - a null formatter writes raw
	 * binary
	 */
	transient Codec rxFormatter = new DecimalCodec();
	/**
	 * tx data forrmat - to be written to file - a null formatter writes raw
	 * binary
	 */
	transient Codec txFormatter = new DecimalCodec();

	/**
	 * blocking and non-blocking publish/subscribe reading is possible at the
	 * same time. If blocking is not used then the internal buffer will fill to
	 * the BUFFER_SIZE and just be left - overrun data will be lost
	 */
	transient int BUFFER_SIZE = 1024;

	/**
	 * blocking queue for blocking rx read requests
	 */
	transient BlockingQueue<Integer> blockingRX = new LinkedBlockingQueue<Integer>();

	/**
	 * our set of ports to multiplex tx data and demux rx data
	 */
	transient HashMap<String, Port> ports = new HashMap<String, Port>();

	/**
	 * used as the "default" port - now that Serial can multiplex with multiple
	 * ports - the default is used for methods which are not explicit ... e.g.
	 * connect(), disconnect() etc.. are now equivalent to connect(portName),
	 * disconnect(portName)
	 */
	String portName = null;

	// ============ conversion end ========

	// ============ recording begin ========

	// int rate = 57600;
	// ====== recording file io begin ======
	transient FileOutputStream fileRX = null;

	transient FileOutputStream fileTX = null;

	// gui has its own counters
	int txCount = 0;

	int rxCount = 0;

	/**
	 * For the purpose of efficient testing only - it "catches" published bytes
	 */
	ArrayList<Integer> testOnByte = new ArrayList<Integer>();

	public Serial(String n) {
		super(n);
	}

	public void addByteListener(SerialDataListener service) {
		addListener("publishRX", service.getName(), "onByte", Integer.class);
	}

	@Override
	public int available() {
		return blockingRX.size();
	}

	// ============ recording end ========

	public void clear() {
		blockingRX.clear();
	}

	/**
	 * "Default" connect - connects to a hardware serial port with default
	 * parameters
	 * 
	 * FIXME - you'll have to decide if connecting to null is closing...
	 */
	@Override
	public boolean connect(String name) {
		return connect(name, 57600, 8, 1, 0);
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
		if (ports.containsKey(name)) {
			error("%s already connected - disconnect first", name); // FIXME -
																	// this is
																	// contractual
																	// - must
																	// remove if
																	// disconnect
																	// then...
			return true;
		}

		// //// TODO WORK FROM HERE ////// - FIXME for backward compatibility
		/*
		 * this aint gonna work :P if (name == null || name.length() == 0) {
		 * log.info("got emtpy connect name - disconnecting"); return
		 * disconnect(); }
		 */
		// backwards compatibility - don't need to be explicit about the port
		// your connected too
		try {
			/*
			 * if (serialDevice != null && name.equals(portName) &&
			 * isConnected()) { log.info(String.format(
			 * "connect to already connected port - setting parameters rate=%d",
			 * rate)); serialDevice.setParams(rate, databits, stopbits, parity);
			 * save(); this.rate = rate; broadcastState(); return true; }
			 */

			SerialDevice serialDevice = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (serialDevice != null) {
				/*
				 * if (!serialDevice.isOpen()) { serialDevice.open();
				 * //serialDevice.addEventListener(this);
				 * //serialDevice.notifyOnDataAvailable(true); sleep(500); //
				 * changed from 1000 ms to 500 ms }
				 */

				// serialDevice.setParams(rate, databits, stopbits, parity);
				// this.rate = rate;
				// setting default port
				portName = name; // ??? set to name :P
				Port port = new Port(this, name, serialDevice.getInputStream(), serialDevice.getOutputStream(), rate, databits, stopbits, parity);
				ports.put(name, port);
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

	/**
	 * FIXME - implement connects to a FilePlayer details of tx/rx and timing
	 * can be part os a SerialFilePlayer implementation
	 * 
	 * @param name
	 */
	public void connectFilePlayer(String name) throws IOException {
		//
	}

	/**
	 * FIXME - implement Baddass loopback null/modem cable - auto creates a new
	 * Serial service and connects to it FIXME - no need for null/modem cable
	 * virtual port ?
	 * 
	 * @param name
	 */
	public void connectLoopback(String name) throws IOException {
		//
		log.info("implement me");
	}

	/**
	 * connect to a tcp/ip socket - allows serial communication over socket
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void connectTCP(String host, int port) throws IOException {
		info("connectTCP %s %d", host, port);
		Socket socket = new Socket(host, port);
		String portName = String.format("%s.%s", getName(), socket.getRemoteSocketAddress().toString());
		ports.put(portName, new Port(this, portName, socket.getInputStream(), socket.getOutputStream()));
		this.portName = portName;
		broadcastState();
	}

	// FIXME - rename connectVirtualUART???
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

	@Override
	public void disconnect() {
		if (portName == null) {
			log.info("already disconnected");
			return;
		}
		if (!ports.containsKey(portName)) {
			log.warn("%s not open", portName);
			return;
		}

		Port port = ports.get(portName);
		port.close();
		ports.remove(portName);
		portName = null;
		broadcastState();
	}

	public void disconnect(String name) {

		if (!ports.containsKey(name)) {
			log.warn("%s not open", name);
			return;
		}

		Port port = ports.get(name);
		port.close();
		ports.remove(name);
		portName = null;
		broadcastState();
	}

	public void disconnectAll() {
		info("disconnecting all ports");
		for (String portName : ports.keySet()) {
			Port port = ports.get(portName);
			port.close();
		}
		portName = null;
		broadcastState();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control", "sensor", "microcontroller" };
	}

	/**
	 * ------ publishing points begin -------
	 */

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

	/**
	 * ------ publishing points end -------
	 */

	/**
	 * all possible ports - inclusive of the ones this serial
	 * service has created
	 */
	@Override
	public ArrayList<String> getPortNames() {
		HashSet<String> sort = new HashSet<String>(ports.keySet());
		ArrayList<String> hwports = SerialDeviceFactory.getSerialDeviceNames();
		for (int i = 0; i < hwports.size(); ++i){
			sort.add(hwports.get(i));
		}
		
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(sort);
		
		Collections.sort(ret);
		return ret;
	}

	/**
	 * -------- blocking reads begin --------
	 */

	public int getRxCount() {
		return rxCount;
	}

	/**
	 * -------- blocking reads begin --------
	 */

	public boolean isConnected() {
		return portName != null || ports.size() > 0;
	}

	public boolean isConnected(String name) {
		if (name == null || !ports.containsKey(name)) {
			return false;
		}
		return true;
	}

	public boolean isRecording() {
		return (fileRX != null || fileTX != null);
	}

	/**
	 * onByte is typically the functions clients of the Serial service use when
	 * they want to consume serial data.
	 * 
	 * The serial service implements this function primarily so it can test
	 * itself
	 * 
	 * readFromPublishedByte is a catch mechanism to verify tests
	 */
	@Override
	public void onByte(Integer b) {
		log.info(String.format("%d", b));
		synchronized (testOnByte) {
			testOnByte.add(b);
			testOnByte.notify();
		}
	}

	/**
	 * Process the incoming de-muxed byte from one of the input streams Possible
	 * optimization is to inline this within the Port thread
	 * 
	 * @param newByte
	 * @throws IOException
	 * @throws CodecException 
	 */
	public final void processRxByte(int newByte) throws IOException, CodecException {
		newByte = newByte & 0xff;
		++rxCount;

		// publish the rx byte !
		invoke("publishRX", newByte);

		if (blockingRX.size() < BUFFER_SIZE) {
			blockingRX.add(newByte);
		}

		// FILE I/O
		if (fileRX != null) {
			if (rxFormatter != null) {
				fileRX.write(rxFormatter.decode(newByte).getBytes());
			} else {
				fileRX.write(newByte);
			}
		}
	}

	/**
	 * main line RX publishing point
	 * 
	 * @param data
	 * @return
	 */
	public int publishRX(Integer data) {
		return data;
	}

	/**
	 * main line TX publishing point
	 * 
	 * @param display
	 * @return
	 */
	public Integer publishTX(Integer data) {
		return data;
	}

	// ============= write methods begin ====================

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
			Integer newByte = blockingRX.take();
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
				newByte = blockingRX.take();
			} else {
				newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
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
		// int[] bytes = new int[length];
		// int count = read(bytes, timeoutMS);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int count = 0;
		Integer newByte = null;
		while (count < length) {
			if (timeoutMS < 1) {
				newByte = blockingRX.take();
			} else {
				newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
			}
			if (newByte == null) {
				if (count == 0) {
					error("got nothing!");
					return null;
				} else {
					error("expecting %d bytes got %d", length, count);
					break;
				}
			}
			// data[count] = newByte;
			bytes.write((byte) newByte.intValue());
			++count;
		}
		// bytes.close();
		return new String(bytes.toByteArray());
	}

	// FIXME remove blocking public
	// FIXME overload with timeouts etc - remove exposed blocking
	// FIXME - implement
	public byte[] readToDelimiter(String delimeter) {
		return null;
	}

	public boolean record() {
		String filename = String.format("rxtx.%s.%d.data", getName(), System.currentTimeMillis());
		return record(filename);
	}

	public boolean record(String filename) {
		boolean ret = true;
		// String ext = getExtention(rxFormatter);
		ret &= recordTX(String.format("%s.tx.%s", filename, txFormatter.getCodecExt()));
		ret &= recordRX(String.format("%s.rx.%s", filename, rxFormatter.getCodecExt()));
		return ret;
	}

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

	public void releaseService() {
		super.releaseService();
		disconnectAll();
		stopRecording();
	}

	public void reset() {
		blockingRX.clear();
		rxCount = 0;
		txCount = 0;
	}

	public void setBufferSize(int size) {
		BUFFER_SIZE = size;
	}

	/**
	 * uses key ascii, decimal, hex, arduino ... to dynamically set file
	 * formatter
	 * 
	 * @param key
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void setFormat(String key) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		setRXFormatter(CodecFactory.getDecoder(key));
		setTXFormatter(CodecFactory.getDecoder(key));
		broadcastState();
	}

	public void setRXFormatter(Codec formatter) {
		rxFormatter = formatter;
	}

	public void setTXFormatter(Codec formatter) {
		txFormatter = formatter;
	}

	public void stopRecording() {
		FileIO.close(fileRX);
		FileIO.close(fileTX);
		broadcastState();
	}

	public void stopService() {
		super.stopService();
		stopRecording();
	}

	@Override
	public Status test() {

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

			// Runtime.start("gui", "GUIService");
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
			String data = "HELLO";
			uart.write(data);
			String helo = serial.readString(data.length(), timeout);

			if (!data.equals(helo)) {
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

			// support write(int) kill pill or not ?
			// I say yes
			info("testing blocking");
			for (int i = 255; i > -1; --i) {
				serial.write(i);
				int readBack = uart.read();
				log.info(String.format("written %d read back %d", i, readBack));
				if (i < 256 && i > -1) {
					if (readBack != i) {
						status.addError("read back not the same as written for value %d %d !", i, readBack);
					}
				}
			}

			// FIXME - test the -1 write(int) kill pill
			// serial.write(-1) -> should close port !!!

			// in the real world we don't know when the sender to
			// our receiver is done - so we'll sleep here
			sleep(300);
			info("clear buffers");
			serial.clear();
			uart.clear();

			// test publish/subscribe nonblocking
			addByteListener(this);
			uart.write(64);

			synchronized (testOnByte) {
				log.info("started wait");
				testOnByte.wait(1000);
				log.info("finished wait");
			}

			if (testOnByte.size() != 1) {
				status.addError("wrong size %d returned", testOnByte.size());
			}

			if (testOnByte.get(0) != 64) {
				status.addError("wrong size returned published");
			}

			serial.stopRecording();
			uart.stopRecording();

			// ======= decimal format begin ===========
			serial.setFormat("decimal");
			uart.setFormat("decimal");

			// default non-binary format is ascii decimal
			serial.record("decimal.2");
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
			serial.setFormat("hex");
			serial.record("hex.3");
			// uart.record("test/Serial/uart.3");
			serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
			sleep(30);
			serial.broadcastState();
			serial.stopRecording();
			// uart.stopRecording();
			// ======= hex format begin ===========

			// parsing of files based on extension check

			// TODO flush & close tests ?
			serial.disconnect();
			uart.disconnect();

			if (status.hasError()) {
				log.error("we have an error!");
			}

			log.info(status.flatten().toString());

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}

	// FIXME - write(byte[] buff, int offset, int len) - OutputStream signature
	@Override
	public void write(byte[] data) throws IOException, CodecException {
		for (int i = 0; i < data.length; ++i) {
			write(data[i] & 0xFF);
		}
	}

	@Override
	public void write(int data) throws IOException, CodecException {
		// int newByte = data & 0xFF;

		for (String portName : ports.keySet()) {
			Port port = ports.get(portName);
			port.write(data);
		}

		// main line TX
		invoke("publishTX", data);

		++txCount;
		if (fileTX != null) {
			if (txFormatter != null) {
				fileTX.write(txFormatter.decode(data).getBytes());
			} else {
				fileTX.write(data);
			}
		}
	}

	// ============= write methods begin ====================
	@Override
	public void write(String data) throws IOException, CodecException {
		write(data.getBytes());
	}

	// FIXME - changer Formatters based on file extension !!!
	// file (formatter/parser) --to--> tx
	public void writeFile(String filename) {
		try {

			byte[] fileData = FileIO.fileToByteArray(new File(filename));

			if (txFormatter != null) {
				// FIXME parse the incoming file
				for (int i = 0; i < fileData.length; ++i) {
					// FIXME - determine what is needed / expected to parse
					// write(txFormatter.parse(fileData[i]));
				}
			} else {
				for (int i = 0; i < fileData.length; ++i) {
					write(fileData[i]);
				}
			}

		} catch (Exception e) {
			error(e);
		}
	}

	public static void main(String[] args) {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Runtime.start("gui", "GUIService");
			Serial serial = (Serial) Runtime.start("serial", "Serial");
			serial.setFormat("arduino");
			serial.createVirtualUART();
			//serial.connectTCP("localhost", 9090);

			// serial.connect("COM15");
			//serial.test();
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
