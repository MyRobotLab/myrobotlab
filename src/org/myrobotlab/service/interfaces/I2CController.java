package org.myrobotlab.service.interfaces;

import java.io.IOException;

/*
 * This interface is based on the methods for i2c read and write in the Pi4J project
 * It's used by RasPi and Arduino services to be able to use the same
 * device drivers either connected directly to the GPIO pins of a Raspberry PI or
 * to the ic2 bus on an Arduino. 
 */
public interface I2CController extends Attachable {
	/**
	 * This method creates a I2CDevice
	 * 
	 * @param busaddress
	 * @param address
	 *          local address in the i2c device
	 * @param serviceName
	 *          name of the service that invokes the createI2cDevice 
	 */
	void i2cAttach(I2CControl control, int busAddress, int deviceAddress);

	/**
	 * This method returns and already existing I2CDevice
	 * 
	 * @param busaddress
	 * @param address
	 *          local address in the i2c device
	 * 
	 */
	void releaseI2cDevice(I2CControl control, int busAddress, int deviceAddress);

	/**
	 * This method writes several bytes to the i2c device from given buffer.
	 * 
	 * @param busaddress
	 * @param address
	 *          local address in the i2c device
	 * @param buffer
	 *          buffer of data to be written to the i2c device in one go
	 * @param size
	 *          number of bytes to be written
	 * 
	 */

	void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size);
  	
	/**
	 * This method reads bytes from the i2c device to given buffer.
	 * 
	 * @param busaddress
	 * @param buffer
	 *          buffer of data to be read from the i2c device in one go
	 * @param size
	 *          number of bytes to be read
	 * 
	 * @return number of bytes read
	 * 
	 */
	int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size);

	/**
	 * This method reads bytes from the i2c device to given buffer.
	 * 
	 * 
	 * /** This method writes and reads bytes to/from the i2c device in a single
	 * method call
	 *
	 * @param busaddress
	 * @param address
	 *          local address in the i2c device
	 * @param writeBuffer
	 *          buffer of data to be written to the i2c device in one go
	 * @param writeSize
	 *          number of bytes to be written from buffer
	 * @param readBuffer
	 *          buffer of data to be read from the i2c device in one go
	 * @param readSize
	 *          number of bytes to be read
	 *
	 * @return number of bytes read
	 *
	 * @throws IOException
	 *           thrown in case byte cannot be read from the i2c device or i2c bus
	 */
	int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize);
  
}