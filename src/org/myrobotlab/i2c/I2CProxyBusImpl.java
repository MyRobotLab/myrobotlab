package org.myrobotlab.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;

public class I2CProxyBusImpl implements I2CBus {

	private int bus = -1;

	@Override
	public void close() throws IOException {

	}

	@Override
	public I2CDevice getDevice(int bus) throws IOException {
		this.bus = bus;
		return new I2CProxyDeviceImpl();
	}

}
