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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class FrogLegGUI extends ServiceGUI implements ListSelectionListener {

	public class Keyboard implements KeyListener {

		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");

		@Override
		public void keyPressed(KeyEvent keyEvent) {

			// myService.send(boundServiceName, "keyCommand",
			// keyEvent.getKeyCode());
			myService.send(boundServiceName, "keyCommand", KeyEvent.getKeyText(keyEvent.getKeyCode()));

			Calendar cal = Calendar.getInstance();
			addLogEntry(sdf.format(cal.getTime()) + " " + keyEvent.getKeyCode() + " " + KeyEvent.getKeyText(keyEvent.getKeyCode()));

		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {
			// log.error("Released" + keyEvent);
		}

		@Override
		public void keyTyped(KeyEvent keyEvent) {
			// log.error("Typed" + keyEvent);
		}

		/*
		 * private void printIt(String title, KeyEvent keyEvent) { //int keyCode
		 * = keyEvent.getKeyCode(); //String keyText =
		 * KeyEvent.getKeyText(keyCode); // log.error(title + " : " + keyText +
		 * " / " + // keyEvent.getKeyChar()); }
		 */
	}

	public final static Logger log = LoggerFactory.getLogger(FrogLegGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	JLabel boundPos = null;
	JLabel loginValue = new JLabel("");

	JLabel msgCountValue = new JLabel("");
	VideoWidget video0 = null;

	VideoWidget video1 = null;
	BasicArrowButton forward = new BasicArrowButton(BasicArrowButton.NORTH);
	BasicArrowButton back = new BasicArrowButton(BasicArrowButton.SOUTH);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);
	JList currentPlayers;

	JList currentLog;

	DefaultListModel logModel = new DefaultListModel();

	int msgCount = 0;

	Keyboard keyboard = null;

	public FrogLegGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	/*
	 * public void rosterUpdate(ArrayList<String> newRoster) {
	 * rosterModel.clear(); for (int i = 0; i < newRoster.size(); ++i) {
	 * rosterModel.add(i, newRoster.get(i)); } }
	 */
	public void addLogEntry(String msg) {
		logModel.add(0, msg);
	};

	@Override
	public void attachGUI() {
		video0.attachGUI();
		video1.attachGUI();
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void detachGUI() {
		video0.detachGUI();
		video1.detachGUI();
	}

	public void displayFrame(SerializableImage img) {

		video0.displayFrame(img);
	}

	@Override
	public void init() {

		video0 = new VideoWidget(boundServiceName, myService, tabs);
		video0.init();
		video1 = new VideoWidget(boundServiceName, myService, tabs);
		video1.init();
		keyboard = new Keyboard();
		// build input begin ------------------

		gc.gridx = 0;
		gc.gridy = 0;

		display.add(video0.display, gc);
		++gc.gridx;
		display.add(video1.display, gc);
		gc.gridx = 0;

		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("login "), gc);
		gc.gridx = 1;
		display.add(loginValue, gc);

		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("msgs "), gc);
		gc.gridx = 1;
		display.add(msgCountValue, gc);

		++gc.gridy;
		gc.gridx = 0;
		++gc.gridy;
		JButton keyboardButton = new JButton(
				"<html><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control</td></tr></table></html>");
		display.add(keyboardButton, gc);
		keyboardButton.addKeyListener(keyboard);

		gc.gridx = 1;

		currentLog = new JList(logModel);
		currentLog.setFixedCellWidth(200);
		currentLog.addListSelectionListener(this);
		currentLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentLog.setVisibleRowCount(10);

		TitledBorder title;
		JPanel logPanel = new JPanel();
		JScrollPane logScrollPane = new JScrollPane(currentPlayers);
		title = BorderFactory.createTitledBorder("key log");
		logPanel.setBorder(title);
		logPanel.add(logScrollPane);

		display.add(logPanel, gc);

		JScrollPane currentFiltersScrollPane2 = new JScrollPane(currentLog);
		logPanel.add(currentFiltersScrollPane2);

		++gc.gridy;
		++gc.gridy;
		++gc.gridy;
		gc.gridx = 0;

		gc.gridx = 1;
	}

	public void setLogin(String login) {
		loginValue.setText(login);
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

}