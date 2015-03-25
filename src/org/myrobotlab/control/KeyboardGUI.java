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
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class KeyboardGUI extends ServiceGUI implements ListSelectionListener {

	public class CheckBoxChange implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			JCheckBox t = (JCheckBox) e.getSource();
			if (t.getModel().isSelected()) {
				sendStrings = true;
			} else {
				sendStrings = false;
			}
		}
	}

	public class Keyboard implements KeyListener {

		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");

		@Override
		public void keyPressed(KeyEvent keyEvent) {

			int code = keyEvent.getKeyCode();
			String text = KeyEvent.getKeyText(code);

			if (sendStrings) {
				// keyBuffer.append(b)
				if (code == KeyEvent.VK_ENTER) {
					myService.send(boundServiceName, "keyCommand", keyBuffer.toString());
					addLogEntry(sdf.format(cal.getTime()) + " " + keyBuffer.toString());
					keyBuffer.setLength(0);
				} else {
					keyBuffer.append(text);
				}
			} else {
				myService.send(boundServiceName, "keyCommand", text);
				addLogEntry(sdf.format(cal.getTime()) + " " + keyEvent.getKeyCode() + " " + KeyEvent.getKeyText(keyEvent.getKeyCode()));
			}

		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {
			// log.error("Released" + keyEvent);
		}

		@Override
		public void keyTyped(KeyEvent keyEvent) {
			// log.error("Typed" + keyEvent);
		}

	}

	public final static Logger log = LoggerFactory.getLogger(KeyboardGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;
	JList<String> currentPlayers;

	JList<String> currentLog;

	JCheckBox sendStringsCheckBox;

	public boolean sendStrings = false;
	DefaultListModel<String> logModel = new DefaultListModel<String>();
	int msgCount = 0;

	StringBuffer keyBuffer = new StringBuffer();

	Keyboard keyboard = null;

	Calendar cal = Calendar.getInstance();

	/**
	 * @wbp.parser.entryPoint
	 */
	public KeyboardGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void addLogEntry(String msg) {
		logModel.add(0, msg);
	};

	@Override
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

	@Override
	public void init() {

		keyboard = new Keyboard();
		CheckBoxChange checkBoxChange = new CheckBoxChange();
		// build input begin ------------------
		sendStringsCheckBox = new JCheckBox();
		sendStringsCheckBox.setName("send strings");

		JButton keyboardButton = new JButton(
				"<html><body><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control.</td></tr></table></body></html>");

		display.add(keyboardButton, gc);
		keyboardButton.addKeyListener(keyboard);

		++gc.gridx;
		display.add(sendStringsCheckBox, gc);
		++gc.gridx;
		display.add(new JLabel("send strings"), gc);

		currentLog = new JList(logModel);
		currentLog.setFixedCellWidth(400);
		currentLog.addListSelectionListener(this);
		currentLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentLog.setVisibleRowCount(10);

		TitledBorder title;
		JPanel logPanel = new JPanel();
		JScrollPane logScrollPane = new JScrollPane(currentPlayers);
		title = BorderFactory.createTitledBorder("key log");
		logPanel.setBorder(title);
		logPanel.add(logScrollPane);

		gc.gridx = 0;
		gc.gridwidth = 3;
		++gc.gridy;
		display.add(logPanel, gc);

		JScrollPane currentFiltersScrollPane2 = new JScrollPane(currentLog);
		logPanel.add(currentFiltersScrollPane2);

		sendStringsCheckBox.addChangeListener(checkBoxChange);

	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

}