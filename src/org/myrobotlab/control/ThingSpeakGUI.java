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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.ThingSpeak;
import org.slf4j.Logger;

public class ThingSpeakGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(ThingSpeakGUI.class.getCanonicalName());

	JTextField writeKey = new JTextField(15);
	JLabel intervalSeconds = new JLabel("");
	JLabel lastUpdate = new JLabel("");
	JButton save = new JButton("save");

	public ThingSpeakGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == save) {
			myService.send(boundServiceName, "saveConfig");
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", ThingSpeak.class);
		myService.send(boundServiceName, "broadcastState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", ThingSpeak.class);
	}

	public void getState(final ThingSpeak thing) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				writeKey.setText(thing.getWriteKey());
				intervalSeconds.setText(thing.getIntervalSeconds().toString());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss");
				Date resultdate = new Date(thing.getLastUpdate());
				lastUpdate.setText(sdf.format(resultdate));

			}
		});
	}

	@Override
	public void init() {
		JPanel input = new JPanel(new GridLayout(0, 2));
		input.add(new JLabel("write key"));
		input.add(writeKey);
		input.add(new JLabel("update interval"));
		input.add(intervalSeconds);
		input.add(new JLabel("last update"));
		input.add(lastUpdate);

		input.add(save);

		save.addActionListener(this);
		display.add(input);

	}

}
