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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.myrobotlab.service.Clock;
import org.myrobotlab.service.GUIService;

public class ClockGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	JButton startClock = new JButton("start clock");

	JPanel clockDisplayPanel = new JPanel(new BorderLayout());
	JPanel clockControlPanel = new JPanel();

	JLabel clockDisplay = new JLabel("<html><p style=\"font-size:30px;\">00:00:00</p></html>");
	// String displayFormat =
	// "<html><p style=\"font-size:30px\">%02d:%02d:%02d</p></html>";
	String displayFormat = "<html><p style=\"font-size:30px\">%s</p></html>";
	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	JLabel msgDisplay = new JLabel("");

	JTextField interval = new JTextField("1000");
	JTextField data = new JTextField(10);

	public ClockGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == startClock) {
			if (startClock.getText().compareTo("start clock") == 0) {

				myService.send(boundServiceName, "setInterval", Integer.parseInt(interval.getText()));
				// myService.send(boundServiceName, "setData", data.getText());
				myService.send(boundServiceName, "startClock");

			} else {
				myService.send(boundServiceName, "stopClock");
			}
		}
		myService.send(boundServiceName, "publishState");
	}

	public void addClockEvent(Date time, String name, String method, Object... data) {
		myService.send(boundServiceName, "addClockEvent", time, name, method, data);
	}

	@Override
	public void attachGUI() {
		subscribe("countdown", "countdown", Long.class);
		subscribe("publishState", "getState", Clock.class);
		subscribe("pulse", "pulse");

		myService.send(boundServiceName, "publishState");
	}

	public void countdown(Long amtRemaining, String pulseData) {

		long sec = amtRemaining / 1000 % 60;
		long min = amtRemaining / (60 * 1000) % 60;
		long hrs = amtRemaining / (60 * 60 * 1000) % 12;

		// clockControlPanel.setVisible(false);
		// color:#2BFF00;
		// msgDisplay.setText("<html><p style=\"font-size:10px;text-align:center;\">until core meltdown<br/>have a nice day !</p></html>");
		msgDisplay.setText("");
		clockDisplay.setOpaque(true);
		msgDisplay.setOpaque(true);
		clockDisplay.setBackground(new Color(0x2BFF00));
		msgDisplay.setBackground(new Color(0x2BFF00));
		msgDisplay.setText(pulseData);

		clockDisplay.setText(String.format(displayFormat, hrs, min, sec));
	}

	@Override
	public void detachGUI() {
		unsubscribe("countdown", "countdown", Long.class);
		unsubscribe("publishState", "getState", Clock.class);
		unsubscribe("pulse", "pulse");
	}

	public void getState(final Clock c) {
		/*
		 * setText is ThreadSafe - no need to invoke SwingUtilities
		 * 
		 * SwingUtilities.invokeLater(new Runnable() { public void run() {
		 */

		interval.setText((c.interval + ""));

		if (c.isClockRunning) {
			startClock.setText("stop clock");
			data.setEnabled(false);
			interval.setEnabled(false);
		} else {
			startClock.setText("start clock");
			data.setEnabled(true);
			interval.setEnabled(true);
		}

		/*
		 * }
		 * 
		 * });
		 */
	}

	@Override
	public void init() {
		display.setLayout(new BorderLayout());

		clockDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		clockDisplayPanel.add(clockDisplay, BorderLayout.CENTER);
		msgDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		clockDisplayPanel.add(msgDisplay, BorderLayout.SOUTH);

		display.add(clockDisplayPanel, BorderLayout.CENTER);
		display.add(clockControlPanel, BorderLayout.SOUTH);

		startClock.addActionListener(this);
		clockControlPanel.add(startClock);
		clockControlPanel.add(new JLabel("  interval  "));
		clockControlPanel.add(interval);
		clockControlPanel.add(new JLabel("  ms  "));

	}

	public void pulse(Date date) {
		clockDisplay.setText(String.format(displayFormat, dateFormat.format(date)));
		// countdown(System.currentTimeMillis(), date.);
	}

}
