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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterAddMask;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterAddMaskGUI extends OpenCVFilterGUI implements ActionListener {
	
	public OpenCVFilterAddMaskGUI(String boundFilterName, String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);	
		
		ComboBoxModel list = new ComboBoxModel(this);

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
	public void getFilterState(final FilterWrapper filterWrapper) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				OpenCVFilterAddMask bf = (OpenCVFilterAddMask)filterWrapper.filter;
				sources.setSelectedItem(bf.sourceName);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		OpenCVFilterAddMask bf = (OpenCVFilterAddMask) boundFilter.filter;
		setFilterState(bf);
	}

}
