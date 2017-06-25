/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.interfaces;

public interface I2CBusControl {

	/* Mats:
	 * Not sure what methods to put here yet
	 
	public boolean setController(String controllerName, String deviceBus, String deviceAddress);
	public boolean setController(String controllerName);
	public boolean setController(I2CController controller);
	public boolean setController(I2CController controller, String deviceBus, String deviceAddress);
	public void unsetController();
	*/
	public void setDeviceBus(String deviceBus);
	public void setDeviceAddress(String deviceAddress);
	
}
