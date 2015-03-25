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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.myrobotlab.control.widget.JIntegerField;
import org.myrobotlab.service.FaceTracking;
import org.myrobotlab.service.GUIService;

public class FaceTrackingGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	JButton startFaceTracking = null;
	ButtonGroup group = new ButtonGroup();

	JRadioButton none = new JRadioButton("none");
	JRadioButton increment = new JRadioButton("increment");
	JRadioButton integer = new JRadioButton("integer");
	JRadioButton string = new JRadioButton("string");

	JTextField interval = new JTextField("1000");
	JTextField pulseDataString = new JTextField(10);
	JIntegerField pulseDataInteger = new JIntegerField(10);

	ActionListener setType = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			myService.send(boundServiceName, "setType", ((JRadioButton) e.getSource()).getText());
		}
	};

	public FaceTrackingGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	// TODO - Object? can this be buried and managed reflectively?
	// FaceTracking myBoundService = null;

	@Override
	public void actionPerformed(ActionEvent e) {
		myService.send(boundServiceName, "setType", e.getActionCommand());
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", FaceTracking.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", FaceTracking.class);
	}

	public JButton getstartFaceTrackingButton() {
		if (startFaceTracking == null) {
			startFaceTracking = new JButton("start FaceTracking");
			startFaceTracking.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (startFaceTracking.getText().compareTo("start FaceTracking") == 0) {
						startFaceTracking.setText("stop FaceTracking");

						// TODO - design considerations
						// myService.send(boundServiceName, "setState",
						// myBoundService); // double set on local
						// if (myBoundService != null)
						{
							// TODO - proxy data class like GWT?? - directly
							// setting field wont work remotely
							// myBoundService.pulseDataString =
							// pulseDataString.getText();
						}
						// TODO - setting fields on a proxy class - vs accessors
						// like these
						// what about a helper funcion - hides boundServiceName
						// - and the setting of fields on
						// the Service side (buried in Service)
						// setMethods can be bogus - and set removed then field
						// actually set
						// getMethods could block ?

						// myService.send(boundServiceName, "setType", );

						// myService.send(boundServiceName, "set",
						// Integer.parseInt(interval.getText()));
						myService.send(boundServiceName, "setInterval", Integer.parseInt(interval.getText()));
						myService.send(boundServiceName, "startFaceTracking");
						myService.send(boundServiceName, "setPulseDataString", pulseDataString.getText());
						myService.send(boundServiceName, "setPulseDataInteger", Integer.parseInt(pulseDataInteger.getText()));

					} else {
						startFaceTracking.setText("start FaceTracking");
						myService.send(boundServiceName, "stopFaceTracking");
					}
				}

			});

		}

		return startFaceTracking;

	}

	/*
	 * without a proxy class - this is kindof messy you could be
	 * creating/setting a local FaceTracking when you want a Remote public void
	 * setState() { // if (myBoundService != null) {
	 * myService.send(boundServiceName, "setState", myBoundService); }
	 * 
	 * }
	 */

	// TODO - instead of actual FaceTracking - send data Proxy FaceTrackingData
	// to set/get state !
	public void getState(FaceTracking c) {

		if (c != null) {
		}

	}

	@Override
	public void init() {

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;

		display.add(getstartFaceTrackingButton(), gc);
		gc.gridwidth = 1;
		++gc.gridx;
		display.add(new JLabel("  interval  "), gc);
		++gc.gridx;
		display.add(interval, gc);
		++gc.gridx;
		display.add(new JLabel("  ms  "), gc);

		// build filters begin ------------------
		JPanel pulseData = new JPanel(new GridBagLayout());
		TitledBorder title;
		title = BorderFactory.createTitledBorder("pulse data");
		pulseData.setBorder(title);

		none.setActionCommand("none");
		none.setSelected(true);
		none.addActionListener(setType);

		increment.setActionCommand("increment");
		increment.addActionListener(setType);

		integer.setActionCommand("integer");
		integer.addActionListener(setType);

		string.setActionCommand("string");
		string.addActionListener(setType);

		// Group the radio buttons.
		group.add(none);
		group.add(increment);
		group.add(integer);
		group.add(string);

		// reuse gc
		gc.gridx = 0;
		gc.gridy = 0;
		pulseData.add(none, gc);
		++gc.gridy;
		pulseData.add(increment, gc);
		++gc.gridy;
		pulseData.add(integer, gc);
		++gc.gridx;
		pulseData.add(pulseDataInteger, gc);
		gc.gridx = 0;
		++gc.gridy;
		pulseData.add(string, gc);
		++gc.gridx;
		pulseData.add(pulseDataString, gc);

		// reuse gc
		gc.gridx = 0;
		gc.gridy = 2;
		display.add(pulseData, gc);

	}

}
