package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecOutputStream;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.Port;
import org.myrobotlab.serial.PortQueue;
import org.myrobotlab.serial.PortSource;
import org.myrobotlab.serial.PortStream;
import org.myrobotlab.service.interfaces.QueueSource;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Serial extends Service implements PortSource, QueueSource, SerialDataListener {

	/**
	 * general read timeout - 0 is infinite > 0 is number of milliseconds to
	 * wait up to, until data is returned timeout = null: wait forever timeout =
	 * 0: non-blocking mode (return immediately on read) timeout = x: set
	 * timeout to x milliseconds
	 */
	private Integer timeoutMS = null;

	private static final long serialVersionUID = 1L;

	/**
	 * hardware library for non-Android
	 */
	final public static String HARDWARE_LIBRARY_RXTX = "org.myrobotlab.serial.PortRXTX";

	/**
	 * different hardware library - hotspot only
	 */
	final public static String HARDWARE_LIBRARY_JSSC = "org.myrobotlab.serial.PortJSSC";

	/**
	 * Android only bluetooth library
	 */
	final public static String HARDWARE_LIBRARY_ANDROID_BLUETOOTH = "android.somethin";

	public final static Logger log = LoggerFactory.getLogger(Serial.class);

	/**
	 * cached list of portnames on the system
	 */
	static HashSet<String> portNames = new HashSet<String>();

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
	 * our set of ports we have access to. This is a shared resource between ALL
	 * serial services. It should be possible simply to iterate through this
	 * list to get all (cached) names .. operating system ports may have changed
	 * and need re-querying
	 * 
	 * it also might be worthwhile to keep a "static" list for remote and
	 * virtual ports so that remote and other services can have access to that
	 * list
	 * 
	 * has to be transient because many Ports are not serializable
	 * 
	 * remote manipulations and identification should always be done through
	 * portNames
	 */
	static transient HashMap<String, Port> ports = new HashMap<String, Port>();

	/**
	 * all the ports we are currently connected to typically there is 0 to 1
	 * connected ports - however the serial service has the ability to "fork"
	 * ports where it is connected to 2 or more ports simultaneously
	 */
	transient HashMap<String, Port> connectedPorts = new HashMap<String, Port>();

	/**
	 * used as the "default" port - now that Serial can multiplex with multiple
	 * ports - the default is used for methods which are not explicit ... e.g.
	 * connect(), disconnect() etc.. are now equivalent to connect(portName),
	 * disconnect(portName)
	 */
	String portName = null;

	/**
	 * "the" port - there is only one although we can fork and multiplex others.
	 * This is the port which we determine if this Serial service is connected
	 * or not
	 */
	transient Port port = null;

	/**
	 * we need to dynamically load our preferred hardware type, because we may
	 * want to change it or possibly the platform does not support it. When it
	 * is null - we will let MRL figure out what is best.
	 */
	String hardwareLibrary = null;

	/**
	 * rx files saved to an output stream
	 */
	transient CodecOutputStream outRX = new CodecOutputStream("rx", this);

	/**
	 * tx bytes saved to an output stream
	 */
	transient CodecOutputStream outTX = new CodecOutputStream("tx", this);

	/**
	 * number of tx bytes
	 */
	int txCount = 0;

	/**
	 * number of received bytes
	 */
	int rxCount = 0;

	/**
	 * default bps
	 */
	int baudrate = 57600;

	/**
	 * default databits
	 */
	int databits = 8;

	/**
	 * default stopbits
	 */
	int stopbits = 1;

	/**
	 * default parity
	 */
	int parity = 0;

	/**
	 * list of RX listeners - if "local" they will be immediately called back by
	 * the serial device's thread when data arrives, if they are "remote" they
	 * should be published to. They can subscribe to the publishRX method when a
	 * lister is added. Serial is the first listener added to this map
	 */
	transient HashMap<String, SerialDataListener> listeners = new HashMap<String, SerialDataListener>();

	/**
	 * conversion utility TODO - support endianess
	 * 
	 * @param bytes
	 * @param offset
	 * @param length
	 * @return
	 */
	public static int bytesToInt(int[] bytes, int offset, int length) {
		return (int) bytesToLong(bytes, offset, length);
	}

	/**
	 * conversion utility TODO - support endianess
	 * 
	 * @param bytes
	 * @param offset
	 * @param length
	 * @return
	 */
	public static long bytesToLong(int[] bytes, int offset, int length) {

		long retVal = 0;

		for (int i = 0; i < length; ++i) {
			retVal |= (bytes[offset + i] & 0xff);
			if (i != length - 1) {
				retVal <<= 8;
			}
		}

		return retVal;
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

	public Serial(String n) {
		super(n);
		listeners.put(n, this);
	}

	public void addByteListener(SerialDataListener listener) {
		addByteListener(listener.getName());
	}

	/**
	 * awesome method - which either sets up the pub/sub remote or assigns a
	 * local reference from the publishing thread
	 * 
	 * good pattern in that all logic is in this method which uses a string
	 * "name" parameter - addByteListener(SerialDataListener listener) will call
	 * this method too rather than implementing its own local logic
	 * 
	 * @param name
	 */
	public void addByteListener(String name) {
		ServiceInterface si = Runtime.getService(name);

		// if (si instanceof SerialDataListener && si.isLocal()){
		if (SerialDataListener.class.isAssignableFrom(si.getClass()) && si.isLocal()) {
			// direct callback
			listeners.put(si.getName(), (SerialDataListener) si);
		} else {
			// pub sub
			addListener("publishRX", si.getName(), "onByte", Integer.class);
			addListener("publishConnect", si.getName(), "onConnect", String.class);
			addListener("publishDisconnect", si.getName(), "onDisconnect", String.class);
		}
	}

	/**
	 * method similar to InputStream's
	 * 
	 * @return
	 */
	public int available() {
		return blockingRX.size();
	}

	/**
	 * clears the rx buffer
	 */
	public void clear() {
		blockingRX.clear();
		outRX.clear();
		outTX.clear();
	}

	public boolean connect(String name) {
		return connect(name, baudrate, databits, stopbits, parity);
	}

	/**
	 * The main simple connect - it attempts to connect to one of the known
	 * ports in memory if that fails it will try to connect to a hardware port
	 * 
	 * TODO - make "connecting" to pre-existing ports re-entrant !!!
	 * 
	 * connect - optimized to have a SerialDataListener passed in - which will
	 * optimize data streaming back from the port.
	 * 
	 * preference is to have this local optimizaton
	 * 
	 * connect = open + listen
	 * 
	 * @param inPortName
	 * @param listener
	 * @return
	 * @throws Exception
	 */
	public boolean connect(String inPortName, int baudrate, int databits, int stopbits, int parity) {
		try {
			info("connect to port %s %d|%d|%d|%d", inPortName, baudrate, databits, stopbits, parity);
			this.baudrate = baudrate;
			this.databits = databits;
			this.stopbits = stopbits;
			this.parity = parity;

			// two possible logics to see if we are connected - look at the
			// state of the port
			// on the static resource - or just check to see if its on the
			// "connectedPort" set

			// #1 check to see if were already connected a port 
			//if (this.portName != null && this.portName.equals(portName) && ports.containsKey(portName)) {
			if (this.portName != null) {
				Port port = ports.get(portName);
				if (port.isOpen() && port.isListening()) {
					info("already connected to %s - disconnect", portName);
					return true;
				}
			}

			// #2 connect to a pre-existing
			if (ports.containsKey(inPortName)) {
				connectPort(ports.get(inPortName), null);
				return true;
			}

			// #3 we dont have an existing port - so we'll try a hardware port
			// connect at defaullt parameters - if you need custom parameters
			// create the hardware port first
			Port port = createHardwarePort(inPortName, baudrate, databits, stopbits, parity);
			if (port == null) {
				return false;
			}

			connectPort(port, null);
			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}

	public boolean connect(String name, SerialDataListener listener) {
		return connect(name, baudrate, databits, stopbits, parity);
	}

	/**
	 * FIXME - implement connects to a FilePlayer details of tx/rx and timing
	 * can be part os a SerialFilePlayer implementation
	 * 
	 * @param name
	 */
	public boolean connectFilePlayer(String name) {
		//
		return false;
	}

	/**
	 * FIXME - implement Baddass loopback null/modem cable - auto creates a new
	 * Serial service and connects to it FIXME - no need for null/modem cable
	 * virtual port ?
	 * 
	 * @param name
	 */
	public boolean connectLoopback(String name) {
		// TODO - implement
		log.info("implement me");
		return false;
	}

	public Port connectPort(Port newPort, SerialDataListener listener) throws IOException {
		port = newPort;
		portName = port.getName();
		ports.put(port.getName(), port);
		portNames.add(port.getName());
		invoke("getPortNames");

		if (listener != null) {
			listeners.put(listener.getName(), listener);
		}
		port.listen(listeners);
		port.open();
		connectedPorts.put(portName, newPort);

		// invoking remote & local onConnect
		invoke("publishConnect", port.getName());
		for (String key : listeners.keySet()) {
			// NOT A GOOD OPTIMIZATION - AS THE "EVENT" IS MUCH MORE IMPORTANT
			// THAN THE SPEED OF THE DATA
			// listeners.get(key).onConnect(portName);
			send(listeners.get(key).getName(), "onConnect", portName);
		}

		// save(); why?
		broadcastState();
		return port;
	}

	public boolean connectTCP(String host, int port) throws IOException {
		Port tcpPort = createTCPPort(host, port, this);
		connectPort(tcpPort, this);
		return true;
	}

	/**
	 * for a virtual UART to create a unopened port available for connection. it
	 * will connect itself to one end of the twisted buffer pair
	 * 
	 * @param portName
	 * @param myPort
	 * @throws IOException
	 */
	public String connectVirtualNullModem(String newPortName) throws IOException {

		BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
		BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

		// create other end of null modem cable
		// which MRL Services can connect to
		Port newPort = new PortQueue(newPortName, right, left);
		ports.put(newPortName, newPort);

		// add our virtual port
		String uartPortName = String.format("%s_uart", newPortName);
		PortQueue vPort = new PortQueue(uartPortName, left, right);
		connectPort(vPort, this);

		info(String.format("created virtual null modem cable %s <--> %s", newPortName, uartPortName));
		return newPortName;
	}

	/**
	 * connecting to a virtual UART allows a Serial service to interface with a
	 * mocked hardware. To do this a Serial service creates 2 stream ports and
	 * twists the virtual cable between them.
	 * 
	 * A virtual port is half a virtual pipe, and if unconnected - typically is
	 * not very interesting...
	 * 
	 * @param listener
	 * @return
	 * @throws IOException
	 */

	public Serial connectVirtualUART(String myPort, String uartPort) throws IOException {

		// get port names
		if (myPort == null) {
			myPort = getName();
		}

		if (uartPort == null) {
			uartPort = String.format("%s_uart", myPort);
		}

		BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
		BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

		/*
		 * if (listener != null) { listeners.put(listener.getName(), listener);
		 * }
		 */

		// add our virtual port
		PortQueue vPort = new PortQueue(myPort, left, right);
		ports.put(myPort, vPort);
		// connectPort(vPort, this);

		// create & connect virtual uart
		Serial uart = (Serial) Runtime.start(uartPort, "Serial");
		PortQueue uPort = new PortQueue(uartPort, right, left);
		uart.connectPort(uPort, uart);

		log.info(String.format("connectToVirtualUART - creating uart %s <--> %s", myPort, uartPort));
		return uart;
	}

	/**
	 * Dynamically create a hardware port - this method is needed to abtract
	 * away the specific hardware library. Its advantageous to have abstraction
	 * when interfacing with a specific implementation (JNI/JNA - other?). The
	 * abstraction allows the service to attempt to choose the correct library
	 * depending on platform or personal user choice.
	 * 
	 * We don't want the whole Serial service to explode because of an Import of
	 * an implementation which does not exist on a specific platform. I know
	 * this from experience :)
	 */

	public Port createHardwarePort(String name, int rate, int databits, int stopbits, int parity) {
		log.info(String.format("creating %s port %s %d|%d|%d|%d", hardwareLibrary, name, rate, databits, stopbits, parity));
		try {

			hardwareLibrary = getHardwareLibrary();

			Class<?> c = Class.forName(hardwareLibrary);
			Constructor<?> constructor = c.getConstructor(new Class<?>[] { String.class, int.class, int.class, int.class, int.class });
			Port hardwarePort = (Port) constructor.newInstance(name, rate, databits, stopbits, parity);

			info("created  port %s %d|%d|%d|%d - goodtimes", name, rate, databits, stopbits, parity);
			ports.put(name, hardwarePort);
			return hardwarePort;

		} catch (Exception e) {
			error(e.getMessage());
			Logging.logError(e);
		}

		return null;
	}

	public Port createTCPPort(String host, int tcpPort, SerialDataListener listener) throws IOException {
		info("connectTCP %s %d", host, tcpPort);
		@SuppressWarnings("resource")
		Socket socket = new Socket(host, tcpPort);
		String portName = String.format("%s.%s", getName(), socket.getRemoteSocketAddress().toString());
		Port socketPort = new PortStream(portName, socket.getInputStream(), socket.getOutputStream());
		ports.put(portName, socketPort);
		return socketPort;
	}

	public PortQueue createVirtualPort(String name) {
		BlockingQueue<Integer> rx = new LinkedBlockingQueue<Integer>();
		BlockingQueue<Integer> tx = new LinkedBlockingQueue<Integer>();
		PortQueue portQueue = new PortQueue(name, rx, tx);
		ports.put(name, portQueue);
		return portQueue;
	}

	public Serial createVirtualUART() throws IOException {
		return connectVirtualUART(null, null);
	}

	/**
	 * decode relies on the rx codec decode method which will block a thread
	 * until the out rx stream data buffer decodes a message which can be
	 * returned
	 * 
	 * @return
	 */
	public String decode() {
		Codec codec = outRX.getCodec();
		if (codec != null) {
			return codec.decode();
		}
		return null;
	}

	/**
	 * disconnect = close + remove listeners all ports on serial network
	 */
	public void disconnect() {
		if (!connectedPorts.containsKey(portName)) {
			info("disconnect unknown port %s", portName);
		}

		if (portName == null) {
			info("already disconnected");
			return;
		}

		// remote published disconnect
		invoke("publishDisconnect", port.getName());

		// local disconnect
		for (String key : listeners.keySet()) {
			// DUMB OPTIMIZATION - THE EVENT IS FAR MORE IMPORTANT THAN THE
			// SPEED OF THE DATA
			// listeners.get(key).onDisconnect(portName);
			send(listeners.get(key).getName(), "onDisconnect", port.getName());
		}

		info("disconnecting all ports");
		// forked ports
		for (String portName : connectedPorts.keySet()) {
			Port port = connectedPorts.get(portName);
			port.close();
		}

		connectedPorts.clear();

		portName = null;
		port = null;
		broadcastState();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control", "sensor", "microcontroller" };
	}

	@Override
	public String getDescription() {
		return "reads and writes serial data";
	}

	public String getHardwareLibrary() {
		// if user has forced a specific library
		// use it - customer is always right !!!
		if (hardwareLibrary != null) {
			return hardwareLibrary;
		}

		// otherwise make a "best" guess
		Platform platform = Platform.getLocalInstance();
		if (platform.isDalvik()) {
			return HARDWARE_LIBRARY_ANDROID_BLUETOOTH;
		} else {
			return HARDWARE_LIBRARY_RXTX;
		}
	}

	public HashMap<String, SerialDataListener> getListeners() {
		return listeners;
	}

	public Port getPort() {
		return port;
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
	 * "all" currently known ports - if something is missing refresh ports
	 * should be called to force hardware search
	 * 
	 * @throws ClassNotFoundException
	 */
	@Override
	public List<String> getPortNames() {
		return new ArrayList<String>(portNames);
	}

	PortSource getPortSource() {
		try {
			hardwareLibrary = getHardwareLibrary();
			log.info(String.format("loading class: %s", hardwareLibrary));
			Class<?> c = Class.forName(getHardwareLibrary());
			return (PortSource) c.newInstance();
		} catch (Exception e) {
			Logging.logError(e);
		}

		return null;
	}

	@Override
	public BlockingQueue<?> getQueue() {
		return blockingRX;
	}

	public Codec getRXCodec() {
		return outRX.getCodec();
	}

	public String getRXCodecKey() {
		return outRX.getKey();
	}

	public int getRXCount() {
		return rxCount;
	}

	public int getTimeout() {
		return timeoutMS;
	}

	public Codec getTXCodec() {
		return outTX.getCodec();
	}

	public String getTXCodecKey() {
		return outTX.getKey();
	}

	public boolean isConnected() {
		return portName != null;
	}

	public boolean isRecording() {
		boolean ret = (outRX != null && outRX.getOut() != null) || (outTX != null && outTX.getOut() != null);
		return ret;
	}

	/**
	 * onByte is typically the functions clients of the Serial service use when
	 * they want to consume serial data.
	 * 
	 * The serial service implements this function primarily so it can test
	 * itself
	 * 
	 * readFromPublishedByte is a catch mechanism to verify tests
	 * 
	 * @throws IOException
	 */
	@Override
	public final Integer onByte(Integer newByte) throws IOException {
		newByte = newByte & 0xff;
		++rxCount;

		// publish the rx byte !
		invoke("publishRX", newByte);

		if (blockingRX.size() < BUFFER_SIZE) {
			blockingRX.add(newByte);
		}

		// FILE I/O
		outRX.write(newByte);

		return newByte;
	}

	@Override
	public String onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		return portName;
	}

	@Override
	public String onDisconnect(String portName) {
		info("%s disconnected from %s", getName(), portName);
		return portName;
	}

	/**
	 * successful connection event
	 * 
	 * @param portName
	 * @return
	 */
	public String publishConnect(String portName) {
		info("%s publishConnect %s", getName(), portName);
		return portName;
	}

	/**
	 * disconnect event
	 * 
	 * @param portName
	 * @return
	 */
	public String publishDisconnect(String portName) {
		return portName;
	}

	/**
	 * event to return list of ports of all ports this serial service can see
	 * 
	 * @param portNames
	 * @return
	 */
	public List<String> publishPortNames(List<String> portNames) {
		return portNames;
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
	 * @param temp
	 * @return
	 * @throws IOException
	 */
	public int read() throws IOException, InterruptedException {

		if (timeoutMS == null) {
			return blockingRX.take();
		}

		Integer newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
		if (newByte == null) {
			String error = String.format("%d ms timeout was reached - no data", timeoutMS);
			error(error);
			throw new IOException(error);
		}

		return newByte;
	}

	public int read(byte[] data) throws IOException, InterruptedException {
		for (int i = 0; i < data.length; ++i) {
			data[i] = (byte) read();
		}
		return data.length;
	}

	/**
	 * Read size bytes from the serial port. If a timeout is set it may return
	 * less characters as requested. With no timeout it will block until the
	 * requested number of bytes is read.
	 * 
	 * @param size
	 * @return
	 * @throws InterruptedException
	 */
	public byte[] read(int length) throws InterruptedException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int count = 0;
		Integer newByte = null;
		while (count < length) {
			if (timeoutMS == null) {
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
			bytes.write(newByte.intValue() & 0xff);
			++count;
		}
		return bytes.toByteArray();
	}

	public int read(int[] data) throws InterruptedException {
		int count = 0;
		Integer newByte = null;
		while (count < data.length) {
			if (timeoutMS == null) {
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

	public byte[] readLine() throws InterruptedException {
		return readLine('\n');
	}

	public byte[] readLine(char deliminater) throws InterruptedException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Integer newByte = null;
		while (newByte == null || newByte != deliminater) {
			if (timeoutMS == null) {
				newByte = blockingRX.take();
			} else {
				newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
			}
			if (newByte == null) {
				info("non blocking got nothing");
				return bytes.toByteArray();
			}
			bytes.write(newByte.intValue() & 0xff);
		}
		return bytes.toByteArray();
	}

	public String readString() throws InterruptedException {
		byte[] bytes = readLine('\n');
		return new String(bytes);
	}

	public String readString(char delimiter) throws InterruptedException {
		byte[] bytes = readLine(delimiter);
		return new String(bytes);
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
	public String readString(int length) throws InterruptedException {
		byte[] bytes = read(length);
		return new String(bytes);
	}

	// FIXME remove blocking public
	// FIXME overload with timeouts etc - remove exposed blocking
	// FIXME - implement
	public byte[] readToDelimiter(String delimeter) {
		return null;
	}

	public void record() throws FileNotFoundException {
		String filename = String.format("rxtx.%s.%d.data", getName(), System.currentTimeMillis());
		record(filename);
	}

	public void record(String filename) throws FileNotFoundException {
		recordTX(String.format("%s.tx.%s", filename, outTX.getCodecExt()));
		recordRX(String.format("%s.rx.%s", filename, outRX.getCodecExt()));
	}

	public void recordRX(String filename) throws FileNotFoundException {
		outRX.record(filename);
	}

	public void recordTX(String filename) throws FileNotFoundException {
		outTX.record(filename);
	}

	/**
	 * force refreshing ports
	 * 
	 * @return
	 */
	public List<String> refresh() {

		// all current ports
		portNames.addAll(ports.keySet());

		// plus hardware ports
		PortSource portSource = getPortSource();
		if (portSource != null) {
			List<String> osPortNames = portSource.getPortNames();
			for (int i = 0; i < osPortNames.size(); ++i) {
				portNames.add(osPortNames.get(i));
			}
		}

		broadcastState();
		return new ArrayList<String>(portNames);
	}

	public void removeByteListener(SerialDataListener listener) {
		removeByteListener(listener.getName());
	}

	public void removeByteListener(String name) {
		ServiceInterface si = Runtime.getService(name);

		// if (si instanceof SerialDataListener && si.isLocal()){
		if (SerialDataListener.class.isAssignableFrom(si.getClass()) && si.isLocal()) {
			// direct callback
			listeners.remove(si.getName());
		} else {
			// pub sub
			removeListener("publishRX", si.getName(), "onByte", Integer.class);
			removeListener("publishConnect", si.getName(), "onConnect", String.class);
			removeListener("publishDisconnect", si.getName(), "onDisconnect", String.class);
		}
	}

	public void reset() {
		clear();
		setTimeout(null);
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
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void setCodec(String key) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException,
			IllegalArgumentException, InvocationTargetException {
		outRX.setCodec(key);
		outTX.setCodec(key);
		broadcastState();
	}

	public void setDTR(boolean state) {
		port.setDTR(state);
	}

	public String setHardwareLibrary(String clazz) {
		hardwareLibrary = clazz;
		return hardwareLibrary;
	}

	public void setRXCodec(Codec codec) {
		outRX.setCodec(codec);
	}

	/**
	 * default timeout for all reads 0 = infinity > 0 - will wait for the number
	 * in milliseconds if the data has not arrived then an IOError will be
	 * thrown
	 * 
	 * @param timeout
	 * @return
	 */
	public Integer setTimeout(Integer timeout) {
		timeoutMS = timeout;
		return timeout;
	}

	public void setTXCodec(Codec codec) {
		outTX.setCodec(codec);
	}

	public void stopRecording() {
		try {
			outRX.close();
			outTX.close();
			broadcastState();
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
		stopRecording();
	}

	@Override
	public String toString() {
		return String.format("%s->%s", getName(), portName);
	}

	// write(byte[] b) IOException
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			write(data[i] & 0xff); // recently removed - & 0xFF
		}
	}

	// write(int b) IOException
	public void write(int b) throws IOException {
		// int newByte = data & 0xFF;

		if (connectedPorts.size() == 0) {
			error("can not write to a closed port!");
		}

		for (String portName : connectedPorts.keySet()) {
			Port writePort = connectedPorts.get(portName);
			writePort.write(b);
		}

		// main line TX
		invoke("publishTX", b);

		++txCount;
		outTX.write(b);
	}

	// write(int[] data) throws IOException - not in OutputStream
	public void write(int[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			write(data[i]); // recently removed - & 0xFF
		}
	}

	// ============= write methods begin ====================
	// write(String data) not in OutputStream
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	// FIXME - change Codec based on file extension !!!
	// file (formatter/parser) --to--> tx
	public void writeFile(String filename) {
		try {

			byte[] fileData = FileIO.fileToByteArray(new File(filename));

			/*
			 * TODO - ENCODING !!! if (txCodec != null) { // FIXME parse the
			 * incoming file for (int i = 0; i < fileData.length; ++i) { //
			 * FIXME - determine what is needed / expected to parse //
			 * write(txFormatter.parse(fileData[i])); } } else {
			 */
			for (int i = 0; i < fileData.length; ++i) {
				write(fileData[i]);
			}
			// }

		} catch (Exception e) {
			error(e);
		}
	}

	
	public static void main(String[] args) {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Runtime.start("gui", "GUIService");
			//Serial serial = (Serial) Runtime.start("serial", "Serial");
			


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

	
			try {

				int timeout = 500;// 500 ms serial timeout

				// Runtime.start("gui", "GUIService");
				// Runtime.start("webgui", "WebGUI");

				// get serial handle and creates a uart & virtual null modem cable
				Serial serial = (Serial) Runtime.start("serial", "Serial");
				serial.setTimeout(timeout);
				
				String port = "COM15";
				
				// EASY VIRTUAL SWITCH

				// ---- Virtual Begin -----
				VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
				virtual.createVirtualPort(port);
				Serial uart = virtual.getUART();
				uart.setTimeout(300);
				// ---- Virtual End -----
				
				serial.connect(port);


				// verify the null modem cable is connected
				if (!serial.isConnected()) {
					throw new IOException(String.format("%s not connected", serial.getName()));
				}

				if (!uart.isConnected()) {
					throw new IOException(String.format("%s not connected", uart.getName()));
				}

				// start binary recording
				serial.record("serial");
				uart.record("uart");

				// test blocking on exact size
				serial.write(10);
				serial.write(20);
				serial.write(30);
				serial.write(40);
				serial.write(50);
				serial.write(60);
				serial.write(70);
				uart.write("000D\r");
				// read back
				log.info(serial.readString(5));

				// blocking read with timeout
				String data = "HELLO";
				uart.write(data);
				String hello = serial.readString(data.length());

				if (!data.equals(hello)) {
					throw new IOException("data not equal");
				}

				serial.info("read back [%s]", hello);

				serial.info("array write");
				serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 127, (byte) 128, (byte) 254, (byte) 255 });
				uart.clear();
				serial.write("this is the end of the line \n");
				serial.clear();
				byte[] blah = serial.readLine();
				byte[] readBackArray = uart.readLine();
				log.info(Arrays.toString(readBackArray));

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
				serial.info("clear buffers");
				serial.clear();
				uart.clear();

				if (serial.available() != 0) {
					throw new IOException("available data after clear");
				}

				// support write(int) kill pill or not ?
				// I say yes
				serial.info("testing blocking");
				for (int i = 255; i > -1; --i) {
					serial.write(i);
					int readBack = uart.read();
					log.info(String.format("written %d read back %d", i, readBack));
					if (i < 256 && i > -1) {
						if (readBack != i) {
							throw new IOException(String.format("read back not the same as written for value %d %d !", i, readBack));
						}
					}
				}

				// FIXME - test the -1 write(int) kill pill
				// serial.write(-1) -> should close port !!!

				// in the real world we don't know when the sender to
				// our receiver is done - so we'll sleep here
				sleep(300);
				serial.info("clear buffers");
				serial.clear();
				uart.clear();

				// test publish/subscribe nonblocking
				serial.addByteListener(serial); // <-- FIXME CREATES INFINITE LOOP BUG
				uart.write(64);
				
				// TODO - low level details of strings & timeouts
				// TODO - filename
				serial.clear();
				serial.setCodec("ascii");
				uart.setCodec("ascii");

				// basic record
				String inRecord = "this is a short ascii row\n";
				uart.write(inRecord);
				String record = serial.readString();

				serial.clear();
				uart.clear();
				
				serial.record("serialASC");
				uart.record("uartASC");

				serial.stopRecording();
				uart.stopRecording();

				// ======= decimal format begin ===========
				serial.setCodec("decimal");
				uart.setCodec("decimal");

				// default non-binary format is ascii decimal
				serial.record("serial.2");
				uart.record("uart.2");
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
				serial.setCodec("hex");
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

				//log.info(status.flatten().toString());

			} catch (Exception e) {
				Logging.logError(e);
			}
		

	}

}
