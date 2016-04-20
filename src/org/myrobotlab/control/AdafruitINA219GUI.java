/**
 *                    
 * @author Mats (at) myrobotlab.org
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

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.AdafruitINA219;
import org.slf4j.Logger;

public class AdafruitINA219GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(AdafruitINA219GUI.class.getCanonicalName());

	JLabel voltageLabel = null;
	JLabel currentLabel = null;
	JLabel powerLabel = null;
	
	JLabel voltageValue = null;
	JLabel currentValue= null;
	JLabel powerValue = null;
	
	public AdafruitINA219GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

	}

	@Override
	public void attachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's

		// subscribe("publishState", "getState", AdafruitINA219.class);
		// send("publishState");
	}

	@Override
	public void detachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's

		// unsubscribe("publishState", "getState", AdafruitINA219.class);
	}

	public void getState(AdafruitINA219 template) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	public void init() {
		display.setLayout(new BorderLayout());
	}

}
