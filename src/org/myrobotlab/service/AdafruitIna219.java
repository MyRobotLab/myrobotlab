package org.myrobotlab.service;

import java.util.ArrayList;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

//import com.pi4j.io.i2c.I2CBus;
/**
 * AdaFruit Ina219 Shield Controller Service
 * 
 * @author Mats
 * 
 *         References : https://www.adafruit.com/products/904
 */
public class AdafruitIna219 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AdafruitIna219.class);
	transient I2CControl controller;

	public static final byte INA219_SHUNTVOLTAGE = 0x01;
	public static final byte INA219_BUSVOLTAGE = 0x02;

	// Default i2cAddress
	public int busAddress = 1;
	public int deviceAddress = 0x40;
	public String type = "AdafruitIna219";

	public int busVoltage = 0;
	public int shuntVoltage = 0;
	public double current = 0.0;
	public double power = 0.0;

	// TODO Add methods to calibrate
	// Currently only supports setting the shunt resistance to a different
	// value than the default, in case it has been exchanged to measure
	// a different range of current
	public double shuntResistance = 0.1; // expressed in Ohms
	public int scaleRange = 32; // 32V = bus full-scale range
	public int pga = 8; // 320 mV = shunt full-scale range

	public ArrayList<String> controllers;
	public String controllerName;
	private boolean isAttached = false;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			AdafruitIna219 adafruitINA219 = (AdafruitIna219) Runtime.start("AdafruitIna219", "AdafruitIna219");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public AdafruitIna219(String n) {
		super(n);

		ServiceInterface runtime = Runtime.createAndStart("runtime", "Runtime");
		subscribe(runtime.getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();

	}

	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CControl.class);
		return controllers;
	}

	/**
	 * This methods sets the i2c Controller that will be used to communicate with
	 * the i2c device
	 */
	// @Override
	public boolean setController(String controllerName) {
		return setController((I2CControl) Runtime.getService(controllerName));
	}

	/**
	 * This methods sets the i2c Controller that will be used to communicate with
	 * the i2c device
	 */
	public boolean setController(I2CControl controller) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}
		controllerName = controller.getName();
		this.controller = controller;
		isAttached = true;

		log.info(String.format("%s setController %s", getName(), controllerName));
		
		broadcastState();
		return true;
	}

	public void unsetController() {
		controller = null;
		controllerName = null;
		isAttached = false;
		broadcastState();
	}

	public I2CControl getController() {
		return controller;
	}

	public String getControllerName() {

		String controlerName = null;

		if (controller != null) {
			controlerName = controller.getName();
		}

		return controlerName;
	}

	public boolean isAttached() {
		return controller != null;
	}

	/**
	 * This method creates the i2c device
	 */
	boolean setDeviceAddress(int DeviceAddress) {
		if (controller != null) {
			if (deviceAddress != DeviceAddress) {
				controller.releaseDevice(busAddress, deviceAddress);
				controller.createDevice(busAddress, DeviceAddress, type);
			}
		}
		log.info(String.format("Setting device address to x%02X", deviceAddress));
		deviceAddress = DeviceAddress;
		return true;
	}

	/**
	 * This method sets the shunt resistance in ohms Default value is .1 Ohms (
	 * R100 )
	 */
	void setShuntResistance(double ShuntResistance) {
		shuntResistance = ShuntResistance;
	}

	/**
	 * This method reads and returns the power in milliWatts
	 */
	public void refresh() {

		double power = getPower();
		broadcastState();
	}

	double getPower() {
		power = getBusVoltage() * getCurrent();
		return power;
	}

	/**
	 * This method reads and returns the shunt current in milliAmperes
	 */
	double getCurrent() {
		current = getShuntVoltage() / shuntResistance;
		return current;
	}

	/**
	 * This method reads and returns the shunt Voltage in milliVolts
	 */
	double getShuntVoltage() {
		byte[] writebuffer = { INA219_SHUNTVOLTAGE };
		byte[] readbuffer = { 0x0, 0x0 };
		controller.i2cWrite(busAddress, deviceAddress, writebuffer, writebuffer.length);
		controller.i2cRead(busAddress, deviceAddress, readbuffer, readbuffer.length);
		// log.info(String.format("getShuntVoltage x%02X x%02X", readbuffer[0],
		// readbuffer[1]));
		shuntVoltage = (((int) (readbuffer[0]) << 8) + ((int) readbuffer[1] & 0xff));
		return shuntVoltage;
	}

	/**
	 * This method reads and returns the bus Voltage in milliVolts
	 */
	double getBusVoltage() {
		byte[] writebuffer = { INA219_BUSVOLTAGE };
		byte[] readbuffer = { 0x0, 0x0 };
		controller.i2cWrite(busAddress, deviceAddress, writebuffer, writebuffer.length);
		controller.i2cRead(busAddress, deviceAddress, readbuffer, readbuffer.length);
		// log.info(String.format("getBusVoltage x%02X x%02X", readbuffer[0],
		// readbuffer[1]));
		busVoltage = (((int) (readbuffer[0]) << 8 & 0xffff) + ((int) readbuffer[1] & 0xf8)) * 4;
		return busVoltage;
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(AdafruitIna219.class.getCanonicalName());
		meta.addDescription("Adafruit INA219 Voltage and Current sensor Service");
		meta.addCategory("sensor");
		meta.setSponsor("Mats");
		return meta;
	}

}
