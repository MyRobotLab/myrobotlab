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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Pcf8574;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class Pcf8574GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Pcf8574GUI.class);

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controller = new JComboBox<String>();
	JComboBox<String> deviceAddressList = new JComboBox<String>();
	JComboBox<String> deviceBusList = new JComboBox<String>();

	JLabel controllerLabel     = new JLabel("Controller");
	JLabel deviceBusLabel     = new JLabel("Bus");
	JLabel deviceAddressLabel = new JLabel("Address");

	Pcf8574 boundService = null;

	public Pcf8574GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		boundService = (Pcf8574) Runtime.getService(boundServiceName);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("pcf8574GUI actionPerformed");
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
		subscribe("publishState", "getState", Pcf8574.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Pcf8574.class);
	}

	public void getState(Pcf8574 ads1115) {

		refreshControllers();
		controller.setSelectedItem(ads1115.getControllerName());
		deviceBusList.setSelectedItem(ads1115.deviceBus);
		deviceAddressList.setSelectedItem(ads1115.deviceAddress);
		if (ads1115.isAttached()) {
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
		north.add(controllerLabel);
		north.add(controller);
		north.add(deviceBusLabel);		
		north.add(deviceBusList);
		north.add(deviceAddressLabel);
		north.add(deviceAddressList);
		north.add(attachButton);
		attachButton.addActionListener(this);	

		display.add(north, BorderLayout.NORTH);
		
		getDeviceBusList();
		getDeviceAddressList();
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

				boundService.refreshControllers();
				controller.removeAllItems();
				List<String> c = boundService.controllers;	
				for (int i = 0; i < c.size(); ++i) {
					controller.addItem(c.get(i));
				}
				controller.setSelectedItem(boundService.getControllerName());
			}
		});
	}
}
