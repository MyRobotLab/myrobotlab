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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.control.widget.SliderWithText;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterAffine;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterAffineGUI extends OpenCVFilterGUI implements ChangeListener {

	SliderWithText angle = new SliderWithText(JSlider.HORIZONTAL, 0, 360, 0);
	
	public OpenCVFilterAffineGUI(String boundFilterName, String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

		angle.addChangeListener(this);

		GridBagConstraints gc2 = new GridBagConstraints();

		TitledBorder title;
		JPanel j = new JPanel(new GridBagLayout());
		title = BorderFactory.createTitledBorder("Affine Config");
		j.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;
		j.add(new JLabel("Angle"), gc);
		++gc.gridx;
		j.add(angle, gc);
		++gc.gridx;
		j.add(angle.value, gc);
		++gc.gridy;
		gc.gridx = 0;
		display.add(j, gc);

	}

	// FIXME - update components :)
	@Override
	public void getFilterState(final FilterWrapper filterWrapper) {
		boundFilter = filterWrapper;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				OpenCVFilterAffine af = (OpenCVFilterAffine)filterWrapper.filter;
			}
		});

	}


	@Override
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		OpenCVFilterAffine af = (OpenCVFilterAffine) boundFilter.filter;
		if (o == angle) {
			af.setAngle(angle.getValue());
			angle.setText(angle.getValue());
		} else {
			log.info("Unknown object in state change {}", o);
		}
		setFilterState(af);
		
	}

}
