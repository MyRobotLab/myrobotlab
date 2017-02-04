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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AdafruitIna219;
import org.myrobotlab.service.I2cMux;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class I2cMuxGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(I2cMuxGUI.class);

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controller = new JComboBox<String>();
	JComboBox<String> deviceAddressList = new JComboBox<String>();
	JComboBox<String> deviceBusList = new JComboBox<String>();

	JLabel controllerLabel     = new JLabel("Controller");
	JLabel deviceBusLabel     = new JLabel("Bus");
	JLabel deviceAddressLabel = new JLabel("Address");
	
	I2cMux boundService = null;

	public I2cMuxGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		boundService = (I2cMux) Runtime.getService(boundServiceName);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == attachButton) {
			if (attachButton.getText().equals(attach)) {
				int index = controller.getSelectedIndex();
				if (index != -1) {
					myService.send(boundServiceName, attach, 
							controller.getSelectedItem(),
							deviceBusList.getSelectedItem(),
							deviceAddressList.getSelectedItem());
				}
			} else {
				myService.send(boundServiceName, detach);
			}
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", I2cMux.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", I2cMux.class);
	}

	public void getState(I2cMux i2cMux) {

		refreshControllers();
		controller.setSelectedItem(i2cMux.getControllerName());
		deviceBusList.setSelectedItem(i2cMux.deviceBus);
		deviceAddressList.setSelectedItem(i2cMux.deviceAddress);
		if (i2cMux.isAttached()) {
			attachButton.setText(detach);
			controller.setEnabled(false);
			deviceBusList.setEnabled(false);
			deviceAddressList.setEnabled(false);
		} else {
			attachButton.setText(attach);
			controller.setEnabled(true);
			deviceBusList.setEnabled(true);
			deviceAddressList.setEnabled(true);
		}
	}

	@Override
	public void init() {

		// Container BACKGROUND = getContentPane();
		display.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.add(attachButton);
		north.add(controller);
		north.add(deviceBusLabel);		
		north.add(deviceBusList);
		north.add(deviceAddressLabel);
		north.add(deviceAddressList);
		attachButton.addActionListener(this);

		refreshControllers();
		getDeviceBusList();
		getDeviceAddressList();

		display.add(north, BorderLayout.NORTH);
	}

	public void getDeviceBusList() {
		List<String> mbl = boundService.deviceBusList;
		for (int i = 0; i < mbl.size(); i++) {
			deviceBusList.addItem(mbl.get(i));
		}
	}
	
	public void getDeviceAddressList() {

		List<String> mal = boundService.deviceAddressList;
		for (int i = 0; i < mal.size(); i++) {
			deviceAddressList.addItem(mal.get(i));
		}
	}

	public void refreshControllers() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				List<String> v = boundService.controllers;
				controller.removeAllItems();
				if (v != null) {
					for (int i = 0; i < v.size(); ++i) {
						controller.addItem(v.get(i));
					}
					controller.setSelectedItem(boundService.getControllerName());
				}
			}
		});
	}
}
