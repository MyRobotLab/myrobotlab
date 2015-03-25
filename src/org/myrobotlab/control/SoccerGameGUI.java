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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class SoccerGameGUI extends ServiceGUI implements ListSelectionListener {

	public class Keyboard implements KeyListener {
		@Override
		public void keyPressed(KeyEvent keyEvent) {

			if (keyEvent.getKeyCode() == 84) {
				announceValue.setVisible(true);
				announceButton.setVisible(true);
			}
			myService.send(boundServiceName, "playerCommand", keyEvent.getKeyCode());

		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {
			log.error("Released" + keyEvent);
		}

		@Override
		public void keyTyped(KeyEvent keyEvent) {
			log.error("Typed" + keyEvent);
		}

		private void printIt(String title, KeyEvent keyEvent) {
			int keyCode = keyEvent.getKeyCode();
			String keyText = KeyEvent.getKeyText(keyCode);
			log.error(title + " : " + keyText + " / " + keyEvent.getKeyChar());
		}
	}

	public final static Logger log = LoggerFactory.getLogger(SoccerGameGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	JLabel boundPos = null;
	JLabel timeValue = new JLabel("20");
	JLabel statusValue = new JLabel("connected");
	JLabel typeValue = new JLabel("player");
	JLabel teamValue = new JLabel("");
	JLabel loginValue = new JLabel("");

	JLabel msgCountValue = new JLabel("");
	JTextField announceValue = new JTextField("hello", 15);

	JButton announceButton = null;
	BasicArrowButton forward = new BasicArrowButton(BasicArrowButton.NORTH);
	BasicArrowButton back = new BasicArrowButton(BasicArrowButton.SOUTH);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);
	JList currentPlayers;
	JList currentLog;
	DefaultListModel rosterModel = new DefaultListModel();

	DefaultListModel logModel = new DefaultListModel();

	int msgCount = 0;

	Keyboard keyboard = null;

	public SoccerGameGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void addLogEntry(String msg) {
		logModel.add(0, msg);
	}

	@Override
	public void attachGUI() {
	};

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource("/resource/" + path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void detachGUI() {
	}

	public JButton getAnnounceButton() {
		JButton button = new JButton("send");
		// button.setVisible(false);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				myService.send(boundServiceName, "announce", announceValue.getText());
			}

		});

		return button;

	}

	@Override
	public void init() {

		keyboard = new Keyboard();
		// build input begin ------------------

		gc.gridx = 0;
		gc.gridy = 0;
		display.add(new JLabel("time "), gc);

		gc.gridx = 1;
		display.add(timeValue, gc);

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
		display.add(new JLabel("team "), gc);
		gc.gridx = 1;
		display.add(teamValue, gc);

		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("type "), gc);
		gc.gridx = 1;
		display.add(typeValue, gc);

		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("status "), gc);
		gc.gridx = 1;
		display.add(statusValue, gc);

		if (typeValue.getText().compareTo("cameraman") == 0) {
			++gc.gridy;
			gc.gridx = 0;
			display.add(new JLabel("SWEET "), gc);
			gc.gridx = 1;
			display.add(statusValue, gc);

		}

		++gc.gridy;
		gc.gridx = 0;
		// ImageIcon pic = new ImageIcon("soccerball.jpg");
		ImageIcon pic = createImageIcon("soccerball.jpg", "its a soccer ball, duh");
		display.add(new JLabel(pic), gc);

		gc.gridx = 1;
		currentPlayers = new JList(rosterModel);
		currentPlayers.setFixedCellWidth(200);
		currentPlayers.addListSelectionListener(this);
		currentPlayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentPlayers.setVisibleRowCount(10);

		currentLog = new JList(logModel);
		currentLog.setFixedCellWidth(200);
		currentLog.addListSelectionListener(this);
		currentLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentLog.setVisibleRowCount(10);

		TitledBorder title;
		JPanel rosterLogPanel = new JPanel();
		JScrollPane currentFiltersScrollPane = new JScrollPane(currentPlayers);
		title = BorderFactory.createTitledBorder("player roster / play log");
		rosterLogPanel.setBorder(title);
		rosterLogPanel.add(currentFiltersScrollPane);

		display.add(rosterLogPanel, gc);

		JScrollPane currentFiltersScrollPane2 = new JScrollPane(currentLog);
		rosterLogPanel.add(currentFiltersScrollPane2);

		// myService.getCFG("playerType");
		/*
		 * // input begin JPanel input = new JPanel(new GridBagLayout());
		 * GridBagConstraints gc2 = new GridBagConstraints(); gc2.gridx = 1;
		 * gc2.gridy = 0; input.add(forward, gc2);
		 * 
		 * gc2.gridx = 0; gc2.gridy = 1; input.add(left, gc2);
		 * 
		 * gc2.gridx = 2; gc2.gridy = 1; input.add(right, gc2);
		 * 
		 * gc2.gridx = 1; gc2.gridy = 2; input.add(back, gc2);
		 * 
		 * // re-using gc gc.gridx = 1; ++gc.gridy; display.add(input, gc); //
		 * input end
		 */

		gc.gridx = 0;
		++gc.gridy;
		JButton keyboardButton = new JButton(
				"<html><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control</td></tr></table></html>");
		display.add(keyboardButton, gc);
		keyboardButton.addKeyListener(keyboard);

		++gc.gridy;
		++gc.gridy;
		++gc.gridy;
		gc.gridx = 0;

		// announceValue.setVisible(false);
		display.add(announceValue, gc);

		gc.gridx = 1;

		announceButton = getAnnounceButton();
		display.add(announceButton, gc);

	}

	public void rosterUpdate(ArrayList<String> newRoster) {
		rosterModel.clear();
		for (int i = 0; i < newRoster.size(); ++i) {
			rosterModel.add(i, newRoster.get(i));
		}
	}

	public void setLogin(String login) {
		loginValue.setText(login);
	}

	public void setTeam(String team) {
		teamValue.setText(team);
	}

	public void setType(String type) {
		typeValue.setText(type);
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

}