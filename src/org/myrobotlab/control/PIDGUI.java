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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.PID;
import org.slf4j.Logger;

public class PIDGUI extends ServiceGUI implements ActionListener {

	JTextField kp = new JTextField(10);
	JTextField ki = new JTextField(10);
	JTextField kd = new JTextField(10);
	JButton setPID = new JButton("set");

	JButton direction = new JButton("invert");
	JButton setPid = new JButton("set pid");

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(PIDGUI.class.getCanonicalName());

	public PIDGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == direction) {
			if (direction.getText().equals("invert")) {
				myService.send(boundServiceName, "setControllerDirection", new Integer(PID.DIRECTION_REVERSE));
				direction.setText("direct");
			} else {
				myService.send(boundServiceName, "setControllerDirection", new Integer(PID.DIRECTION_DIRECT));
				direction.setText("invert");
			}
		} else if (o == setPID) {
			Double Kp = Double.parseDouble(kp.getText());
			Double Ki = Double.parseDouble(ki.getText());
			Double Kd = Double.parseDouble(kd.getText());
			myService.send(boundServiceName, "setPID", Kp, Ki, Kd);
		}

	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", PID.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", PID.class);
	}

	public void getState(final PID pid) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int dir = pid.getControllerDirection();
				if (dir == PID.DIRECTION_REVERSE) {
					direction.setText("direct");
				} else {
					direction.setText("invert");
				}

				ki.setText(String.format("%s", pid.getKi()));
				kp.setText(String.format("%s", pid.getKp()));
				kd.setText(String.format("%s", pid.getKd()));

			}
		});
	}

	@Override
	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;

		direction.addActionListener(this);
		setPID.addActionListener(this);

		JPanel flow = new JPanel();

		flow.add(new JLabel("Kp"));
		flow.add(kp);
		flow.add(new JLabel("Ki"));
		flow.add(ki);
		flow.add(new JLabel("Kd"));
		flow.add(kd);
		flow.add(setPID);
		flow.add(direction);

		display.add(flow);
	}

}
