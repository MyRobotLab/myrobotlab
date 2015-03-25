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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class WiiBotGUI extends ServiceGUI implements ListSelectionListener {

	public class Keyboard implements KeyListener {

		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");

		@Override
		public void keyPressed(KeyEvent keyEvent) {

			myService.send(boundServiceName, "keyPressed", keyEvent.getKeyCode());
		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {
		}

		@Override
		public void keyTyped(KeyEvent keyEvent) {
		}
	}

	public final static Logger log = LoggerFactory.getLogger(WiiBotGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	Keyboard keyboard = new Keyboard();

	public WiiBotGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
	};

	@Override
	public void detachGUI() {
	}

	@Override
	public void init() {

		JButton keyboardButton = new JButton(
				"<html><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control</td></tr></table></html>");
		display.add(keyboardButton, gc);
		keyboardButton.addKeyListener(keyboard);

		TitledBorder title;
		JPanel logPanel = new JPanel();
		title = BorderFactory.createTitledBorder("wiibot");
		logPanel.setBorder(title);

		display.add(logPanel, gc);

	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

}