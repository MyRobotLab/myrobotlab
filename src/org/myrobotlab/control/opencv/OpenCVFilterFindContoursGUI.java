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

package org.myrobotlab.control.opencv;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterFindContoursGUI extends OpenCVFilterGUI {

	public OpenCVFilterFindContoursGUI(String boundFilterName, String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

	}

	public void apply() {
		log.debug("apply");

	}

	// @Override
	public void attachGUI() {
		log.debug("attachGUI");

	}

	// @Override
	public void detachGUI() {
		log.debug("detachGUI");

	}

	@Override
	public void getFilterState(FilterWrapper filter) {
		// TODO Auto-generated method stub

	}

}
