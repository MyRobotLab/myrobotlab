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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.InMoov;
import org.slf4j.Logger;

public class InMoovGUI extends ServiceGUI implements ActionListener {

	HashSet<String> handTemplate = new HashSet<String>(Arrays.asList("%s.%s", "%s.%sHand", "%s.%sHand.index", "%s.%sHand.majeure", "%s.%sHand.ringFinger", "%s.%sHand.pinky",
			"%s.%sHand.thumb", "%s.%sHand.wrist"));
	HashSet<String> armTemplate = new HashSet<String>(Arrays.asList("%s.%s", "%s.%sArm", "%s.%sArm.bicep", "%s.%sArm.rotate", "%s.%sArm.shoulder", "%s.%sArm.omoplate"));
	HashSet<String> headTemplate = new HashSet<String>(Arrays.asList("%s.head", "%s.head.eyesTracking.xpid", "%s.head.eyesTracking.ypid", "%s.head.jaw", "%s.head.mouthControl",
			"%s.head.eyesTracking", "%s.head.eyeX", "%s.head.eyeY", "%s.head.rothead", "%s.head.neck", "%s.head.headTracking.xpid", "%s.head.headTracking.ypid",
			"%s.head.headTracking", "%s.mouth", "speechAudioFile"));

	HashMap<String, HashSet<String>> templates = new HashMap<String, HashSet<String>>();

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoovGUI.class.getCanonicalName());

	JLayeredPane imageMap;

	JButton leftHand = new JButton("start left hand");
	JButton leftArm = new JButton("start left arm");

	JButton rightHand = new JButton("start right hand");
	JButton rightArm = new JButton("start right arm");

	JButton head = new JButton("start head");

	JButton attachDetach = new JButton("attach");

	String defaultLeftPort;
	String defaultRightPort;

	VideoWidget opencv;

	public InMoovGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		templates.put("hand", handTemplate);
		templates.put("arm", armTemplate);
		templates.put("head", headTemplate);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == leftHand) {
			processAction(leftHand, "left", "hand");
		} else if (o == rightHand) {
			processAction(rightHand, "right", "hand");
		} else if (o == leftArm) {
			processAction(leftArm, "left", "arm");
		} else if (o == rightArm) {
			processAction(rightArm, "right", "arm");
		} else if (o == head) {
			processAction(head, "left", "head");
		} else {
			log.error("unkown event");
		}

	}

	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <-
	// Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", InMoovGUI.class);
		myService.send(boundServiceName, "publishState");
		opencv.attachGUI(); // default attachment
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", InMoovGUI.class);
		opencv.detachGUI(); // default attachment
	}

	public String getPort(String side) {
		if ("left".equals(side) && defaultLeftPort == null) {
			defaultLeftPort = JOptionPane.showInputDialog(getDisplay(), "left COM port");
			return defaultLeftPort;
		}

		if ("right".equals(side) && defaultRightPort == null) {
			defaultRightPort = JOptionPane.showInputDialog(getDisplay(), "right COM port");
			return defaultRightPort;
		}

		if ("left".equals(side)) {
			return defaultLeftPort;
		}

		if ("right".equals(side)) {
			return defaultRightPort;
		}

		return null;
	}

	public void getState(final InMoov moov) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	public void init() {
		display.setLayout(new BorderLayout());

		opencv = new VideoWidget(String.format("%s.opencv", boundServiceName), myService, tabs);
		opencv.init();
		// opencv = new VideoWidget(boundServiceName, myService, tabs);
		// opencv.

		/*
		 * imageMap = new JLayeredPane(); imageMap.setPreferredSize(new
		 * Dimension(692, 688));
		 * 
		 * 
		 * JLabel image = new JLabel(); ImageIcon dPic =
		 * Util.getImageIcon("InMoov/body.png"); image.setIcon(dPic); Dimension
		 * s = image.getPreferredSize(); image.setBounds(0, 0, s.width,
		 * s.height); imageMap.add(image, new Integer(1));
		 */

		JPanel controls = new JPanel(new GridLayout(6, 1));

		controls.add(leftHand);
		controls.add(leftArm);
		controls.add(head);
		controls.add(rightHand);
		controls.add(rightArm);

		display.add(controls, BorderLayout.EAST);
		display.add(opencv.display, BorderLayout.CENTER);

		leftHand.addActionListener(this);
		leftArm.addActionListener(this);
		rightHand.addActionListener(this);
		rightArm.addActionListener(this);
		head.addActionListener(this);
	}

	public void processAction(JButton button, String side, String part) {
		log.info(String.format("processAction [%s], %s %s", button.getText(), side, part));
		if (String.format("start %s %s", side, part).equals(button.getText())) {
			String port = getPort(side);
			log.info(String.format("starting %s %s with port %s", side, part, port));
			String upperPart = Character.toUpperCase(part.charAt(0)) + part.substring(1);
			String upperSide = Character.toUpperCase(side.charAt(0)) + side.substring(1);
			send(String.format("start%s%s", upperSide, upperPart), port);
			button.setText(String.format("hide %s %s", side, part));
			return;
		} else if (String.format("hide %s %s", side, part).equals(button.getText())) {

			HashSet<String> template = templates.get(part);
			for (String s : template) {
				myService.hidePanel(String.format(s, boundServiceName, side));
			}
			button.setText(String.format("show %s %s", side, part));

		} else if (String.format("show %s %s", side, part).equals(button.getText())) {
			HashSet<String> template = templates.get(part);
			for (String s : template) {
				myService.unhidePanel(String.format(s, boundServiceName, side));
			}
			button.setText(String.format("hide %s %s", side, part));
		} else {
			log.error("can't process [{}]", button.getText());
		}
	}

}
