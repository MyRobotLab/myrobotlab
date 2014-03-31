package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class PickToLightServer extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PickToLightServer.class.getCanonicalName());
	
	public PickToLightServer(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public boolean kitToLight(String xmlKit)
	{
		return true;
	}
	
	// ??? THROW FOR ERROR ???//
	public String turnLEDsOn(String arduinoName, String listOfLEDNumbers)
	{
		log.info("turnLedsOn request");
		Arduino arduino = (Arduino)Runtime.getInstance().getService(arduinoName);
		
		if (arduino == null)
		{
			error("can't get arduino %s", arduinoName);
		}
		
		if (!arduino.isConnected())
		{
			error("arduino %s not connected", arduinoName);
		}
		
		String[] leds = listOfLEDNumbers.split(" ");
		for (int i = 0; i < leds.length; ++i)
		{
			try {
				int address = Integer.parseInt(leds[i]);
				arduino.digitalWrite(address, 1);
			} catch(NumberFormatException e)
			{
				Logging.logException(e); // TODO error handles exception ?
				error(e.getMessage());
			}
		}
		
		return String.format("%s.digitalWrite(%s)", arduinoName, listOfLEDNumbers);
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		PickToLightServer pickToLight = (PickToLightServer)Runtime.createAndStart("pickToLight", "PickToLight");	
		Arduino arduino01 = (Arduino)Runtime.createAndStart("arduino01", "Arduino");	
		arduino01.connect("COM3");
				
		//log.info(pickToLight.turnLEDsOn("arduino01", "2 3 4 10"));
		
		Runtime.createAndStart("web", "WebGUI");
		
		//Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}


}
