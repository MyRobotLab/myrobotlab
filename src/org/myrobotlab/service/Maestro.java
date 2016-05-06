package org.myrobotlab.service;

import java.util.ArrayList;

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author GroG http://www.pololu.com/product/1352
 *         http://www.pololu.com/product/1350
 *
 */
public class Maestro extends Service implements ArduinoShield, ServoController {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Maestro.class);


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		try {

			Maestro template = new Maestro("template");
			template.startService();

			Runtime.createAndStart("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Maestro(String n) {
		super(n);
	}

	@Override
	public boolean attach(Arduino arduino) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAttached() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void attach(String name) throws MRLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detach(String name) {
		// TODO Auto-generated method stub
	}

	@Override
	public void connect(String port) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean servoAttach(Servo servo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean servoDetach(Servo servo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void servoSweepStart(Servo servo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void servoSweepStop(Servo servo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void servoWrite(Servo servo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void servoWriteMicroseconds(Servo servo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean servoEventsEnabled(Servo servo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setServoSpeed(Servo servo) {
		// TODO Auto-generated method stub
		
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

		ServiceType meta = new ServiceType(Maestro.class.getCanonicalName());
		meta.addDescription("Maestro USB Servo Controllers ");
		meta.addCategory("microcontroller");
		meta.addPeer("serial", "Serial", "Serial service is needed for Pololu");
		
		return meta;
	}

}
