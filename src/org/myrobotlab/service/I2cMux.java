package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * 
 * I2CMux - This is the MyRobotLab Service that can be used if you have several i2c devices that
 * share the same address. Create one I2CMux for each of the i2c buses that you want to use.
 * It can be used with tca9548a and possibly other devices.
 * 
 * 
 * @author Mats Onnerby
 *  
 * More Info : https://www.adafruit.com/product/2717
 * 
 */
public class I2cMux extends Service implements I2CControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(I2cMux.class.getCanonicalName());

	transient I2CControl controller;

	public ArrayList<String> controllers = new ArrayList<String>();
	public String controllerName;
	
	public List<String> deviceAddressList = Arrays.asList(
			 "0x70","0x71","0x72","0x73","0x74","0x75","0x76","0x77");
			
	public String deviceAddress = "0x70";
	
	public List<String> deviceBusList = Arrays.asList(
			"0","1","2","3","4","5","6","7","8");	
	public String deviceBus = "1";
	
	private boolean isAttached = false;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			I2cMux i2cMux = (I2cMux) Runtime.start("i2cMux", "I2CMux");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public I2cMux(String n) {
		super(n);
		
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();
	}

   public void refreshControllers() {
  	 
  	controllers = Runtime.getServiceNamesFromInterface(I2CControl.class);
		controllers.remove(this.getName());
		
		broadcastState();
	}
	
 	public void SetDeviceBus(String deviceBus){
		this.deviceBus = deviceBus;
		broadcastState();
  }
	
	public void SetDeviceAddress(String deviceAddress){
		this.deviceAddress = deviceAddress;
		broadcastState();
  }

	public void createI2cDevice(int busAddress, int deviceAddress, String serviceName) {
				controller.createI2cDevice(busAddress, deviceAddress, this.getName());
	}
	
	/**
	 * This methods sets the i2c Controller that will be used to communicate with
	 * the i2c device
	 */
	// @Override
	public boolean setController(String controllerName, String deviceBus, String deviceAddress) {
		return setController((I2CControl) Runtime.getService(controllerName), deviceBus, deviceAddress);
	}

	public boolean setController(String controllerName) {
		return setController((I2CControl) Runtime.getService(controllerName), this.deviceBus, this.deviceAddress);
	}
	
	public boolean setController(I2CControl controller) {
		return setController(controller, this.deviceBus, this.deviceAddress);
	}
	/**
	 * This methods sets the i2c Controller that will be used to communicate with
	 * the i2c device
	 */
	public boolean setController(I2CControl controller, String deviceBus, String deviceAddress) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}
		controllerName = controller.getName();
		this.controller = controller;
		this.deviceBus = deviceBus;
		this.deviceAddress = deviceAddress;
		isAttached = true;

		log.info(String.format("%s setController %s", getName(), controllerName));
		
		broadcastState();
		return true;
	}

	public void unsetController() {
		controller = null;
		controllerName = null;
		this.deviceBus = null;
		this.deviceAddress = null;
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
		return isAttached;
	}

	@Override
	public void releaseI2cDevice(int busAddress, int deviceAddress) {
			controller.releaseI2cDevice(busAddress, deviceAddress);
	}

	public void setMuxBus(int deviceBus) {
		byte bus[] = new byte[1];
		bus[0] = (byte) (1 << deviceBus);
		controller.i2cWrite(Integer.parseInt(this.deviceBus), Integer.decode(this.deviceAddress), bus, bus.length);
		;
	}
	
	@Override
	public void i2cWrite(int busAddress, int deviceAddress, byte[] buffer, int size) {
		setMuxBus(busAddress);
		String key = String.format("%d.%d", busAddress, deviceAddress);
		log.debug(String.format("i2cWrite busAddress x%02X deviceAddress x%02X key %s", busAddress, deviceAddress, key));
		controller.i2cWrite(Integer.parseInt(this.deviceBus), deviceAddress, buffer, size);
		;
	}
	/**
	 * TODO Add demuxing. i.e the route back to the caller
	 *      The i2c will receive data that neeeds to be returned syncronous
	 *      or asycncronus
	 */
	@Override
	public int i2cRead(int busAddress, int deviceAddress, byte[] buffer, int size) {
		setMuxBus(busAddress);
		controller.i2cRead(Integer.parseInt(this.deviceBus), deviceAddress, buffer, size);
		return buffer.length;
	}
	/**
	 * TODO Add demuxing. i.e the route back to the caller
	 *      The i2c will receive data that neeeds to be returned syncronous
	 *      or asycncronus
	 */
	@Override
	public int i2cWriteRead(int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
		setMuxBus(busAddress);
		controller.i2cWriteRead(Integer.parseInt(this.deviceBus), deviceAddress, writeBuffer, writeSize, readBuffer, readSize);
		return readBuffer.length;
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

		ServiceType meta = new ServiceType(I2cMux.class.getCanonicalName());
		meta.addDescription("Multiplexer for i2c to be able to use multiple i2c devices");
		meta.addCategory("i2c", "control");
    meta.setAvailable(true);
    meta.setSponsor("Mats");
		return meta;
	}

}