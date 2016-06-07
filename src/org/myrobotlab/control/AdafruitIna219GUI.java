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
import org.myrobotlab.service.AdafruitIna219;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AdafruitIna219GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(AdafruitIna219GUI.class);

	String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controller = new JComboBox<String>();

	JButton refresh = new JButton("refresh");

	JLabel busVoltage = new JLabel();
	JLabel shuntVoltage = new JLabel();
	JLabel current = new JLabel();
	JLabel power = new JLabel();

	AdafruitIna219 myAdafruitIna219 = null;

	public AdafruitIna219GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myAdafruitIna219 = (AdafruitIna219) Runtime.getService(boundServiceName);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("AdafruitINA219GUI actionPerformed");
		Object o = e.getSource();
		if (o == refresh) {
			myService.send(boundServiceName, "refresh");
		}
		if (o == attachButton) {
			if (attachButton.getText().equals(attach)) {
				int index = controller.getSelectedIndex();
				if (index != -1) {
					myService.send(boundServiceName, attach, controller.getSelectedItem());
				}
			} else {
				myService.send(boundServiceName, detach);
			}
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", AdafruitIna219.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", AdafruitIna219.class);
	}

	public void getState(AdafruitIna219 ina219) {

		refreshControllers();
		controller.setSelectedItem(ina219.getController());
		if (ina219.isAttached()) {
			attachButton.setText(detach);
			controller.setEnabled(false);
			refresh.setEnabled(true);
		} else {
			attachButton.setText(attach);
			controller.setEnabled(true);
			refresh.setEnabled(false);
		}
		busVoltage.setText(String.format("%s", ina219.busVoltage));
		shuntVoltage.setText(String.format("%s", ina219.shuntVoltage));
		current.setText(String.format("%s", ina219.current));
		power.setText(String.format("%s", ina219.power));
	}

	@Override
	public void init() {

		// Container BACKGROUND = getContentPane();

		display.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.add(controller);
		north.add(attachButton);
		north.add(refresh);
		attachButton.addActionListener(this);	
		refresh.addActionListener(this);

		JPanel center = new JPanel();
		center.add(new JLabel("Bus Voltage   :"));
		center.add(busVoltage);
		center.add(new JLabel(" mV"));

		center.add(new JLabel("Shunt Voltage :"));
		center.add(shuntVoltage);
		center.add(new JLabel(" mV"));

		center.add(new JLabel("Shunt Current :"));
		center.add(current);
		center.add(new JLabel(" mA"));

		center.add(new JLabel("Power         :"));
		center.add(power);
		center.add(new JLabel(" mW"));

		display.add(north, BorderLayout.NORTH);
		display.add(center, BorderLayout.CENTER);
	}
	
	public void refreshControllers() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				ArrayList<String> v = myAdafruitIna219.refreshControllers();
				controller.removeAllItems();
				for (int i = 0; i < v.size(); ++i) {
					controller.addItem(v.get(i));
				}
				controller.setSelectedItem(myAdafruitIna219.getControllerName());
			}
		});
	}
}
