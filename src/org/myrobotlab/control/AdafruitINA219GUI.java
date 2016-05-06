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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AdafruitINA219;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class AdafruitINA219GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(AdafruitINA219GUI.class.getCanonicalName());

	JButton refresh = new JButton("refresh");
	
	JLabel busVoltage = new JLabel();
	JLabel shuntVoltage = new JLabel();
	JLabel current = new JLabel();
	JLabel power = new JLabel();
	
	public AdafruitINA219GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    log.info("AdafruitINA219GUI actionPerformed");
		Object o = e.getSource();
		if (o == refresh) {
			myService.send(boundServiceName, "refresh");
		}
	}

	@Override
	public void attachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's
		subscribe("publishState", "getState", AdafruitINA219.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's

		unsubscribe("publishState", "getState", AdafruitINA219.class);
	}

	public void getState(AdafruitINA219 ina219) {
		busVoltage.setText(String.format("%s",ina219.busVoltage));
		shuntVoltage.setText(String.format("%s",ina219.shuntVoltage));
		current.setText(String.format("%s",ina219.current));
		power.setText(String.format("%s",ina219.power));
	}
	
	@Override
	public void init() {
		
		// Container BACKGROUND = getContentPane();
		
		display.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.add(refresh);
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

}
