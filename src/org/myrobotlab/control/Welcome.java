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

package org.myrobotlab.control;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.myrobotlab.control.widget.JIntegerField;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.CommunicationInterface;

public class Welcome extends ServiceGUI {

	static final long serialVersionUID = 1L;

	CommunicationInterface comm = null;
	JTextField loginValue = new JTextField("bob");
	JTextField loginPasswordValue = new JPasswordField("blahblah");
	JTextField hostnameValue = new JTextField("localhost", 15);
	JIntegerField servicePortValue = new JIntegerField();

	public Welcome(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

	@Override
	public void init() {

		GridBagConstraints gc = new GridBagConstraints();
		// gc.anchor = GridBagConstraints.WEST;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.ipadx = 5;

		servicePortValue.setInt(6767);

		gc.gridx = 0;
		JLabel image = new JLabel();
		image.setIcon(Util.getResourceIcon("mrl_logo.gif"));
		display.add(image);

		++gc.gridy;
		++gc.gridy;
		++gc.gridy;
		++gc.gridy;
		display.add(new JLabel("<html><h3><i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;I for one, welcome our new robot overlords ...</i></h3></html>"), gc);
	}

	public String setRemoteConnectionStatus(String state) {
		return state;
	}

}
