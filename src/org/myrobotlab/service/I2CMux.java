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

/**
 * 
 * I2CMux - This is the MyRobotLab Service that can be used if you have several i2c devices that
 * share the same address. Create one I2CMux for each of the i2c buses that you want to use.
 * It can be used with tca9548a and possibly other devices.
 * 
 * 
 * @author Mats Önnerby
 *  
 * More Info : https://www.adafruit.com/product/2717
 * 
 */
public class I2CMux extends Service implements I2CControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(I2CMux.class.getCanonicalName());

	transient I2CControl controller;

	public ArrayList<String> controllers = new ArrayList<String>();
	public String controllerName;
	
	public ArrayList<String> muxAddressList = new ArrayList<String>();
	public String muxAddress = "0x70";
	
	public ArrayList<String> muxBusList = new ArrayList<String>();
	public String muxBus = "0";
	
	private boolean isAttached = false;

	public String muxType = "I2CMux";

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			I2CMux i2cMux = (I2CMux) Runtime.start("i2cMux", "I2CMux");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public I2CMux(String n) {
		super(n);
		
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
		createMuxAddressList();
		createMuxBusList();
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
	
	private void createMuxAddressList() {
		for (int i=0x70; i <= 0x77 ; i++){
			  String listitem = String.format("0x%02X",i);
				muxAddressList.add(listitem);
		}
	}
	
	public void SetMuxAddress(String muxAddress){
			this.muxAddress = muxAddress;
			broadcastState();
	}
	
	private void createMuxBusList() {
		for (int i=0; i <=8 ; i++){
		  	String listitem = String.format("%s",i);
				muxBusList.add(listitem);
		}
	}
	
	
	public void SetMuxBus(String muxBus){
			this.muxBus = muxBus;
			broadcastState();
	}
	
	public void createDevice(int busAddress, int deviceAddress, String type) {
				controller.createDevice(busAddress, deviceAddress, type);
	}
	
	/**
	 * This methods sets the i2c Controller that will be used to communicate with
	 * the i2c device
	 */
	// @Override
	public boolean setController(String controllerName) {
		return setController((I2CControl) Runtime.getService(controllerName));
	}
	
	public boolean setController(String controllerName, String muxAddress, String muxBus) {
		this.muxAddress = muxAddress;
		this.muxBus = muxBus;
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
		return isAttached;
	}

	@Override
	public void releaseDevice(int busAddress, int deviceAddress) {
			controller.releaseDevice(busAddress, deviceAddress);
	}

	public void setMuxBus(int busAddress) {
		byte buffer[] = new byte[1];
		int muxAddressInt = Integer.decode(muxAddress);
		buffer[0] = (byte) (1 << Integer.parseInt(muxBus));
		controller.i2cWrite(busAddress, muxAddressInt, buffer, buffer.length);
		;
	}
	
	@Override
	public void i2cWrite(int busAddress, int deviceAddress, byte[] buffer, int size) {
		setMuxBus(busAddress);
		String key = String.format("%d.%d", busAddress, deviceAddress);
		log.debug(String.format("i2cWrite busAddress x%02X deviceAddress x%02X key %s", busAddress, deviceAddress, key));
		controller.i2cWrite(busAddress, deviceAddress, buffer, size);
		;
	}

	@Override
	public int i2cRead(int busAddress, int deviceAddress, byte[] buffer, int size) {
		setMuxBus(busAddress);
		controller.i2cRead(busAddress, deviceAddress, buffer, size);
		return buffer.length;
	}

	@Override
	public int i2cWriteRead(int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
		setMuxBus(busAddress);
		controller.i2cWriteRead(busAddress, deviceAddress, writeBuffer, writeSize, readBuffer, readSize);
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

		ServiceType meta = new ServiceType(I2CMux.class.getCanonicalName());
		meta.addDescription("Multiplexer for i2c to be able to use multiple i2c devices");
		meta.addCategory("i2c", "control");
    meta.setSponsor("Mats");
		return meta;
	}

}