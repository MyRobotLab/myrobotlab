package org.myrobotlab.service.interfaces;

import java.io.IOException;
/*
 * This interface is based on the methods for i2c read and write in the Pi4J project
 * It's used by RasPi and Arduino services to be able to use the same
 * device drivers either connected directly to the GPIO pins of a Raspberry PI or
 * to the ic2 bus on an Arduino. 
 */
public interface I2CControl {

/**
* This method writes several bytes to the i2c device from given buffer at given offset.
* 
* @param address local address in the i2c device
* @param buffer buffer of data to be written to the i2c device in one go
* @param offset offset in buffer 
* @param size number of bytes to be written 
* 
* @throws IOException thrown in case byte cannot be written to the i2c device or i2c bus
*/
void i2cWrite(int address, byte[] buffer, int offset, int size);
/**
* This method reads bytes from the i2c device to given buffer at asked offset. 
* 
* @param buffer buffer of data to be read from the i2c device in one go
* @param offset offset in buffer 
* @param size number of bytes to be read 
* 
* @return number of bytes read
* 
* @throws IOException thrown in case byte cannot be read from the i2c device or i2c bus
*/
int i2cRead(byte[] buffer, int offset, int size);

/**
* This method reads bytes from the i2c device to given buffer at asked offset. 
* 
* @param address local address in the i2c device
* @param buffer buffer of data to be read from the i2c device in one go
* @param offset offset in buffer 
* @param size number of bytes to be read 
* 
* @return number of bytes read
* 
* @throws IOException thrown in case byte cannot be read from the i2c device or i2c bus
*/
int i2cRead(int address, byte[] buffer, int offset, int size);

/**
* This method writes and reads bytes to/from the i2c device in a single method call
*
* @param writeBuffer buffer of data to be written to the i2c device in one go
* @param writeOffset offset in write buffer
* @param writeSize number of bytes to be written from buffer
* @param readBuffer buffer of data to be read from the i2c device in one go
* @param readOffset offset in read buffer
* @param readSize number of bytes to be read
*
* @return number of bytes read
*
* @throws IOException thrown in case byte cannot be read from the i2c device or i2c bus
*/
int i2CRead(byte[] writeBuffer, int writeOffset, int writeSize, byte[] readBuffer, int readOffset, int readSize);
	
}