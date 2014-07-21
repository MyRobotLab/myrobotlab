package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CBus;

public class Sprinkler extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sprinkler.class);

	GpioController gpio;
	PCF8574GpioProvider gpioProvider;
	HashMap<String, GpioPinDigitalOutput> pcf8574s = new HashMap<String, GpioPinDigitalOutput>();
	WebGUI wegui;
	Python python;
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("python", "Python", "Python service");
		return peers;
	}


	public Sprinkler(String n) {
		super(n);
	}
	
	public void setState(String key, int value){
		if (value == 0){
			setState(key, false);
		} else {
			setState(key, true);
		}
	}
	
	public void setState(String key, boolean value){
		if (pcf8574s.containsKey(key)){
			gpio.setState(value, pcf8574s.get(key));
		} else {
			error("setState - could not find %s", key);
		}
		
	}

	public void startService() {
		try {
			gpio = GpioFactory.getInstance();
			// gpioProvider = new PCF8574GpioProvider(I2CBus.BUS_1, PCF8574GpioProvider.PCF8574A_0x3F);
			gpioProvider = new PCF8574GpioProvider(I2CBus.BUS_1, 32);

			// provision gpio output pins and make sure they are all LOW at
			// startup			
			
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_00, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_01, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_02, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_03, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_04, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_05, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_06, PinState.HIGH));
			pcf8574s.put(String.format("%d", 0), gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_07, PinState.HIGH));

			// on program shutdown, set the pins back to their default state:
			// HIGH
			// gpio.setShutdownOptions(true, PinState.HIGH, myOutputs);
			
			wegui = (WebGUI)startPeer("webgui");
			python = (Python)startPeer("python");

		} catch (Exception e) {
			Logging.logException(e);
		}

		/*
		 * NO INPUTS FOR SPRINKLER AT THE MOMENT
		 * 
		 * // provision gpio input pins from MCP23017 GpioPinDigitalInput
		 * myInputs[] = { gpio.provisionDigitalInputPin(gpioProvider,
		 * PCF8574Pin.GPIO_00), gpio.provisionDigitalInputPin(gpioProvider,
		 * PCF8574Pin.GPIO_01), gpio.provisionDigitalInputPin(gpioProvider,
		 * PCF8574Pin.GPIO_02) };
		 * 
		 * // create and register gpio pin listener gpio.addListener(new
		 * GpioPinListenerDigital() {
		 * 
		 * @Override public void
		 * handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent
		 * event) { // display pin state on console
		 * System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() +
		 * " = " + event.getState()); } }, myInputs);
		 */
	}

	public void stopService() {
		gpio.shutdown();
		// shuts down pi4j polling threads
	}
	

	@Override
	public String getDescription() {
		return "uber sprinkler system";
	}

	public static void main(String args[]) throws InterruptedException, IOException {

		
	}

}
