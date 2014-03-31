package org.myrobotlab.service;

import java.util.ArrayList;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;


public class Maestro extends Service implements ArduinoShield, ServoController {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Maestro.class);
	
	
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		peers.put("serial", "Serial", "Serial service is needed for Pololu");					
		return peers;
	}
	
	public Maestro(String n) {
		super(n);	
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Maestro template = new Maestro("template");
		template.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean servoAttach(String servoName, Integer pin) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void servoWrite(String servoName, Integer newPos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean servoDetach(String servoName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Integer getServoPin(String servoName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setServoSpeed(String servoName, Float speed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void servoStop(String servoName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean attach(Arduino arduino) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAttached() {
		// TODO Auto-generated method stub
		return false;
	}


}
