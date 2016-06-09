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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.I2CMux;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class I2CMuxGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(I2CMuxGUI.class);

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controller = new JComboBox<String>();
	JComboBox<String> muxAddressList = new JComboBox<String>();
	JComboBox<String> muxBusList = new JComboBox<String>();

	JLabel muxAddressLabel = new JLabel("Address");
	JLabel muxBusLabel     = new JLabel("Bus");
	
	I2CMux myI2CMux = null;

	public I2CMuxGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myI2CMux = (I2CMux) Runtime.getService(boundServiceName);
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
							muxAddressList.getSelectedItem(),
							muxBusList.getSelectedItem());
				}
			} else {
				myService.send(boundServiceName, detach);
			}
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", I2CMux.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", I2CMux.class);
	}

	public void getState(I2CMux i2cMux) {

		refreshControllers();
		controller.setSelectedItem(myI2CMux.controllerName);
		muxAddressList.setSelectedItem(myI2CMux.muxAddress);
		muxBusList.setSelectedItem(myI2CMux.muxBus);
		if (i2cMux.isAttached()) {
			attachButton.setText(detach);
			controller.setEnabled(false);
			muxAddressList.setEnabled(false);
			muxBusList.setEnabled(false);
		} else {
			attachButton.setText(attach);
			controller.setEnabled(true);
			muxAddressList.setEnabled(true);
			muxBusList.setEnabled(true);
		}
	}

	@Override
	public void init() {

		// Container BACKGROUND = getContentPane();
		display.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.add(attachButton);
		north.add(controller);
		north.add(muxAddressLabel);
		north.add(muxAddressList);
		north.add(muxBusLabel);
		north.add(muxBusList);
		attachButton.addActionListener(this);

		getMuxAddressList();
		getMuxBusList();

		display.add(north, BorderLayout.NORTH);
	}

	public void getMuxAddressList() {

		ArrayList<String> mal = myI2CMux.muxAddressList;
		for (int i = 0; i < mal.size(); i++) {
			muxAddressList.addItem(mal.get(i));
		}
	}

	public void getMuxBusList() {
		ArrayList<String> mbl = myI2CMux.muxBusList;
		for (int i = 0; i < mbl.size(); i++) {
			muxBusList.addItem(mbl.get(i));
		}
	}

	public void refreshControllers() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				ArrayList<String> v = myI2CMux.controllers;
				controller.removeAllItems();
				if (v != null) {
					for (int i = 0; i < v.size(); ++i) {
						controller.addItem(v.get(i));
					}
					controller.setSelectedItem(myI2CMux.getControllerName());
				}
			}
		});
	}
}
