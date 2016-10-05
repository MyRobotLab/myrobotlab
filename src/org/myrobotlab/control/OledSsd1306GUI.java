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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OledSsd1306;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class OledSsd1306GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(OledSsd1306GUI.class.getCanonicalName());

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controllerList = new JComboBox<String>();
	JComboBox<String> deviceAddressList = new JComboBox<String>();
	JComboBox<String> deviceBusList = new JComboBox<String>();

	JLabel controllerLabel = new JLabel("Controller");
	JLabel deviceBusLabel = new JLabel("Bus");
	JLabel deviceAddressLabel = new JLabel("Address");

	OledSsd1306 boundService = null;

	public OledSsd1306GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		boundService = (OledSsd1306) Runtime.getService(boundServiceName);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();
				if (o == attachButton) {
					if (attachButton.getText().equals(attach)) {
						int index = controllerList.getSelectedIndex();
						if (index != -1) {
							myService.send(boundServiceName, attach, controllerList.getSelectedItem(), deviceBusList.getSelectedItem(), deviceAddressList.getSelectedItem());
						}
					} else {
						myService.send(boundServiceName, detach);
					}
				}
				return;
			}
		});
	}

	@Override
	public void attachGUI() {
		log.info("AttachGUI subscribing to Adafruit16CServoDriver.class");
		subscribe("publishState", "getState", OledSsd1306.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", OledSsd1306.class);
	}

	public void getState(final OledSsd1306 driver) {
		log.info("getState invoked");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				removeListeners();
				refreshControllers();
				if (driver.controller != null) {
					controllerList.setSelectedItem(driver.controller.getName());
					deviceBusList.setSelectedItem(boundService.deviceBus);
					deviceAddressList.setSelectedItem(boundService.deviceAddress);
				}
				if (driver.isControllerSet) {
					attachButton.setText(detach);
					controllerList.setEnabled(false);
					deviceBusList.setEnabled(false);
					deviceAddressList.setEnabled(false);
				} else {
					attachButton.setText(attach);
					controllerList.setEnabled(true);
					deviceBusList.setEnabled(true);
					deviceAddressList.setEnabled(true);
				}
				restoreListeners();
			}
		});
	}

	@Override
	public void init() {

		// build input begin ------------------
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		panel.add(attachButton, gc);
		++gc.gridx;

		panel.add(controllerList, gc);
		panel.add(deviceBusLabel);
		panel.add(deviceBusList);
		panel.add(deviceAddressLabel);
		panel.add(deviceAddressList);

		display.add(panel);

		refreshControllers();
		getDeviceBusList();
		getDeviceAddressList();

		restoreListeners();
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
				controllerList.removeAllItems();
				for (int i = 0; i < v.size(); ++i) {
					controllerList.addItem(v.get(i));
				}
				if (boundService.controller != null) {
					controllerList.setSelectedItem(boundService.controller.getName());
				}
			}
		});
	}

	public void removeListeners() {
		attachButton.removeActionListener(this);
	}

	public void restoreListeners() {
		attachButton.addActionListener(this);
	}
}
