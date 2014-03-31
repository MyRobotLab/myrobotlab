package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
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

	public final static Logger log = LoggerFactory.getLogger(Serial.class.getCanonicalName());

	private transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	int BUFFER_SIZE = 8192;
	byte[] buffer = new byte[BUFFER_SIZE];
	BlockingQueue<Byte> blockingData = new LinkedBlockingQueue<Byte>();

	private int recievedByteCount = 0;

	boolean publish = true;
	boolean blocking = false;

	private boolean connected = false;
	private String portName = "";

	public static final int PUBLISH_BYTE = 0;
	public static final int PUBLISH_LONG = 1;
	public static final int PUBLISH_INT = 2;
	public static final int PUBLISH_CHAR = 3;
	public static final int PUBLISH_BYTE_ARRAY = 3;
	public static final int PUBLISH_STRING = 4;
	public static final int PUBLISH_MESSAGE = 5;
	public static final int PUBLISH_MRL_MESSAGE = 6;

	public boolean useFixedWidth = false;
	public int msgWidth = 10;
	public char delimeter = '\n';

	public int publishType = PUBLISH_BYTE;

	// Arduino micro-controller specific at the moment
	public int BYTE_SIZE_LONG = 4;
	public int BYTE_SIZE_INT = 2;

	public Serial(String n) {
		super(n);
	}

	public void capacity(int size) {
		buffer = new byte[size];
	}

	// @Override
	public String getDescription() {
		return "used as a general template";
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

				byte newByte;
				recievedByteCount = 0;
				log.info("--------begin---------------");
				// FIXME available you should stay in the wile loop and read
				// that many !!!!
				while (serialDevice.isOpen() && serialDevice.available() > 0) {
					newByte = (byte) serialDevice.read();
					++recievedByteCount;

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

						case PUBLISH_LONG: {
							buffer[recievedByteCount - 1] = newByte;
							if (recievedByteCount % BYTE_SIZE_LONG == 0) {
								long value = 0;
								for (int i = 0; i < BYTE_SIZE_LONG; i++) {
									value = (value << 8) + (buffer[i] & 0xff);
								}

								invoke("publishLong", value);
								recievedByteCount = 0;
							}
							break;
						}
						case PUBLISH_INT: {
							buffer[recievedByteCount - 1] = newByte;
							/*
							 * if (recievedByteCount % BYTE_SIZE_LONG == 0) {
							 * long value = 0; for (int i = 0; i <
							 * BYTE_SIZE_LONG; i++) { value = (value << 8) +
							 * (buffer[i] & 0xff); }
							 * 
							 * invoke("publishInt", value); recievedByteCount =
							 * 0; }
							 */

							int newInt = (newByte & 0xFF);
							invoke("publishInt", newInt);

							break;
						}

						case PUBLISH_BYTE: {
							invoke("publishByte", newByte);
							log.warn(String.format("%s published byte %d", getName(), newByte));
							break;
						}
						
						case PUBLISH_STRING: {
							// here be dragons...
							buffer[recievedByteCount - 1] = newByte;
							if (recievedByteCount % BYTE_SIZE_LONG == 0) {
								String value = "";
								invoke("publishString", value);
								recievedByteCount = 0;
							}
							break;
						}

						}
					} // if publish
				}

				log.info("---out of loop----");
				log.info("cnt {}", recievedByteCount);
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
		log.info(String.format("%s published byte %02x", getName(), (int) data.byteValue()));
		return data;
	}

	public char publishChar(Character data) {
		return data;
	}

	public int publishInt(Integer data) {
		return data;
	}

	public long publishLong(Long data) {
		return data;
	}

	public byte[] publishByteArray(byte[] data) {
		return data;
	}

	public String publishString(String data) {
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
			serialDevice.write(data[i]);
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
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		// Create two virtual ports for UART and user and null them together:
		// create 2 virtual ports
		VirtualSerialPort vp0 = new VirtualSerialPort("/dev/vp0");
		VirtualSerialPort vp1 = new VirtualSerialPort("/dev/vp1");

		// make null modem cable ;)
		VirtualSerialPort.makeNullModem(vp0, vp1);
		/*
		 * vp1.tx = vp0.rx; vp1.rx = vp0.tx; vp1.listener
		 */

		// add virtual ports to the serial device factory
		SerialDeviceFactory.add(vp0);
		SerialDeviceFactory.add(vp1);

		// create the UART serial service
		// log.info("Creating a LIDAR UART Serial service named: " + getName() +
		// "SerialService");
		// String serialName = getName() + "SerialService";
		Serial serial0 = new Serial("serial0");
		serial0.startService();
		serial0.connect("/dev/vp0");

		Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
		arduino.connect("/dev/vp1");

		arduino.sendMsg(2, 2, 2);

		Runtime.createAndStart("webgui", "WebGUI");

		// user serial
		// Serial serial1 = new Serial("lidar_serial");
		Serial serial1 = new Serial("serial1");
		serial1.startService();

		// Runtime.createAndStart("gui", "GUIService");

		serial1.connect("/dev/vp1");

		for (int i = 0; i < 1000; ++i) {
			byte x = (byte) i;
			serial0.write((byte) x);
		}

		serial0.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 });
		serial0.write(new byte[] { 5, 5, 5, 5, 5 });
		serial0.write(new byte[] { 6, 6, 6, 6, 6 });
		serial0.write(new byte[] { 7, 7, 7, 7, 7 });
		serial0.write(new byte[] { 8, 8, 8, 8, 8 });
		serial0.write(new byte[] { 9, 9, 9, 9, 9 });
		serial0.write(new byte[] { 5, 5, 5, 5, 5 });
		serial0.write(new byte[] { 6, 6, 6, 6, 6 });
		serial0.write(new byte[] { 7, 7, 7, 7, 7 });
		serial0.write(new byte[] { 8, 8, 8, 8, 8 });
		serial0.write(new byte[] { 9, 9, 9, 9, 9 });

		serial1.write(new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });
		serial1.write(new byte[] { 6, 6, 6, 6, 6, 6 });

		serial0.write(new byte[] { 16 });
		log.info("here");

		/*
		 * 
		 * Serial serial = new Serial("serial"); serial.startService();
		 * 
		 * serial.connect("COM16", 9600, 8, 1, 0); /* // create 2 virtual ports
		 * VirtualSerialPort vp0 = new VirtualSerialPort("/dev/virtualPort0");
		 * VirtualSerialPort vp1 = new VirtualSerialPort("/dev/virtualPort1");
		 * 
		 * // make null modem cable ;) vp1.tx = vp0.rx; vp1.rx = vp0.tx;
		 * 
		 * // add virtual ports to the serial device factory
		 * SerialDeviceFactory.add(vp0); SerialDeviceFactory.add(vp1);
		 * 
		 * // create two serial services Serial searSerial = new
		 * Serial("searSerial"); Serial serial1 = new Serial("serial1");
		 * 
		 * searSerial.startService(); serial1.startService();
		 * 
		 * ArrayList<String> portNames = searSerial.getPortNames();
		 * 
		 * log.info("listing port names:"); for (int i = 0; i <
		 * portNames.size(); ++i) { log.info(portNames.get(i)); }
		 * 
		 * searSerial.connect("/dev/virtualPort0");
		 * serial1.connect("/dev/virtualPort1");
		 * 
		 * WebGUI web = new WebGUI("web"); web.startService();
		 * 
		 * // user starts initialization sequence
		 * log.info("user sends first set of bytes"); serial1.write(new byte[] {
		 * 13, 117, 100, 58 });
		 * 
		 * // second initialization sequence
		 * log.info("user sends second set of bytes"); serial1.write(new byte[]
		 * { 5, 5, 5, 5 });
		 * 
		 * // now the lidar is initialized sear virtual lidar will send // a
		 * long sequence of bytes log.info("lidar sending back data");
		 * searSerial.write(new byte[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
		 * 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
		 * 15 }); /*
		 * 
		 * Arduino arduino = new Arduino("arduino"); arduino.startService();
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
		/*
		 * Serial serial = new Serial("serial"); serial.startService();
		 * 
		 * serial.connect("COM9", 57600, 8, 1, 0);
		 * 
		 * for (int i = 0; i < 10; ++i) { log.info("here {}",
		 * serial.readByte()); } for (int i = 0; i < 10; ++i) {
		 * log.info("here {}", serial.readInt()); } for (int i = 0; i < 10; ++i)
		 * { log.info("here {}", serial.readByteArray(10)); }
		 */

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

}
