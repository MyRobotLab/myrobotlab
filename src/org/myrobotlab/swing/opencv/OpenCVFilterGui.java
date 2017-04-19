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

package org.myrobotlab.swing.opencv;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public abstract class OpenCVFilterGui {
	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterGui.class);

	final String name;
	JPanel main = new JPanel(new BorderLayout());
	JPanel display = new JPanel(new GridBagLayout());
	final String boundServiceName;
	final SwingGui myGui;
	final public GridBagConstraints gc = new GridBagConstraints();

	FilterWrapper boundFilter = null;

	JComboBox<String> sources = new JComboBox<String>();
	ComboBoxModel2 sourcesModel = new ComboBoxModel2(this);

	public OpenCVFilterGui(String boundFilterName, String boundServiceName, SwingGui myGui) {
		name = boundFilterName;
		this.boundServiceName = boundServiceName;
		this.myGui = myGui;

		sources.addActionListener(sourcesModel);

		// title
		TitledBorder title;
		title = BorderFactory.createTitledBorder(name);
		display.setBorder(title);

		JPanel input = new JPanel();
		title = BorderFactory.createTitledBorder("input");
		input.setBorder(title);
		input.add(sources);

		main.add(input, BorderLayout.NORTH);
		main.add(display, BorderLayout.CENTER);

	}

	public JPanel getDisplay() {
		return main;
	}

	public abstract void getFilterState(final FilterWrapper filterWrapper);

	public void initFilterState(OpenCVFilter filter) {
		boundFilter = new FilterWrapper(name, filter);
		sources.setModel(sourcesModel);
		sources.setSelectedItem(filter.sourceKey);
	}

	public void setFilterState(OpenCVFilter filter) {
		myGui.send(boundServiceName, "setFilterState", new FilterWrapper(name, filter));
	}

	@Override
	public String toString() {
		return name;
	}

}
