package org.myrobotlab.serial.gnu;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceFrameworkFactory;
import org.slf4j.Logger;

public class SerialDeviceFactoryGNU implements SerialDeviceFrameworkFactory {

	public final static Logger log = LoggerFactory.getLogger(SerialDeviceFactory.class.getCanonicalName());

	public ArrayList<String> getSerialDeviceNames() {
		// rxtx - you have to enumerate through port identifiers (bleh)
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<SerialDevice> devices = getSerialDevices();
		for (int i = 0; i < devices.size(); ++i) {
			names.add(devices.get(i).getName());
		}

		return names;
	}

	public SerialDevice getSerialDevice(String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException {

		CommPortIdentifier portId;
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getName().equals(name)) {
				return new SerialDeviceGNU(portId, rate, databits, stopbits, parity);
			}
		}
		return null;
	}

	/**
	 * An ugly way of "simply" getting names
	 * 
	 * @return
	 */
	private ArrayList<SerialDevice> getSerialDevices() {
		ArrayList<SerialDevice> ret = new ArrayList<SerialDevice>();
		CommPortIdentifier portId;
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				SerialDeviceGNU sd;
				try {
					sd = new SerialDeviceGNU(portId);
					ret.add(sd);
				} catch (SerialDeviceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

}
