package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

public class Pir extends Service implements PinListener {

	public final static Logger log = LoggerFactory.getLogger(Pir.class);

	private static final long serialVersionUID = 1L;
	
	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Pir.class.getCanonicalName());
		meta.addDescription("PIR - Passive Infrared Sensor");
		meta.setAvailable(true); // false if you do not want it viewable in a
									// gui
		// add dependency if necessary
		// meta.addDependency("org.coolproject", "1.0.0");
		meta.addCategory("sensor");
		return meta;
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Runtime.start("pir", "Pir");
			Runtime.start("gui", "SwingGui");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}
	
	boolean isActive = false;
	boolean isEnabled = false;

	Integer pin;

	PinArrayControl pinControl;
	List<String> controllers;

	public Pir(String n) {
		super(n);
	}

	public void attach(PinArrayControl control, int pin) {
		this.pinControl = control;
		this.pin = pin;
		pinControl.attach(this, pin);
	}
	
	public void disable(){
		if (pinControl == null){
			error("pin control not set");
			return;
		}
		
		if (pin == null){
			error("pin not set");
			return;
		}
		
		pinControl.disablePin(pin);
		isEnabled = false;
		broadcastState();
	}
	
	public void enable(){
		if (pinControl == null){
			error("pin control not set");
			return;
		}
		
		if (pin == null){
			error("pin not set");
			return;
		}
		
		pinControl.enablePin(pin);
		isEnabled = true;
		broadcastState();
	}
	
	public List<String> refresh(){
		controllers = Runtime.getServiceNamesFromInterface(PinArrayControl.class);
		broadcastState();
		return controllers;
	}
	
	@Override
	public void onPin(PinData pindata) {
		// log.info("onPin {}", pindata);
		 boolean sense = (pindata.value != 0);
		 
		 if (isActive != sense){
			 // state change
			 invoke("publishSense", sense);
			 isActive = sense;
		 }
	}
	
	public Boolean publishSense(Boolean b){
		return b;
	}
	
	public void setPin(int pin){
		this.pin = pin;
		broadcastState();
	}
	

	public void setPinArrayControl(PinArrayControl pinControl){
		this.pinControl = pinControl;
		broadcastState();
	}

}
