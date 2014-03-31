package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.i2c.I2CFactory;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.wiringpi.SoftPwm;

public class RasPi extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RasPi.class.getCanonicalName());

	// the 2 pins for I2C on the raspberry
	GpioController gpio; 	  
	
	// FIXME - do a 
	GpioPinDigitalOutput gpio01;
	GpioPinDigitalOutput gpio03;

	HashMap<String, Device> devices = new HashMap<String, Device>();

	public static class Device {
		public I2CBus bus;
		public I2CDevice device;
		public String type;
	}
	
	

	public RasPi(String n) {
		super(n);

		log.info(String.format("platform is %s", Platform.getLocalInstance()));
		if ("arm".equals(Platform.getArch())) {
			// init I2C
			gpio = GpioFactory.getInstance();
			gpio01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
			gpio03 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);

		} else {
			// we should be running on a Raspberry Pi
			log.error("architecture is not arm");
		}

	}
	
	/* FIXME - make these methods
	createDigitalAndPwmPin
	public GpioPinDigitalOutput  provisionDigitalOutputPin
	*/

	@Override
	public String getDescription() {
		return "Raspberry Pi service used for accessing specific RasPi hardware such as I2C";
	}

	public void writeRaw(int busAddress, int deviceAddress, byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7, byte d8, byte d9, byte d10, byte d11, byte d12,
			byte d13, byte d14, byte d15) {
		try {
			log.info("--------writeRaw begin -------------");

			log.info(String.format("test %d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15));
			I2CDevice device = getDevice(busAddress, deviceAddress);
			device.write(0x00, new byte[] { d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15 }, 0, 16);

			log.info("--------writeRaw end-------------");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public I2CDevice getDevice(int busAddress, int deviceAddress) {
		try {
			String key = String.format("%d.%d", busAddress, deviceAddress);
			if (!devices.containsKey(key)) {
				// FIXME -- remove put in createDevice
				I2CBus bus = I2CFactory.getInstance(busAddress);
				log.info(String.format("getDevice %d", deviceAddress));
				I2CDevice device = bus.getDevice(deviceAddress);

				Device d = new Device();
				d.bus = bus;
				d.device = device;
				d.type = "display";

				devices.put(key, d);
				return d.device;

			} else {
				return devices.get(key).device;
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	// FIXME - test by reading? writing ?
	// FIXME - return array
	public Integer[] scanI2CDevices(int busAddress) {
		log.info("scanning through I2C devices");
		ArrayList<Integer> list = new ArrayList<Integer>();
		try {
			/*
			 * From its name we can easily deduce that it provides a
			 * communication link between ICs (integrated circuits). I2C is
			 * multimaster and can support a maximum of 112 devices on the bus.
			 * The specification declares that 128 devices can be connected to
			 * the I2C bus, but it also defines 16 reserved addresses.
			 */
			I2CBus bus = I2CFactory.getInstance(busAddress);
			

			for (int i = 0; i < 128; ++i) {
				I2CDevice device = bus.getDevice(i);
				if (device != null) {
					try {
						device.read();
						list.add(i);
						/*
						sb.append(i);
						sb.append(" ");
						*/
						log.info(String.format("found device on address %d", i));
					} catch (Exception e) {
						log.warn(String.format("bad read on address %d", i));
					}

				}
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		Integer[] ret = list.toArray(new Integer[list.size()]);
		return ret;
	}

	// FIXME - create low level I2CDevice
	public I2CDevice createDevice(int busAddress, int deviceAddress, String type) {

		try {

			String key = String.format("%d.%d", busAddress, deviceAddress);
			I2CBus bus = I2CFactory.getInstance(busAddress);

			// PCF8574GpioProvider pcf = new PCF8574GpioProvider(busAddress,
			// deviceAddress);
			// I2CDevice device = bus.getDevice(deviceAddress);

			// PCF8574GpioProvider p = new PCF8574GpioProvider(busAddress,
			// deviceAddress);
			// p.setValue(pin, value)

			if ("com.pi4j.gpio.extension.pcf.PCF8574GpioProvider".equals(type)) {
				Device d = new Device();
				d.bus = bus;
				d.device = (I2CDevice) new PCF8574GpioProvider(busAddress, deviceAddress);
				d.type = d.device.getClass().getCanonicalName();// "PCF8574GpioProvider";
																// // full type
																// name
				devices.put(key, d);
				return d.device;
			} else {
				log.error("could not create device %s", type);
				return null;
			}

		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	// FIXME LOW LEVEL ANY I2C READ OR WRITE !!!
	public byte I2CWrite(int busAddress, int deviceAddress, byte value) {
		I2CDevice device = getDevice(busAddress, deviceAddress);

		if (device == null) {
			error("bus %d device %d not valid", busAddress, deviceAddress);
			return -1;
		}

		try {
			device.write(value);
			return value;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return -1;
	}
	
	
	public void testGPIOOutput(){
		GpioPinDigitalMultipurpose pin = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_02, PinMode.DIGITAL_INPUT, PinPullResistance.PULL_DOWN);

	}
	
	public void testPWM(){
		try {
		
		 // initialize wiringPi library
        com.pi4j.wiringpi.Gpio.wiringPiSetup();

        // create soft-pwm pins (min=0 ; max=100)
        SoftPwm.softPwmCreate(1, 0, 100);

        // continuous loop
        while (true) {            
            // fade LED to fully ON
            for (int i = 0; i <= 100; i++) {
                SoftPwm.softPwmWrite(1, i);
                Thread.sleep(100);
            }

            // fade LED to fully OFF
            for (int i = 100; i >= 0; i--) {
                SoftPwm.softPwmWrite(1, i);
                Thread.sleep(100);
            }
        }
		} catch (Exception e) {
			
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		/*
		 * RasPi.displayString(1, 70, "1");
		 * 
		 * RasPi.displayString(1, 70, "abcd");
		 * 
		 * RasPi.displayString(1, 70, "1234");
		 * 
		 * 
		 * //RasPi raspi = new RasPi("raspi");
		 */

		// raspi.writeDisplay(busAddress, deviceAddress, data)

		int i = 0;

		Runtime.createAndStart(String.format("ras%d", i), "Runtime");
		Runtime.createAndStart(String.format("rasPi%d", i), "RasPi");
		Runtime.createAndStart(String.format("rasGUI%d", i), "GUIService");
		Runtime.createAndStart(String.format("rasPython%d", i), "Python");
		// Runtime.createAndStart(String.format("rasClock%d",i), "Clock");
		Runtime.createAndStart(String.format("rasRemote%d", i), "RemoteAdapter");
	}

}
