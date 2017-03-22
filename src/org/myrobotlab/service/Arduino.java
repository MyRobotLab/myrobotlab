package org.myrobotlab.service;

import static org.myrobotlab.arduino.Msg.MAGIC_NUMBER;
import static org.myrobotlab.arduino.Msg.MAX_MSG_SIZE;
import static org.myrobotlab.arduino.Msg.MRLCOMM_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.arduino.ArduinoUtils;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.DeviceSummary;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.i2c.I2CBus;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.motor.MotorConfigDualPwm;
import org.myrobotlab.motor.MotorConfigPulse;
import org.myrobotlab.motor.MotorConfigSimpleH;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialRelayListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.UltrasonicSensorControl;
import org.myrobotlab.service.interfaces.UltrasonicSensorController;

public class Arduino extends Service implements Microcontroller, PinArrayControl, I2CBusController, I2CController,
		SerialDataListener, ServoController, MotorController, NeoPixelController, UltrasonicSensorController,
		DeviceController, RecordControl, SerialRelayListener, PortListener, PortPublisher {

	private static final long serialVersionUID = 1L;

	public static class I2CDeviceMap {
		public int busAddress;
		public transient I2CControl control;
		public int deviceAddress;
	}

	public static class Sketch implements Serializable {
		private static final long serialVersionUID = 1L;
		public String data;
		public String name;

		public Sketch(String name, String data) {
			this.name = name;
			this.data = data;
		}
	}

	public transient static final int BOARD_TYPE_ID_UNKNOWN = 0;
	public transient static final int BOARD_TYPE_ID_MEGA = 1;
	public transient static final int BOARD_TYPE_ID_UNO = 2;
	public transient static final int BOARD_TYPE_ID_ADK_MEGA = 3;
	public transient static final int BOARD_TYPE_ID_NANO = 4;
	public transient static final int BOARD_TYPE_ID_PRO_MINI = 5;

	public transient static final String BOARD_TYPE_MEGA = "mega";
	public transient static final String BOARD_TYPE_MEGA_ADK = "megaADK";
	public transient static final String BOARD_TYPE_UNO = "uno";
	public transient static final String BOARD_TYPE_NANO = "nano";
	public transient static final String BOARD_TYPE_PRO_MINI = "pro mini";

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_BACKWARD = 0;
	public static final int MOTOR_FORWARD = 1;

	public static final int MRL_IO_NOT_DEFINED = 0;
	public static final int MRL_IO_SERIAL_0 = 1;
	public static final int MRL_IO_SERIAL_1 = 2;
	public static final int MRL_IO_SERIAL_2 = 3;
	public static final int MRL_IO_SERIAL_3 = 4;

	public static final int DIGITAL = 0;
	public static final int ANALOG = 1;

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 *
	 * @return ServiceType - returns all the data
	 *
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Arduino.class.getCanonicalName());
		meta.addDescription("This service interfaces with an Arduino micro-controller");
		meta.addCategory("microcontroller");
		meta.addPeer("serial", "Serial", "serial device for this Arduino");
		return meta;
	}

	/**
	 * degreeToMicroseconds - convert a value to send to servo from degree
	 * (0-180) to microseconds (544-2400)
	 * 
	 * @param degree
	 * @return
	 */
	public Integer degreeToMicroseconds(double degree) {
		// if (degree >= 544) return (int)degree; - G-> I don't think
		// this is a good idea, if they want to use microseconds - then let them
		// use
		// the controller.servoWriteMicroseconds method
		// this method vs mapping I think was a good idea.. :)
		return (int) Math.round((degree * (2400 - 544) / 180) + 544);
	}

	/**
	 * path of the Arduino IDE must be set by user should not be static - since
	 * gson will not serialize it, and it won't be 'saved()'
	 */
	public String arduinoPath;

	transient Map<Integer, Arduino> attachedController = new ConcurrentHashMap<Integer, Arduino>();

	/**
	 * board info "from" MrlComm - which can be different from what the user
	 * say's it is - if there is a difference the "user" should be notified -
	 * but not forced to use the mrlBoardInfo.
	 */
	final BoardInfo boardInfo = new BoardInfo();

	/**
	 * board type - UNO Mega etc..
	 * 
	 * if the user 'connects' first then the info could come from the board ..
	 * but if the user wants to upload first a npe will be thrown so we default
	 * it here to Uno
	 */
	String boardType = null;

	int byteCount;

	public transient int controllerAttachAs = MRL_IO_NOT_DEFINED;
	/**
	 * id reference of sensor, key is the MrlComm device id
	 */
	transient Map<Integer, DeviceMapping> deviceIndex = new ConcurrentHashMap<Integer, DeviceMapping>();

	/**
	 * Devices - string name index of device we need 2 indexes for sensors
	 * because they will be referenced by name OR by index
	 */
	transient Map<String, DeviceMapping> deviceList = new ConcurrentHashMap<String, DeviceMapping>();

	int error_arduino_to_mrl_rx_cnt = 0;

	int error_mrl_to_arduino_rx_cnt = 0;

	boolean heartbeat = false;

	I2CBus i2cBus = null;

	volatile byte[] i2cData = new byte[64];

	/**
	 * i2c This needs to be volatile because it will be updated in a different
	 * threads
	 */
	volatile boolean i2cDataReturned = false;

	volatile int i2cDataSize;

	Map<String, I2CDeviceMap> i2cDevices = new ConcurrentHashMap<String, I2CDeviceMap>();

	transient int[] ioCmd = new int[MAX_MSG_SIZE];

	public transient Msg msg;

	int msgSize;

	Integer nextDeviceId = 0;
	int numAck = 0;
	transient Map<String, PinArrayListener> pinArrayListeners = new ConcurrentHashMap<String, PinArrayListener>();

	/**
	 * the definitive sequence of pins - "true address"
	 */
	Map<Integer, PinDefinition> pinIndex = null;

	/**
	 * map of pin listeners
	 */
	transient Map<Integer, List<PinListener>> pinListeners = new ConcurrentHashMap<Integer, List<PinListener>>();

	/**
	 * pin named map of all the pins on the board
	 */
	Map<String, PinDefinition> pinMap = null;

	/**
	 * Serial service - the Arduino's serial connection
	 */
	Serial serial;

	public Sketch sketch;

	public String uploadSketchResult;

	private long boardInfoRequestTs;

	public Arduino(String n) {
		super(n);
		// serial = (Serial) createPeer("serial"); // Trying to make not necessary
		createPinList();
		String mrlcomm = FileIO.resourceToString("Arduino/MrlComm/MrlComm.ino");
		setSketch(new Sketch("MrlComm", mrlcomm));

		// add self as an attached device
		// to handle pin events and other base
		// Arduino methods
		attachDevice(this, (Object[]) null);
	}

	// > analogWrite/pin/value
	public void analogWrite(int pin, int value) {
		log.info(String.format("analogWrite(%d,%d)", pin, value));
		msg.analogWrite(pin, value);
	}

	/**
	 * attach a pin listener which listens for an array of all active pins
	 */
	@Override
	public void attach(PinArrayListener listener) {
		pinArrayListeners.put(listener.getName(), listener);
	}

	/**
	 * attach a pin listener who listens to a specific pin
	 */
	@Override
	public void attach(PinListener listener, int address) {
		String name = listener.getName();

		if (listener.isLocal()) {
			List<PinListener> list = null;
			if (pinListeners.containsKey(address)) {
				list = pinListeners.get(address);
			} else {
				list = new ArrayList<PinListener>();
			}
			list.add(listener);
			pinListeners.put(address, list);

		} else {
			addListener("publishPin", name, "onPin");
		}
	}

	/**
	 * String interface - this allows you to easily use url api requests like
	 * /attach/nameOfListener/3
	 */
	public void attach(String listener, int address) {
		attach((PinListener) Runtime.getService(listener), address);
	}

	synchronized private Integer attachDevice(DeviceControl device, Object[] attachConfig) {
		DeviceMapping map = new DeviceMapping(device, attachConfig);
		map.setId(nextDeviceId);
		deviceList.put(device.getName(), map);
		deviceIndex.put(nextDeviceId, map);
		++nextDeviceId;
		return map.getId();
	}

	// this allow to connect a controller to another controller with Serial1,
	// Serial2, Serial3 on a mega board
	public void connect(Arduino controller, String serialPort) throws IOException {
		if (controller == null) {
			error("setting null as controller");
			return;
		}
		if (controller == this) {
			error("controller can't attach to itself");
			return;
		}
		if (!controller.boardType.toLowerCase().contains("mega")) {
			error("You must connect to a Mega controller");
			return;
		}
		if (controllerAttachAs != MRL_IO_NOT_DEFINED) {
			log.info("controller already attached");
			return;
		}
		SerialRelay relay = (SerialRelay) Runtime.createAndStart("relay", "SerialRelay");
		switch (serialPort) {
		case "Serial1":
			controllerAttachAs = MRL_IO_SERIAL_1;
			break;
		case "Serial2":
			controllerAttachAs = MRL_IO_SERIAL_2;
			break;
		case "Serial3":
			controllerAttachAs = MRL_IO_SERIAL_3;
			break;
		default:
			error("Unknow serial port");
			return;
		}
		relay.attach(controller, this, controllerAttachAs);
		msg = new Msg(this, relay);
		msg.softReset(); // needed because there is no serial connect <- GroG
							// says -
							// this is heavy handed no?
		enableBoardInfo(true); // start the heartbeat getBoardInfo
		msg.getBoardInfo();
		log.info("waiting for boardInfo lock..........");
		synchronized (boardInfo) {
			try {
				long waitTime = System.currentTimeMillis();
				boardInfo.wait(4500); // max wait 4.5 seconds - for port to
				log.info("waited {} ms for Arduino {} to say hello.....", System.currentTimeMillis() - waitTime,
						getName());
			} catch (InterruptedException e) {
			}
		}

		// we might be connected now
		// see what our version is like...
		Integer version = boardInfo.getVersion();

		if (version == null) {
			error("%s did not get response from arduino....", serial.getPortName());
		} else if (!version.equals(MRLCOMM_VERSION)) {
			error("MrlComm.ino responded with version %s expected version is %s", version, MRLCOMM_VERSION);
		} else {
			info("%s connected on %s %s responded version %s ... goodtimes...", serial.getName(), controller.getName(),
					serialPort, version);
		}
		// GAP broadcastState();
	}

	// @Calamity - I like your method signature - but I think it
	// should create a MrlSerial device and read and write similar to the I2C
	// MrlDevice instead of replacing the service's serial service
	// @grog - I don't mind using a MrlSerial device, as both way will
	// essentially do the same thing. The difference is only where the
	// messages will be send in MrlComm (processCommand vs update methods). It
	// could not be the same way as I2C because I2C read
	// block and blocking is evil
	// two thing I had in mind when I did it:
	// 1- be able to connect MrlComm to a master MrlComm using different
	// communication protocol (Serial, I2C, bluetooth, wifi)
	// but this also can be done with different device type
	// 2- I also had in mind of having the Master arduino and it's slaves (chain
	// of slave) act as one device. So they could talk
	// and interract with each other without having to go back to the javaland.
	// Not sure if it's a good idea or not, but that's
	// one of the reason I had go that way

	public void connect(String port) {
		connect(port, Serial.BAUD_115200, 8, 1, 0);
	}

	/**
	 * default params to connect to Arduino & MrlComm.ino
	 *
	 * @param port
	 * @return
	 * @throws IOException
	 */
	@Override
	public void connect(String port, int rate, int databits, int stopbits, int parity) {

		try {
			// FIXME - GroG asks, who put the try here - shouldn't it throw if
			// we
			// can't connect
			// how would you recover?
			if (isConnected() && port.equals(serial.getPortName())) {
				log.info("already connected to port {}", port);
				return;
			}

			serial.connect(port, rate, databits, stopbits, parity);

			// most likely on a real board this send will never get to
			// mrlcomm - because the board is not ready - but it doesnt hurt
			// and in fact it helps VirtualArduino - since we currently do not
			// have a DTR CDR line in the virtual port as use this as a signal
			// of
			// connection

			// by default ack'ing is now on..
			// but with this first msg there is no msg before it,
			// and there is a high probability that the board is not really
			// ready
			// and this msg along with the ack will be ignored
			// so we turn of ack'ing locally
			msg.enableAcks(false);
			enableBoardInfo(true); // start the heartbeat getBoardInfo
			msg.getBoardInfo();

			log.info("waiting for boardInfo lock..........");
			synchronized (boardInfo) {
				try {
					long waitTime = System.currentTimeMillis();
					boardInfo.wait(4500);
					log.info("waited {} ms for Arduino {} to say hello", System.currentTimeMillis() - waitTime,
							getName());
				} catch (InterruptedException e) {
				}
			}

			// we might be connected now
			// see what our version is like...
			Integer version = boardInfo.getVersion();

			if (version == null) {
				error("%s did not get response from arduino....", serial.getPortName());
			} else if (!version.equals(MRLCOMM_VERSION)) {
				error("MrlComm.ino responded with version %s expected version is %s", version, MRLCOMM_VERSION);
			} else {
				info("%s connected on %s responded version %s ... goodtimes...", serial.getName(), serial.getPortName(),
						version);
			}

			msg.enableAcks(true);

		} catch (Exception e) {
			log.error("serial open threw", e);
			error(e.getMessage());
		}

		// GAP broadcastState();
	}

	public Map<String, PinDefinition> createPinList() {
		pinMap = new ConcurrentHashMap<String, PinDefinition>();
		pinIndex = new ConcurrentHashMap<Integer, PinDefinition>();

		if (boardType != null && boardType.toLowerCase().contains("mega")) {
			for (int i = 0; i < 70; ++i) {
				PinDefinition pindef = new PinDefinition();
				String pinName = null;
				if (i == 0) {
					pindef.setRx(true);
				}
				if (i == 1) {
					pindef.setTx(true);
				}
				if (i < 1 || (i > 13 && i < 54)) {
					pinName = String.format("D%d", i);
					pindef.setDigital(true);
				} else if (i > 53) {
					pinName = String.format("A%d", i - 54);
					pindef.setAnalog(true);
				} else {
					pinName = String.format("D%d", i);
					pindef.setPwm(true);
				}
				pindef.setName(pinName);
				pindef.setAddress(i);
				// pinMap is a translation map
				// we put both string address and 'name' and
				// any other aliases we want the 'real' pin to be identified by
				pinMap.put(pinName, pindef);
				pinMap.put(String.format("%d", i), pindef);
				pinIndex.put(i, pindef);
			}
		} else {
			for (int i = 0; i < 20; ++i) {
				PinDefinition pindef = new PinDefinition();
				String pinName = null;
				if (i == 0) {
					pindef.setRx(true);
				}
				if (i == 1) {
					pindef.setTx(true);
				}
				if (i < 14) {
					pinName = String.format("D%d", i);
					pindef.setDigital(true);
				} else {
					pindef.setAnalog(true);
					pinName = String.format("A%d", i - 14);
				}
				if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
					pindef.setPwm(true);
					pinName = String.format("D%d", i);
				}
				pindef.setName(pinName);
				pindef.setAddress(i);
				pinMap.put(pinName, pindef);
				pinMap.put(String.format("%d", i), pindef);
				pinIndex.put(i, pindef);
			}
		}
		return pinMap;
	}

	// > customMsg/[] msg
	public void customMsg(int... params) {
		msg.customMsg(params);
	}

	@Override
	// > deviceDetach/deviceId
	public void detach(DeviceControl device) {
		// TODO check / detach - must be careful of infinit loop
		// if (device.isAttached()){
		//
		// }
		log.info("detaching device {}", device.getName());
		msg.deviceDetach(getDeviceId(device));
		if (deviceList.containsKey(device.getName())) {
			DeviceMapping dm = deviceList.get(device.getName());
			deviceIndex.remove(dm.getId());
			deviceList.remove(device.getName());
		}
	}

	/**
	 * silly Arduino implementation - but keeping it since its familiar
	 * 
	 * @param address
	 * @param value
	 */
	// > digitalWrite/pin/value
	public void digitalWrite(int pin, int value) {
		log.info("digitalWrite {} {}", pin, value);
		msg.digitalWrite(pin, value);
		PinDefinition pinDef = pinIndex.get(pin); // why ?
		invoke("publishPinDefinition", pinDef);
	}

	// > disablePin/pin
	public void disablePin(int address) {
		msg.disablePin(address);
		PinDefinition pinDef = pinIndex.get(address);
		invoke("publishPinDefinition", pinDef);
	}

	// > disablePins
	public void disablePins() {
		msg.disablePins();
	}

	public void disconnect() {
		// FIXED - all don in 'onDisconnect()'
		// enableBoardInfo(false);
		// boardInfo is not valid after disconnect
		// because we might be connecting to a different Arduino
		// boardInfo.reset();
		for (Arduino controller : attachedController.values()) {
			controller.disconnect();
		}
		attachedController.clear();
		if (controllerAttachAs != MRL_IO_NOT_DEFINED) {
			controllerAttachAs = MRL_IO_NOT_DEFINED;
			serial = (Serial) createPeer("serial");
		} else {
			serial.disconnect();
		}
		broadcastState();
	}

	// > enableAck/bool enabled
	public void enableAck(boolean enabled) {
		// ackEnabled = enabled;
		// enable both sides acking Java & MrlComm
		msg.enableAcks(enabled);
	}

	// msg
	// > enableBoardInfo/bool enabled
	public void enableBoardInfo(Boolean enabled) {
		// msg.enableBoardInfo(enabled);
		if (enabled) {
			addTask("getBoardInfo", 1000, "sendBoardInfoRequest");
		} else {
			purgeTask("getBoardInfo");
		}

	}

	// > enablePin/address/type/b16 rate
	public void enablePin(int address) {
		enablePin(address, 0);
	}

	public void enablePin(String address) {
		PinDefinition pd = pinMap.get(address);
		enablePin(pd.getAddress());
	}

	public void disablePin(String address) {
		PinDefinition pd = pinMap.get(address);
		disablePin(pd.getAddress());
	}

	/**
	 * start polling reads of selected pin
	 *
	 * @param pin
	 * @throws Exception
	 */
	// > enablePin/address/type/b16 rate
	public void enablePin(int address, int rate) {
		if (!isConnected()) {
			error("must be connected to enable pins");
			return;
		}
		PinDefinition pin = pinIndex.get(address);
		msg.enablePin(address, getMrlPinType(pin), rate);
		pin.setEnabled(true);
		invoke("publishPinDefinition", pin); // broadcast pin change
	}

	// > getBoardInfo
	public BoardInfo getBoardInfo() {
		// msg.getBoardInfo(); do not do this -
		// results in a serial infinit loop
		// msg.getBoardInfo();
		return boardInfo;
	}

	public void sendBoardInfoRequest() {
		boardInfoRequestTs = System.currentTimeMillis();
		msg.getBoardInfo();
	}

	@Override
	public String getBoardType() {
		return boardType;
	}

	@Override
	public DeviceController getController() {
		return this;
	}

	public DeviceControl getDevice(Integer deviceId) {
		return deviceIndex.get(deviceId).getDevice();
	}

	Integer getDeviceId(DeviceControl device) {
		return getDeviceId(device.getName());
	}

	Integer getDeviceId(String name) {
		if (deviceList.containsKey(name)) {
			Integer id = deviceList.get(name).getId();
			if (id == null) {
				error("cannot get device id for %s - device attempetd to attach - but I suspect something went wrong",
						name);
			}
			return id;
		}
		log.error("getDeviceId could not find device {}", name);
		return null;
	}

	private String getDeviceName(int deviceId) {
		return getDevice(deviceId).getName();
	}

	/**
	 * int type to describe the pin defintion to Pin.h 0 digital 1 analog
	 * 
	 * @param pin
	 * @return
	 */
	public Integer getMrlPinType(PinDefinition pin) {
		if (boardType == null) {
			error("must have pin board type to determin pin definition");
			return null;
		}

		if (pin.isAnalog()) {
			return 1;
		}

		return 0;
	}

	@Override
	public List<PinDefinition> getPinList() {
		List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
		return list;
	}

	public String getPortName() {
		return serial.getPortName();
	}

	/**
	 * Use the serial service for serial activities ! No reason to replicate
	 * methods
	 *
	 * @return
	 */
	public Serial getSerial() {
		return serial;
	}

	public Sketch getSketch() {
		return sketch;
	}

	/**
	 * sends a heartbeat, if not replied from in the next heartbeat disconnects
	 * and resets
	 */
	// > heartbeat
	/*
	 * public void heartbeat() { if (!heartbeat) { log.info(
	 * "No answer from controller:{}. Disconnecting...", this.getName());
	 * purgeTask("heartbeat"); if (isConnected()) { disconnect(); } }
	 * 
	 * // resetting to false - publishHeartbeat will set to // true (hopefully
	 * before the next heartbeat) heartbeat = false; msg.heartbeat(); }
	 */

	@Override
	public void i2cAttach(I2CControl control, int busAddress, int deviceAddress) {
		// TODO Auto-generated method stub - I2C
		// Create the i2c bus device in MrlComm the first time this method is
		// invoked.
		// Add the i2c device to the list of i2cDevices
		// Pattern: deviceAttach(device, Object... config)
		// To add the i2c bus to the deviceList I need an device that represents
		// the i2c bus here and in MrlComm
		// This will only handle the creation of i2cBus.
		if (i2cBus == null) {
			i2cBus = new I2CBus(String.format("I2CBus%s", busAddress));
			i2cBusAttach(i2cBus, busAddress);
		}

		// This part adds the service to the mapping between
		// busAddress||DeviceAddress
		// and the service name to be able to send data back to the invoker
		String key = String.format("%d.%d", busAddress, deviceAddress);
		I2CDeviceMap devicedata = new I2CDeviceMap();
		if (i2cDevices.containsKey(key)) {
			log.error(String.format("Device %s %s %s already exists.", busAddress, deviceAddress, control.getName()));
		} else {
			devicedata.busAddress = busAddress;
			devicedata.deviceAddress = deviceAddress;
			devicedata.control = control;
			i2cDevices.put(key, devicedata);
		}
	}

	/**
	 * Internal Arduino method to create an i2cBus object in MrlComm that is
	 * shared between all i2c devices
	 * 
	 * @param control
	 * @param busAddress
	 */
	// > i2cBusAttach/deviceId/i2cBus
	private void i2cBusAttach(I2CBusControl control, int busAddress) {
		Integer deviceId = attachDevice(i2cBus, new Object[] { busAddress });
		msg.i2cBusAttach(deviceId, busAddress);
	}

	@Override
	// > i2cRead/deviceId/deviceAddress/size
	public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
		i2cDataReturned = false;
		// Get the device index to the MRL i2c bus
		String i2cBus = String.format("I2CBus%s", busAddress);
		int deviceId = getDeviceId(i2cBus);
		log.info(String.format("i2cRead requesting %s bytes", size));
		msg.i2cRead(deviceId, deviceAddress, size);

		int retry = 0;
		int retryMax = 1000; // ( About 1000ms = s)
		try {
			/**
			 * We will wait up to retryMax times to get the i2c data back from
			 * MrlComm.c and wait 1 ms between each try. A blocking queue is not
			 * needed, as this is only a single data element - and blocking is
			 * not necessary.
			 */
			while ((retry < retryMax) && (!i2cDataReturned)) {
				sleep(1);
				++retry;
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
		if (i2cDataReturned) {
			log.debug(String.format("i2cReturnData returned %s bytes to caller %s.", i2cDataSize, control.getName()));
			for (int i = 0; i < i2cDataSize; i++) {
				buffer[i] = i2cData[i];
				log.debug(String.format("i2cReturnData returned ix %s value %s", i, buffer[i]));
			}
			return i2cDataSize;
		}
		// Time out, no data returned
		return -1;
	}

	/**
	 * This methods is called by the i2cBus object when data is returned from
	 * the i2cRead It populates the i2cData area and sets the i2cDataReturned
	 * flag to true so that the loop in i2cRead can return the data to the
	 * caller
	 * 
	 */
	@Override
	public void i2cReturnData(int[] rawData) {
		i2cDataSize = rawData.length;
		for (int i = 0; i < i2cDataSize; i++) {
			i2cData[i] = (byte) (rawData[i] & 0xff);
		}
		log.debug(String.format("i2cReturnData invoked. i2cDataSize = %s %s %s", i2cDataSize, rawData[0], rawData[1]));
		i2cDataReturned = true;
	}

	@Override
	// > i2cWrite/deviceId/deviceAddress/[] data
	public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
		String i2cBus = String.format("I2CBus%s", busAddress);
		int deviceId = getDeviceId(i2cBus);

		int data[] = new int[size];
		for (int i = 0; i < size; ++i) {
			data[i] = buffer[i];// guess you want -128 to 127 ?? [ ] == unsigned
			// char & 0xff;
		}

		msg.i2cWrite(deviceId, deviceAddress, data);
	}

	@Override
	// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
	public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize,
			byte[] readBuffer, int readSize) {
		if (writeSize != 1) {
			i2cWrite(control, busAddress, deviceAddress, writeBuffer, writeSize);
			return i2cRead(control, busAddress, deviceAddress, readBuffer, readSize);
		} else {
			i2cDataReturned = false;
			// Get the device index to the MRL i2c bus
			String i2cBus = String.format("I2CBus%s", busAddress);
			int deviceId = getDeviceId(i2cBus);

			int msgBuffer[] = new int[4];
			msgBuffer[0] = deviceId;
			msgBuffer[1] = deviceAddress;
			msgBuffer[2] = readSize;
			msgBuffer[3] = writeBuffer[0];
			msg.i2cWriteRead(deviceId, deviceAddress, readSize, writeBuffer[0] & 0xFF);
			int retry = 0;
			int retryMax = 1000; // ( About 1000ms = s)
			try {
				/**
				 * We will wait up to retryMax times to get the i2c data back
				 * from MrlComm.c and wait 1 ms between each try. A blocking
				 * queue is not needed, as this is only a single data element -
				 * and blocking is not necessary.
				 */
				while ((retry < retryMax) && (!i2cDataReturned)) {
					sleep(1);
					++retry;
				}
			} catch (Exception e) {
				Logging.logError(e);
			}
			if (i2cDataReturned) {
				log.debug(
						String.format("i2cReturnData returned %s bytes to caller %s.", i2cDataSize, control.getName()));
				for (int i = 0; i < i2cDataSize; i++) {
					readBuffer[i] = i2cData[i];
					log.debug(String.format("i2cReturnData returned ix %s value %s", i, readBuffer[i]));
				}
				return i2cDataSize;
			}
			// Time out, no data returned
			return -1;
		}
	}

	@Override
	public boolean isConnected() {
		// include that we must have gotten a valid MrlComm version number.
		if (serial != null && serial.isConnected() && boardInfo.getVersion() != null) {
			return true;
		}
		return false;
	}

	// FIXME put recording in generated message structure !!!
	@Override
	public boolean isRecording() {
		return msg.isRecording();
	}

	@Override
	public void motorMove(MotorControl mc) {

		MotorConfig c = mc.getConfig();

		if (c == null) {
			error("motor config not set");
			return;
		}

		Class<?> type = mc.getConfig().getClass();

		double powerOutput = mc.getPowerOutput();

		if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH) c;
			msg.digitalWrite(config.getDirPin(), (powerOutput < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
			msg.analogWrite(config.getPwrPin(), (int) Math.abs(powerOutput));
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm) c;
			if (powerOutput < 0) {
				msg.analogWrite(config.getLeftPin(), 0);
				msg.analogWrite(config.getRightPin(), (int) Math.abs(powerOutput));
			} else if (powerOutput > 0) {
				msg.analogWrite(config.getRightPin(), 0);
				msg.analogWrite(config.getLeftPin(), (int) Math.abs(powerOutput));
			} else {
				msg.analogWrite(config.getLeftPin(), 0);
				msg.analogWrite(config.getRightPin(), 0);
			}
		} else if (MotorPulse.class == type) {
			MotorPulse motor = (MotorPulse) mc;
			// sdsendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT),
			// 0);
			// TODO implement with a -1 for "endless" pulses or a different
			// command parameter :P
			// sendMsg(new
			// MrlMsg(PULSE).append(motor.getPulsePin()).append((int)
			// Math.abs(powerOutput)));
		} else {
			error("motorMove for motor type %s not supported", type);
		}

	}

	// ========== pulsePin begin =============
	// FIXME - MasterBlaster had a pulse motor which could support MoveTo
	// We need a Motor + encoder (analog or digital) DiyServo does this...
	@Override
	public void motorMoveTo(MotorControl mc) {
		// speed parameter?
		// modulo - if < 1
		// speed = 1 else
		log.info("motorMoveTo targetPos {} powerLevel {}", mc.getTargetPos(), mc.getPowerLevel());

		Class<?> type = mc.getClass();

		// if pulser (with or without fake encoder
		// send a series of pulses !
		// with current direction
		if (MotorPulse.class == type) {
			MotorPulse motor = (MotorPulse) mc;
			// check motor direction
			// send motor direction
			// TODO powerLevel = 100 * powerlevel

			// FIXME !!! - this will have to send a Long for targetPos at some
			// point !!!!
			double target = Math.abs(motor.getTargetPos());

			int b0 = (int) target & 0xff;
			int b1 = ((int) target >> 8) & 0xff;
			int b2 = ((int) target >> 16) & 0xff;
			int b3 = ((int) target >> 24) & 0xff;

			// TODO FIXME
			// sendMsg(PULSE, deviceList.get(motor.getName()).id, b3, b2, b1,
			// b0, (int) motor.getPowerLevel(), feedbackRate);
		}

	}

	@Override
	public void motorReset(MotorControl motor) {
		// perhaps this should be in the motor control
		// motor.reset();
		// opportunity to reset variables on the controller
		// sendMsg(MOTOR_RESET, motor.getind);
	}

	@Override
	public void motorStop(MotorControl mc) {
		MotorConfig c = mc.getConfig();

		if (c == null) {
			error("motor config not set");
			return;
		}

		Class<?> type = mc.getConfig().getClass();

		if (MotorConfigPulse.class == type) {
			MotorConfigPulse config = (MotorConfigPulse) mc.getConfig();
			// sendMsg(new MrlMsg(PULSE_STOP).append(config.getPulsePin()));
		} else if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH) mc.getConfig();
			msg.analogWrite(config.getPwrPin(), 0);
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm) mc.getConfig();
			msg.analogWrite(config.getLeftPin(), 0);
			msg.analogWrite(config.getRightPin(), 0);
		}
	}

	@Override
	// > neoPixelAttach/deviceId/pin/b32 numPixels
	public void neoPixelAttach(NeoPixel neopixel, int pin, int numPixels) {
		Integer deviceId = attachDevice(neopixel, new Object[] { pin, numPixels });
		msg.neoPixelAttach(getDeviceId(neopixel)/* byte */, pin/* byte */, numPixels/* b32 */);
	}

	@Override
	// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
	public void neoPixelSetAnimation(NeoPixel neopixel, int animation, int red, int green, int blue, int speed) {
		msg.neoPixelSetAnimation(getDeviceId(neopixel), animation, red, green, blue, speed);
	}

	@Override
	// > neoPixelWriteMatrix/deviceId/[] buffer
	public void neoPixelWriteMatrix(NeoPixel neopixel, List<Integer> data) {
		int id = getDeviceId(neopixel);
		/*
		 * int[] buffer = new int[data.size() + 2]; buffer[0] = id; buffer[1] =
		 * data.size(); for (int i = 0; i < data.size(); i++) { buffer[i + 2] =
		 * data.get(i); }
		 */
		int[] buffer = new int[data.size()];
		for (int i = 0; i < data.size(); ++i) {
			buffer[i] = data.get(i);
		}
		msg.neoPixelWriteMatrix(getDeviceId(neopixel), buffer);
	}

	/**
	 * Callback for Serial service - local (not remote) although a
	 * publish/subscribe could be created - this method is called by a thread
	 * waiting on the Serial's RX BlockingQueue
	 *
	 * Other services may use the same technique or subscribe to a Serial's
	 * publishByte method
	 *
	 * it might be worthwhile to look in optimizing reads into arrays vs single
	 * byte processing .. but maybe there would be no gain
	 *
	 */

	// FIXME - onByte(int[] data)
	@Override
	public Integer onByte(Integer newByte) {
		try {
			/**
			 * Archtype InputStream read - rxtxLib does not have this
			 * straightforward design, but the details of how it behaves is is
			 * handled in the Serial service and we are given a unified
			 * interface
			 *
			 * The "read()" is data taken from a blocking queue in the Serial
			 * service. If we want to support blocking functions in Arduino then
			 * we'll "publish" to our local queues
			 */
			// TODO: consider reading more than 1 byte at a time ,and make this
			// callback onBytes or something like that.

			++byteCount;
			if (log.isDebugEnabled()) {
				log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
			}
			if (byteCount == 1) {
				if (newByte != MAGIC_NUMBER) {
					byteCount = 0;
					msgSize = 0;
					Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
					warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte,
							++error_arduino_to_mrl_rx_cnt));
					// dump.setLength(0);
				}
				return newByte;
			} else if (byteCount == 2) {
				// get the size of message
				if (newByte > 64) {
					byteCount = 0;
					msgSize = 0;
					error(String.format("Arduino->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
					return newByte;
				}
				msgSize = newByte.intValue();
				// dump.append(String.format("MSG|SZ %d", msgSize));
			} else if (byteCount > 2) {
				// remove header - fill msg data - (2) headbytes -1
				// (offset)
				// dump.append(String.format("|P%d %d", byteCount,
				// newByte));
				ioCmd[byteCount - 3] = newByte.intValue();
			} else {
				// the case where byteCount is negative?! not got.
				error(String.format("Arduino->MRL error %d rx negsz errors", ++error_arduino_to_mrl_rx_cnt));
				return newByte;
			}
			if (byteCount == 2 + msgSize) {
				// we've received a full message

				msg.processCommand(ioCmd);

				// Our 'first' getBoardInfo may not receive a acknowledgement
				// so this should be disabled until boadInfo is valid

				/**
				 * acking is done in Msg ! if (boardInfo.isValid() &&
				 * ackEnabled) { synchronized (ackRecievedLock) { try { long ts
				 * = System.currentTimeMillis(); log.info(
				 * "***** starting wait *****"); ackRecievedLock.wait(10000);
				 * log.info("*****  waited {} ms *****",
				 * (System.currentTimeMillis() - ts)); } catch
				 * (InterruptedException e) {// don't care} }
				 * 
				 * if (!ackRecievedLock.acknowledged) { log.error(
				 * "Ack not received : {} {}", Msg.methodToString(ioCmd[0]),
				 * numAck); } } }
				 ***/

				// clean up memory/buffers
				msgSize = 0;
				byteCount = 0;
				Arrays.fill(ioCmd, 0); // optimize remove
			}
		} catch (Exception e) {
			++error_mrl_to_arduino_rx_cnt;
			error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
			log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount));
			// try again (clean up memory buffer)
			msgSize = 0;
			byteCount = 0;
			Logging.logError(e);
		}
		return newByte;
	}

	@Override
	public void onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		enableBoardInfo(true);
		// chained...
		invoke("publishConnect", portName);
	}

	public void onCustomMsg(Integer ax, Integer ay, Integer az) {
		log.info("onCustomMsg");
	}

	@Override
	public void onDisconnect(String portName) {
		info("%s disconnected from %s", getName(), portName);
		enableAck(false);
		enableBoardInfo(false);
		boardInfo.reset();
		// chained...
		invoke("publishDisconnect", portName);
	}

	public void pinMode(int pin, int mode) {
		msg.pinMode(pin, mode);
		PinDefinition pinDef = pinIndex.get(pin);
		invoke("publishPinDefinition", pinDef);
	}

	@Override
	// > pinMode/pin/mode
	public void pinMode(int address, String mode) {
		if (mode != null && mode.equalsIgnoreCase("INPUT")) {
			pinMode(address, INPUT);
		} else {
			pinMode(address, OUTPUT);
		}
	}

	/**
	 * With Arduino we want to be able to do pinMode("D7", "INPUT"), but it
	 * should not be part of the PinArrayControl interface - because when it
	 * comes down to it .. a pin MUST ALWAYS have an address regardless what you
	 * label or name it...
	 * 
	 * @param pinName
	 * @param mode
	 */
	public void pinMode(String pinName, String mode) {
		if (mode != null && mode.equalsIgnoreCase("INPUT")) {
			pinMode(pinNameToAddress(pinName), mode);
		} else {
			pinMode(pinNameToAddress(pinName), mode);
		}
	}

	public Integer pinNameToAddress(String pinName) {
		if (!pinMap.containsKey(pinName)) {
			error("no pin %s exists", pinName);
			return null;
		}
		return pinMap.get(pinName).getAddress();
	}

	/**
	 * 
	 * @param function
	 */
	// < publishAck/function
	public void publishAck(Integer function/* byte */) {
		log.debug("Message Ack received: =={}==", Msg.methodToString(function));

		msg.ackReceived(function);

		numAck++;
		heartbeat = true;
	}

	/**
	 * No longer needed .. Arduino service controls device list - MrlComm does
	 * not public String publishAttachedDevice(int deviceId, String deviceName)
	 * {
	 * 
	 * if (!deviceList.containsKey(deviceName)) { error(
	 * "PUBLISH_ATTACHED_DEVICE deviceName %s not found !", deviceName); }
	 * 
	 * DeviceMapping deviceMapping = deviceList.get(deviceName);
	 * deviceMapping.setId(deviceId); deviceIndex.put(deviceId,
	 * deviceList.get(deviceName));
	 * 
	 * // REMOVE invoke("publishAttachedDevice", deviceName);
	 * 
	 * info("==== ATTACHED DEVICE %s WITH MRLDEVICE %d ====", deviceName,
	 * deviceId);
	 * 
	 * return deviceName; }
	 */

	// < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/[]
	// deviceSummary
	public BoardInfo publishBoardInfo(Integer version/* byte */, Integer boardType/* byte */,
			Integer microsPerLoop/* b16 */, Integer sram/* b16 */, Integer activePins, int[] deviceSummary/* [] */) {
		long now = System.currentTimeMillis();
		boolean broadcast = false;
		if (version != boardInfo.getVersion() || boardType != boardInfo.getBoardType()) {
			broadcast = true;
		}
		boardInfo.setVersion(version);
		boardInfo.setMicrosPerLoop(microsPerLoop);
		boardInfo.setType(boardType);
		boardInfo.setSram(sram);
		boardInfo.setActivePins(activePins);
		boardInfo.setDeviceSummary(arrayToDeviceSummary(deviceSummary));
		boardInfo.heartbeatMs = now - boardInfoRequestTs;

		log.debug("Version return by Arduino: {}", boardInfo.getVersion());
		log.debug("Board type returned by Arduino: {}", boardInfo.getName());
		log.debug("Board type currently set: {}", boardType);
		if (!boardInfo.isUnknown()) {
			setBoard(boardInfo.getName());
			log.debug("Board type set to: {}", boardType);
		} else {
			log.debug("No change in board type");
		}

		synchronized (boardInfo) {
			boardInfo.notifyAll();
		}

		if (broadcast) {
			broadcastState();
		}

		return boardInfo;
	}

	DeviceSummary[] arrayToDeviceSummary(int[] deviceSummary) {
		DeviceSummary[] ds = new DeviceSummary[deviceSummary.length / 2];
		for (int i = 0; i < deviceSummary.length / 2; ++i) {
			int id = deviceSummary[i];
			int typeId = deviceSummary[i + 1];
			DeviceSummary ds0 = new DeviceSummary(getDeviceName(id), id, Msg.deviceTypeToString(typeId), typeId);
			ds[i] = ds0;
		}
		return ds;
	}

	// < publishCustomMsg/[] msg
	public int[] publishCustomMsg(int[] msg/* [] */) {
		return msg;
	}

	// < publishDebug/str debugMsg
	public String publishDebug(String debugMsg/* str */) {
		log.info("publishDebug {}", debugMsg);
		return debugMsg;
	}

	// < publishEcho/b32 sInt/str name1/b8/bu32 bui32/b32 bi32/b9/str name2/[]
	// config/bu32 bui322
	// < publishEcho/bu32 sInt
	public void publishEcho(float myFloat, int myByte, float secondFloat) {
		log.info("myFloat {} {} {} ", myFloat, myByte, secondFloat);
	}

	public void echo(float myFloat, int myByte, float secondFloat) {
		msg.echo(myFloat, myByte, secondFloat);
	}

	/**
	 * return heartbeat - prevents resetting
	 */
	// < publishHeartbeat
	public void publishHeartbeat() {
		heartbeat = true;
	}

	/**
	 * 
	 * @param deviceId
	 * @param data
	 */
	// < publishI2cData/deviceId/[] data
	public void publishI2cData(Integer deviceId, int[] data) {
		log.info("publishI2cData");
		i2cReturnData(data);
	}

	// < publishMRLCommError/str errorMsg
	public String publishMRLCommError(String errorMsg/* str */) {
		log.error(errorMsg);
		return errorMsg;
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */
	public PinData publishPin(PinData pinData) {
		// caching last value
		pinIndex.get(pinData.address).setValue(pinData.value);
		return pinData;
	}

	public Integer getAddress(String pinName) {
		if (pinMap.containsKey(pinName)) {
			return pinMap.get(pinName).getAddress();
		}
		return null;
	}

	// < publishPinArray/[] data
	public PinData[] publishPinArray(int[] data) {
		log.info("publishPinArray {}", data);
		// if subscribers -
		// look for subscribed pins and publish them

		int pinDataCnt = data.length / 3;
		PinData[] pinArray = new PinData[pinDataCnt];

		// parse sort reduce ...
		for (int i = 0; i < pinArray.length; ++i) {
			PinData pinData = new PinData(data[3 * i], Serial.bytesToInt(data, (3 * i) + 1, 2));
			pinArray[i] = pinData;
			int address = pinData.address;

			// handle individual pins
			if (pinListeners.containsKey(address)) {
				List<PinListener> list = pinListeners.get(address);
				for (int j = 0; j < list.size(); ++j) {
					PinListener pinListner = list.get(j);
					if (pinListner.isLocal()) {
						pinListner.onPin(pinData);
					} else {
						invoke("publishPin", pinData);
					}
				}
			}
		}

		for (String name : pinArrayListeners.keySet()) {
			PinArrayListener pal = pinArrayListeners.get(name);
			pal.onPinArray(pinArray);
		}
		return pinArray;
	}

	// FIXME - reconcile - Arduino's input is int[] - this one is not used
	@Override
	public PinData[] publishPinArray(PinData[] pinData) {
		return pinData;
	}

	/**
	 * method to communicate changes in pinmode or state changes
	 * 
	 * @param pinDef
	 * @return
	 */
	public PinDefinition publishPinDefinition(PinDefinition pinDef) {
		return pinDef;
	}

	public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
		SerialRelayData serialData = new SerialRelayData(deviceId, data);
		return serialData;
	}

	// FIXME - ask kwatters what he wants PinDefinition A0 ???
	public PinData publishTrigger(Pin pin) {
		return null;
	}

	// FIXME should be in Control interface - for callback
	// < publishUltrasonicSensorData/deviceId/b16 echoTime
	public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime) {
		// log.info("echoTime {}", echoTime);
		((UltrasonicSensor) getDevice(deviceId)).onUltrasonicSensorData(echoTime.doubleValue());
		return echoTime;
	}

	@Override
	public int read(int address) { // FIXME - block on real read ???
		return pinIndex.get(address).getValue();
	}

	@Override
	public int read(String pinName) {
		return read(pinNameToAddress(pinName));
	}

	// FIXME put recording into generated Msg
	@Override
	public void record() throws Exception {
		msg.record();
	}

	/*
	 * public void refresh() { serial.getPortNames(); broadcastState(); }
	 */

	@Override
	public void releaseI2cDevice(I2CControl control, int busAddress, int deviceAddress) {
		// This method should delete the i2c device entry from the list of
		// I2CDevices
		String key = String.format("%d.%d", busAddress, deviceAddress);
		if (i2cDevices.containsKey(key)) {
			i2cDevices.remove(key);
		}
	}

	@Override
	public void releaseService() {
		super.releaseService();
		sleep(300);
		disconnect();
	}

	/**
	 * resets both MrlComm-land & Java-land
	 */
	public void reset() {
		log.info("reset - resetting all devices");

		// reset MrlComm-land
		softReset();

		for (String name : deviceList.keySet()) {
			DeviceMapping dmap = deviceList.get(name);
			DeviceControl device = dmap.getDevice();
			log.info("unsetting device {}", name);
			device.detach(name);
		}

		// reset Java-land
		deviceIndex.clear();
		deviceList.clear();
		error_mrl_to_arduino_rx_cnt = 0;
		error_arduino_to_mrl_rx_cnt = 0;
	}

	public void serialAttach(SerialRelay serialRelay, int controllerAttachAs) {
		Integer deviceId = attachDevice(serialRelay, new Object[] { controllerAttachAs });
		msg.serialAttach(deviceId, controllerAttachAs);
	}

	@Override
	public void attach(ServoControl servo) throws Exception {
		if (isAttached(servo)) {
			log.info("servo {} already attached", servo.getName());
			return;
		}
		// query configuration out
		int pin = servo.getPin();
		// targetOutput is ALWAYS ALWAYS degreeees
		double targetOutput = servo.getTargetOutput();
		double velocity = servo.getVelocity();

		// this saves original "attach" configuration - and maintains internal
		// data
		// structures
		// and does DeviceControl.attach(this)
		Integer deviceId = attachDevice(servo, new Object[] { pin, targetOutput, velocity });

		// send data to micro-controller - convert degrees to microseconds
		int uS = degreeToMicroseconds(targetOutput);
		msg.servoAttach(deviceId, pin, uS, (int) velocity, servo.getName());

		// the callback - servo better have a check
		// isAttached(ServoControl) to prevent infinite loop
		// servo.attach(this, pin, targetOutput, velocity);
		servo.attach(this);
	}

	public boolean isAttached(DeviceControl device) {
		return deviceList.containsKey(device.getName());
	}

	@Override
	public void attach(ServoControl servo, int pin) throws Exception {
		servo.setPin(pin);
		attach(servo);
	}

	/**
	 * Arduino's servo.attach(pin) which is just energizing on a pin
	 */
	@Override
	// > servoEnablePwm/deviceId/pin
	public void servoAttachPin(ServoControl servo, int pin) {
		log.info("{}.attachPin({})", servo.getName(), servo.getPin());
		msg.servoAttachPin(getDeviceId(servo), pin);
	}

	@Override
	// > servoDisablePwm/deviceId
	public void servoDetachPin(ServoControl servo) {
		log.info("{}.detachPin({})", servo.getName(), servo.getPin());
		msg.servoDetachPin(getDeviceId(servo));
	}

	@Override
	// > servoSetVelocity/deviceId/b16 velocity
	public void servoSetVelocity(ServoControl servo) {
		log.info("servoSetVelocity {} id {} velocity {}", servo.getName(), getDeviceId(servo),
				(int) servo.getVelocity());
		msg.servoSetVelocity(getDeviceId(servo), (int) servo.getVelocity());
	}

	// FIXME - this needs fixing .. should be microseconds - but interface still
	// needs
	// to be in degrees & we don't want to pass double over serial lines
	@Override
	// > servoSweepStart/deviceId/min/max/step
	public void servoSweepStart(ServoControl servo) {
		int deviceId = getDeviceId(servo);
		log.info(String.format("servoSweep %s id %d min %d max %d step %d", servo.getName(), deviceId, servo.getMin(),
				servo.getMax(), servo.getVelocity()));
		msg.servoSweepStart(deviceId, (int) servo.getMin(), (int) servo.getMax(), (int) servo.getVelocity());
	}

	@Override
	// > servoSweepStop/deviceId
	public void servoSweepStop(ServoControl servo) {
		msg.servoSweepStop(getDeviceId(servo));
	}

	/**
	 * servo.write(angle) https://www.arduino.cc/en/Reference/ServoWrite The msg
	 * to mrl will always contain microseconds - but this method will (like the
	 * Arduino Servo.write) accept both degrees or microseconds. The code is
	 * ported from Arduino's Servo.cpp
	 */
	@Override
	// > servoWrite/deviceId/target
	public void servoMoveTo(ServoControl servo) {
		int deviceId = getDeviceId(servo);
		// getTargetOutput ALWAYS ALWAYS Degrees !
		// so we convert to microseconds
		int us = degreeToMicroseconds(servo.getTargetOutput());
		log.info("servoMoveToMicroseconds servo {} id {} {}->{} us", servo.getName(), deviceId, servo.getPos(), us);
		msg.servoMoveToMicroseconds(deviceId, us);
	}

	/**
	 * On standard servos a parameter value of 1000 is fully counter-clockwise,
	 * 2000 is fully clockwise, and 1500 is in the middle.
	 */
	@Override
	// > servoWriteMicroseconds/deviceId/b16 ms
	public void servoWriteMicroseconds(ServoControl servo, int uS) {
		int deviceId = getDeviceId(servo);
		log.info(String.format("writeMicroseconds %s %d id %d", servo.getName(), uS, deviceId));
		// msg.servoWriteMicroseconds(deviceId, uS);
		// lets use speed control
		msg.servoMoveToMicroseconds(deviceId, uS);
	}

	public String setBoard(String board) {
		log.debug("setting board to type {}", board);
		this.boardType = board;
		createPinList();
		// broadcastState();
		return board;
	}

	/**
	 * easy way to set to a 54 pin arduino
	 *
	 * @return
	 */
	public String setBoardMega() {
		boardType = BOARD_TYPE_MEGA;
		createPinList();
		broadcastState();
		return boardType;
	}

	public String setBoardUno() {
		boardType = BOARD_TYPE_UNO;
		createPinList();
		broadcastState();
		return boardType;
	}

	public String setBoardNano() {
		boardType = BOARD_TYPE_NANO;
		createPinList();
		broadcastState();
		return boardType;
	}

	public String setBoardMegaADK() {
		boardType = BOARD_TYPE_MEGA_ADK;
		createPinList();
		broadcastState();
		return boardType;
	}

	/**
	 * DeviceControl methods. In this case they represents the I2CBusControl Not
	 * sure if this is good to use the Arduino as an I2CBusControl Exploring
	 * different alternatives. I may have to rethink. Alternate solutions are
	 * welcome. /Mats.
	 */

	/**
	 * Debounce ensures that only a single signal will be acted upon for a
	 * single opening or closing of a contact. the delay is the min number of pc
	 * cycles must occur before a reading is taken
	 *
	 * Affects all reading of pins setting to 0 sets it off
	 *
	 * TODO - implement on MrlComm side ...
	 * 
	 * @param delay
	 */
	// > setDebounce/pin/delay
	public void setDebounce(int pin, int delay) {
		msg.setDebounce(pin, delay);
	}

	// > setDebug/bool enabled
	public void setDebug(boolean b) {
		msg.setDebug(b);
	}

	/**
	 * dynamically change the serial rate TODO - shouldn't this change Arduino
	 * service serial rate too to match?
	 * 
	 * @param rate
	 */
	// > setSerialRate/b32 rate
	public void setSerialRate(int rate) {
		msg.setSerialRate(rate);
	}

	public void setSketch(Sketch sketch) {
		this.sketch = sketch;
		broadcastState();
	}

	/**
	 * set a pin trigger where a value will be sampled and an event will be
	 * signal when the pin turns into a different state.
	 * 
	 * TODO - implement on MrlComm side...
	 */
	// > setTrigger/pin/triggerValue
	public void setTrigger(int pin, int value) {
		msg.setTrigger(pin, value);
	}

	/**
	 * send a reset to MrlComm - all devices removed, all polling is stopped and
	 * all other counters are reset
	 */
	// > softReset
	public void softReset() {
		msg.softReset();
	}

	@Override
	public void startService() {
		super.startService();
		try {
			if (msg == null) {
				serial = (Serial) startPeer("serial");
				msg = new Msg(this, serial);
				// FIXME - dynamically additive - if codec key has never been
				// used -
				// add key
				// serial.getOutbox().setBlocking(true);
				// inbox.setBlocking(true);
				serial.addByteListener(this);
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public void stopRecording() {
		msg.stopRecording();
	}

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
	}

	public void test() {
		int[] config = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		for (int i = 0; i < 10000; ++i) {
			// msg.echo("name1", 8, 999999999, 121212, 254, "name2", config,
			// 5454545);
			// b32 long int 4 bytes -2,147,483,648 to 2,147,483, 647
			// bu32 unsigned long long 0 to 4,294,967,295

			// 2147483647
			// 2139062143
			// msg.echo(2147483647, "hello", 33, 2147483647L, 32767, 25, "oink
			// oink", config, 2147483647L);
			msg.echo(3.14159F, 17, 345.123F);
			// msg.echo(32767, "hello 1", 127, 2147418111, 32767, 8, "name 2 is
			// here", config, 534332);
			// 2147418111
			// 2147352575
		}
	}

	@Override
	public void attach(UltrasonicSensorControl sensor, Integer triggerPin, Integer echoPin) throws Exception {
		// refer to
		// http://myrobotlab.org/content/control-controller-manifesto
		if (isAttached(sensor)) {
			log.info("{} already attached", sensor.getName());
			return;
		}

		// critical init code
		Integer deviceId = attachDevice(sensor, new Object[] { triggerPin, echoPin });
		msg.ultrasonicSensorAttach(deviceId, triggerPin, echoPin);

		// call the other service's attach
		sensor.attach(this, triggerPin, echoPin);
	}

	@Override
	// > ultrasonicSensorStartRanging/deviceId/b32 timeout
	public void ultrasonicSensorStartRanging(UltrasonicSensorControl sensor) {
		msg.ultrasonicSensorStartRanging(getDeviceId(sensor));
	}

	@Override
	// > ultrasonicSensorStopRanging/deviceId
	public void ultrasonicSensorStopRanging(UltrasonicSensorControl sensor) {
		msg.ultrasonicSensorStopRanging(getDeviceId(sensor));
	}

	public void uploadSketch(String arduinoPath) throws IOException {
		uploadSketch(arduinoPath, serial.getLastPortName());
	}

	public void uploadSketch(String arudinoPath, String comPort) throws IOException {
		uploadSketch(arudinoPath, comPort, getBoardType());
	}

	public void uploadSketch(String arduinoIdePath, String port, String type) throws IOException {
		log.info("uploadSketch ({}, {}, {})", arduinoIdePath, port, type);
		// hail mary - if we have no idea
		// guess uno
		if (type == null || type.equals("")) {
			type = BOARD_TYPE_UNO;
		}

		arduinoIdePath = arduinoIdePath.replace("\\", "/");
		arduinoIdePath = arduinoIdePath.trim();
		if (!arduinoIdePath.endsWith("/")) {
			arduinoIdePath += "/";
		}

		log.info(String.format("arduino IDE Path=%s", arduinoIdePath));
		log.info(String.format("Port=%s", port));
		log.info(String.format("type=%s", type));
		if (arduinoIdePath != null && !arduinoIdePath.equals(ArduinoUtils.arduinoPath)) {
			this.arduinoPath = arduinoIdePath;
			ArduinoUtils.arduinoPath = arduinoIdePath;
			save();
		}

		uploadSketchResult = String.format("Uploaded %s ", new Date());

		boolean connectedState = isConnected();
		try {

			if (connectedState) {
				log.info("disconnecting...");
				disconnect();
			}
			ArduinoUtils.uploadSketch(port, type.toLowerCase());

		} catch (Exception e) {
			log.info("ArduinoUtils threw trying to upload", e);
		}

		if (connectedState) {
			log.info("reconnecting...");
			serial.connect();
		}

		// perhaps you can reduce the inter-process information
		// to succeed | fail .. perhaps you can't
		// I would prefer transparency - send all output to the ui
		uploadSketchResult += ArduinoUtils.getOutput();

		log.info(uploadSketchResult);
		broadcastState();
	}

	/**
	 * PinArrayControl method
	 */
	@Override
	public void write(int address, int value) {
		info("write (%d,%d) to %s", address, value, serial.getName());

		PinDefinition pinDef = pinIndex.get(address);

		if (pinDef.isPwm()) {
			analogWrite(address, value);
		} else {
			digitalWrite(address, value);
		}
		// cache value
		pinDef.setValue(value);
	}

	public int getDeviceCount() {
		return deviceList.size();
	}

	@Override
	public Set<String> getDeviceNames() {
		return deviceList.keySet();
	}

	@Override
	public void detach(String controllerName) {
		// GOOD DESIGN !!! - THIS HAS INPUT STRING - AND WILL
		// ROUTE WITH THE APPROPRIATE TYPE - AUTO-MAGICALLY !!!
		invoke("detach", Runtime.getService(controllerName));
	}

	// GOOD DESIGN !!!
	@Override
	public boolean isAttached(String name) {
		return deviceList.containsKey(name);
	}

	@Override
	public Set<String> getAttached() {
		return deviceList.keySet();
	}

	public void openMrlComm() {
		try {
			String mrlCommFiles = null;
			if (FileIO.isJar()) {
				mrlCommFiles = "resource/Arduino/MrlComm";
				Zip.extractFromSelf("resource/Arduino/MrlComm", "resource/Arduino/MrlComm");
			} else {
				// running in IDE ?
				mrlCommFiles = "src/resource/Arduino/MrlComm";
			}
			File mrlCommDir = new File(mrlCommFiles);
			if (!mrlCommDir.exists() || !mrlCommDir.isDirectory()) {
				error("%s is not a valid directory", mrlCommDir);
				return;
			}
			String exePath = FileIO.gluePaths(arduinoPath, ArduinoUtils.getExeName());
			String inoPath = FileIO.gluePaths(mrlCommDir.getAbsolutePath(), "/MrlComm.ino");
			List<String> cmd = new ArrayList<String>();
			cmd.add(exePath);
			cmd.add(inoPath);
			ProcessBuilder builder = new ProcessBuilder(cmd);
			builder.start();

		} catch (Exception e) {
			error(String.format("%s %s", e.getClass().getSimpleName(), e.getMessage()));
			log.error("openMrlComm threw", e);
		}
	}

	@Override
	public void servoSetAcceleration(ServoControl servo) {
		msg.servoSetAcceleration(getDeviceId(servo), (int) servo.getAcceleration());
	}

	public void setArduinoPath(String path) {
		arduinoPath = path;
	}

	public String getArduinoPath() {
		return arduinoPath;
	}

	public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
		// TODO Auto-generated method stub
		((Servo) getDevice(deviceId)).onServoEvent(eventType, currentPos, targetPos);
		return currentPos;
	}


	@Override
	public String publishConnect(String portName) {
		return portName;
	}

	@Override
	public String publishDisconnect(String portName) {
		return portName;
	}

	@Override
	public List<String> getPortNames() {
		if (serial != null) {
			return serial.getPortNames();
		}
		return new ArrayList<String>();
	}
	
	public static void main(String[] args) {
		try {

			LoggingFactory.init(Level.WARN);

			// Runtime.start("webgui", "WebGui");
			Runtime.start("a", "SwingGui");
			Runtime.start("cli", "Cli");
			RemoteAdapter remote = (RemoteAdapter)Runtime.start("ra", "RemoteAdapter");
			
			// Runtime.start("python", "Python");

			VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
			virtual.connect("COM78");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect("COM78");
						
			Servo servo = (Servo) Runtime.start("servo", "Servo");
			servo.attach(arduino, 8, 90);
			
			Runtime.start("webgui", "WebGui");
			Service.sleep(3000);
			
			remote.startListening();
			
			// Runtime.start("cli", "Cli");
			// Runtime.start("webgui", "WebGui");


		} catch (Exception e) {
			Logging.logError(e);
		}
	}


}