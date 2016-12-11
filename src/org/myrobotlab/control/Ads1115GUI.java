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
import org.myrobotlab.service.Ads1115;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

public class Ads1115GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Ads1115GUI.class);

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controllerName = new JComboBox<String>();
	JComboBox<String> deviceAddressList = new JComboBox<String>();
	JComboBox<String> deviceBusList = new JComboBox<String>();

	JLabel controllerLabel     = new JLabel("Controller");
	JLabel deviceBusLabel     = new JLabel("Bus");
	JLabel deviceAddressLabel = new JLabel("Address");
	
	JButton refresh = new JButton("refresh");

	JLabel adc0 = new JLabel();
	JLabel adc1 = new JLabel();
	JLabel adc2 = new JLabel();
	JLabel adc3 = new JLabel();

	Ads1115 boundService = null;

	public Ads1115GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		boundService = (Ads1115) Runtime.getService(boundServiceName);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("Ads1115GUI actionPerformed");
		Object o = e.getSource();
		if (o == refresh) {
			myService.send(boundServiceName, "refresh");
		}
		if (o == attachButton) {
			if (attachButton.getText().equals(attach)) {
				int index = controllerName.getSelectedIndex();
				if (index != -1) {
					myService.send(boundServiceName, attach, 
							controllerName.getSelectedItem(),
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
		subscribe("publishState", "getState", Ads1115.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Ads1115.class);
	}

	public void getState(Ads1115 ads1115) {

		refreshControllers();
		I2CController controller = ads1115.getController();
		controllerName.setSelectedItem(ads1115.getControllerName());
		deviceBusList.setSelectedItem(ads1115.deviceBus);
		deviceAddressList.setSelectedItem(ads1115.deviceAddress);
		if (controller != null) {
			attachButton.setText(detach);
			controllerName.setEnabled(false);
			deviceBusList.setEnabled(false);
			deviceAddressList.setEnabled(false);
			refresh.setEnabled(true);
		} else {
			attachButton.setText(attach);
			controllerName.setEnabled(true);
			deviceBusList.setEnabled(true);
			deviceAddressList.setEnabled(true);
			refresh.setEnabled(false);
		}
		adc0.setText(String.format("%s", ads1115.adc0));
		adc1.setText(String.format("%s", ads1115.adc1));
		adc2.setText(String.format("%s", ads1115.adc2));
		adc3.setText(String.format("%s", ads1115.adc3));
	}

	@Override
	public void init() {

		// Container BACKGROUND = getContentPane();

		display.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.add(controllerLabel);
		north.add(controllerName);
		north.add(deviceBusLabel);		
		north.add(deviceBusList);
		north.add(deviceAddressLabel);
		north.add(deviceAddressList);
		north.add(attachButton);
		north.add(refresh);
		attachButton.addActionListener(this);	
		refresh.addActionListener(this);

		JPanel center = new JPanel();
		center.add(new JLabel("Adc0: "));
		center.add(adc0);

		center.add(new JLabel("Adc1: "));
		center.add(adc1);

		center.add(new JLabel("Adc2: "));
		center.add(adc2);

		center.add(new JLabel("Adc3: "));
		center.add(adc3);

		display.add(north, BorderLayout.NORTH);
		display.add(center, BorderLayout.CENTER);
		
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
				controllerName.removeAllItems();
				List<String> c = boundService.controllers;	
				for (int i = 0; i < c.size(); ++i) {
					controllerName.addItem(c.get(i));
				}
				controllerName.setSelectedItem(boundService.getControllerName());
			}
		});
	}
}
