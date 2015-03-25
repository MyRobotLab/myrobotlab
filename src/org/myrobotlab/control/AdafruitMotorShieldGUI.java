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
 * References :
 * 		http://learn.adafruit.com/adafruit-motor-shield/af-dcmotor-class
 * 		http://forums.adafruit.com/viewtopic.php?f=31&t=26873 - Servos & cut traces
 * 
 * */

package org.myrobotlab.control;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AdafruitMotorShield;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AdafruitMotorShieldGUI extends ServiceGUI implements ListSelectionListener {

	class ButtonListener implements ActionListener {
		ButtonListener() {
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			log.info(e.getActionCommand());
			myService.send(boundServiceName, e.getActionCommand());
		}
	}

	public final static Logger log = LoggerFactory.getLogger(AdafruitMotorShieldGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;
	private AdafruitMotorShield myAdafruitMotorShield = null;

	JLayeredPane imageMap;

	public AdafruitMotorShieldGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myAdafruitMotorShield = (AdafruitMotorShield) Runtime.getService(boundServiceName);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Arduino.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Arduino.class);
	}

	public void getAFPanel() {
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(new Dimension(400, 266));
		imageMap.setVisible(true);
		// pinComponentList = new ArrayList<PinComponent>();

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util.getImageIcon("AdafruitMotorShield/DC_Motor_Ports.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));
	}

	public void getState(AdafruitMotorShield shield) {
		if (shield != null) {
			// setPorts(roomba.getDeviceNames());
		}

	}

	@Override
	public void init() {

		getAFPanel();
		display.add(imageMap);
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

}