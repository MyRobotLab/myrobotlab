package org.myrobotlab.service.interfaces;

import java.util.ArrayList;

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.service.data.Pin;

public interface MicrocontrollerPeripheral {

	/**
	 * This is basic information to request from a Controller. A list of pins on
	 * the controller so GUIs or other services can figure out if there are any
	 * appropriate
	 * 
	 * @return
	 */
	public ArrayList<Pin> getPinList();

	/**
	 * one Attach to rule them all !
	 * this attach implemented in the controller will route by
	 * actual type to the appropriate motorAttach servoAttach sensorAttach...
	 * 
	 * @param name
	 * @throws MRLException
	 */
	public void attach(String name) throws MRLException;

	/**
	 * one Detach to rule them all 
	 * @param name
	 * @return
	 */
	public boolean detach(String name);
	
	public boolean connect(String port);
}
