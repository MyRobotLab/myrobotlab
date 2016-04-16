package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.slf4j.Logger;

import com.pi4j.io.i2c.I2CBus;
/**
 * AdaFruit INA219 Shield Controller Service
 * 
 * @author Mats
 * 
 *         References : https://www.adafruit.com/products/904
 */
public class AdafruitINA219 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);
	transient I2CControl controller;
	
	public static final byte INA219_SHUNTVOLTAGE = 0x1;
	public static final byte INA219_BUSVOLTAGE   = 0x2;
	
    // Default i2cAddress
	public int busAddress = I2CBus.BUS_1;
	public int deviceAddress = 0x40;
	public String type = "INA219";
	
	public double busVoltage;
	public double shuntVoltage;
	public double current;
	public double power;
	
	// TODO Add methods to calibrate 
	//      Currently only supports setting the shunt resistance to a different
	//      value than the default, in case it has been exchanged to measure
	//      a different range of current
	public double shuntResistance = 0.1;  // expressed in Ohms
	public int    scaleRange = 32;        // 32V = bus full-scale range 
	public int    pga = 8;		          // 320 mV = shunt full-scale range 
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			//AdafruitINA219 adafruitINA219 = (AdafruitINA219) Runtime.start("AdafruitINA219", "AdafruitINA219");
			//Runtime.start("gui", "GUIService");
			double shuntVoltage;
			byte[] readbuffer = {(byte)0xE0,(byte)0xC0}; 
			// pga = 8
			// shuntVoltage = (double)(((short)(readbuffer[0])<<8) + ((short)readbuffer[1] & 0xff)) * 0.00001;
			// pga = 4
			shuntVoltage = (double)(((short)(readbuffer[0])<<8) + ((short)readbuffer[1] & 0xff)) * 0.00001;
			// pga = 2
			shuntVoltage = (double)(((short)(readbuffer[0])<<8) + ((short)readbuffer[1] & 0xff)) * 0.00001;
			log.info(String.format("shuntVoltage %s", shuntVoltage));
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public AdafruitINA219(String n) {
		super(n);
		// TODO Auto-generated constructor stub
	}
	/**
	 * AdaFruit INA219 Shield Controller Service
	 * 
	 * @author Mats
	 * 
	 *         References : https://www.adafruit.com/products/904
	 */
	public boolean setController(I2CControl controller) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}

		log.info(String.format("%s setController %s", getName(), controller.getName()));

		this.controller = controller;
		controller.createDevice(busAddress, deviceAddress, type);
		broadcastState();
		return true;
	}
	/**
	 * This method creates the i2c device 
	 */
	boolean setDeviceAddress(int DeviceAddress){
		if (controller != null) {
			if (deviceAddress != DeviceAddress){
				controller.releaseDevice(busAddress,deviceAddress);
				controller.createDevice(busAddress, DeviceAddress, type);
			}
		}
		log.info(String.format("Setting device address to x%02X", deviceAddress));
		deviceAddress = DeviceAddress;
		return true;
	}
	/**
	 * This method sets the shunt resistance in ohms
	 * Default value is .1 Ohms ( R100 ) 
	 */
	void setShuntResistance(double ShuntResistance){
		shuntResistance = ShuntResistance;
	}
	/**
	 * This method reads and returns the power in Watts
	 */
	double getPower(){
		power = getBusVoltage() * getCurrent();
		return power;
	}
	/**
	 * This method reads and returns the shunt current in Amperes
	 */
	double getCurrent(){
		current = getShuntVoltage() / shuntResistance;
		return current;
	}
	
	/**
	 * This method reads and returns the shunt Voltage in Volts
	 */
	double getShuntVoltage(){
		byte[] writebuffer = {INA219_SHUNTVOLTAGE}; 
		byte[] readbuffer = {0x0,0x0}; 
		controller.i2cWrite(busAddress, deviceAddress, writebuffer, writebuffer.length);
		controller.i2cRead(busAddress, deviceAddress, readbuffer, readbuffer.length);
		log.info(String.format("getShuntVoltage x%02X x%02X", readbuffer[0], readbuffer[1]));
		shuntVoltage = (double)(((short)(readbuffer[0])<<8) + ((short)readbuffer[1] & 0xff)) * 0.00001;
		return shuntVoltage;
	}
	
	/**
	 * This method reads and returns the bus Voltage in Volts
	 */
	double getBusVoltage(){
		int scale = 250;
		byte[] writebuffer = {INA219_BUSVOLTAGE}; 
		byte[] readbuffer = {0x0,0x0}; 
		controller.i2cWrite(busAddress, deviceAddress, writebuffer, writebuffer.length);
		controller.i2cRead(busAddress, deviceAddress, readbuffer, readbuffer.length);
		log.info(String.format("getBusVoltage x%02X x%02X", readbuffer[0], readbuffer[1]));
		busVoltage = (double)(((int)(readbuffer[0])<<5 & 0x7fff) + ((int)readbuffer[1]>>3 & 0x7f)) / scale;
		return busVoltage;
	}
	
	/**
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(AdafruitINA219.class.getCanonicalName());
		meta.addDescription("Adafruit INA219 Voltage and Current sensor Service");
		meta.addCategory("sensor");	
		return meta;		
	}

	
}
