/**
 *
 * @author kmcgerald (at) myrobotlab.org
 *
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version (subject to the "Classpath" exception as provided in the LICENSE.txt
 * file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for details.
 *
 * Enjoy !
 *
 *
 */
package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service._TemplateService;
import org.slf4j.Logger;

public class MQTTGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MQTTGUI.class.getCanonicalName());
	private JTextField timeTextField = new JTextField(30);
	private JTextField topicTextField = new JTextField(30);
	// final JTextArea messageConsole;
	// final JScrollPane messageScrollPane;
	private JTextField messageTextField = new JTextField(100);

	public MQTTGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		// messageConsole = new JTextArea();
		// messageScrollPane = new JScrollPane(messageConsole);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void attachGUI() {
		subscribe("publishMQTTMessage", "displayData", String[].class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishMQTTMessage", "displayData", String[].class);
	}

	public void displayData(String[] tokens) {
		timeTextField.setText(tokens[0]);
		topicTextField.setText(tokens[1]);
		// messageConsole.append(tokens[2]);
		messageTextField.setText(tokens[2]);
	}

	public void getState(_TemplateService template) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			}
		});
	}

	@Override
	public void init() {

		gc.gridheight = 8;
		gc.gridx = 0;
		gc.gridy = 0;

		display.add(new JLabel("Time:"), gc);
		++gc.gridx;
		display.add(timeTextField, gc);
		gc.gridx = 0;
		gc.gridy += 42;

		display.add(new JLabel("Topic:"), gc);
		++gc.gridx;
		display.add(topicTextField, gc);
		gc.gridx = 0;
		gc.gridy += 42;

		display.add(new JLabel("Messages:"), gc);
		++gc.gridx;
		// display.add(messageConsole, gc);
		display.add(messageTextField, gc);
		gc.gridx = 0;
		gc.gridy += 42;
	}
}
