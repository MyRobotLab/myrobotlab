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

package org.myrobotlab.control.widget;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class EditorArduino extends Editor implements ActionListener {

	public final static Logger log = LoggerFactory.getLogger(EditorArduino.class);
	static final long serialVersionUID = 1L;

	// button bar buttons
	ImageButton compileButton;
	ImageButton uploadButton;
	public ImageButton connectButton;
	ImageButton newButton;
	ImageButton openButton;
	ImageButton saveButton;
	ImageButton fullscreenButton;
	ImageButton monitorButton;
	JLabel sketchName = new JLabel("MRLComm copy, paste and upload this sketch into the Arduinio IDE");

	Arduino myArduino = null;
	JMenu boardsMenu = new JMenu("Board");
	public JMenu serialDeviceMenu = new JMenu("Serial Device");
	public JMenu digitalPinMenu = new JMenu("Digital Pins");
	JCheckBoxMenuItem digitalDebounce = new JCheckBoxMenuItem("Debounce");
	JCheckBoxMenuItem digitalTriggerOnly = new JCheckBoxMenuItem("Digital Trigger Only");

	public EditorArduino(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs, SyntaxConstants.SYNTAX_STYLE_C);
		ServiceInterface sw = Runtime.getService(boundServiceName);
		myArduino = (Arduino) sw;
		examplesMenu.add(createExamplesMenu());
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		super.actionPerformed(event);
		Object o = event.getSource();

		if (o == compileButton) {
			myService.send(boundServiceName, "compile", sketchName.getText(), textArea.getText());
		} else if (o == uploadButton) {
			myService.send(boundServiceName, "upload", textArea.getText());
			return;
		} else if (o == digitalDebounce) {
			if (digitalDebounce.isSelected()) {
				myService.send(boundServiceName, "digitalDebounceOn");
			} else {
				myService.send(boundServiceName, "digitalDebounceOff");
			}
		} else if (o == digitalTriggerOnly) {
			if (digitalTriggerOnly.isSelected()) {
				myService.send(boundServiceName, "setDigitalTriggerOnly", true);
			} else {
				myService.send(boundServiceName, "setDigitalTriggerOnly", false);
			}
		} else if (o == connectButton) {
		} else if ("examples".equals(event.getActionCommand())) {
			JMenuItem menu = (JMenuItem) o;
			loadResourceFile(menu.getText());
		}
	}

	private JMenu createExamplesMenu() {
		JMenu menu;
		menu = new JMenu("Communication");
		menu.add(createMenuItem("MRLComm.ino", "examples"));

		return menu;
	}

	@Override
	public void init() {
		super.init();
		// NOTE !!! - must be lowercase to match image names
		/*
		compileButton = addImageButtonToButtonBar("Arduino", "compile", this);
		compileButton.setVisible(false);
		uploadButton = addImageButtonToButtonBar("Arduino", "upload", this);
		uploadButton.setVisible(false);
		connectButton = addImageButtonToButtonBar("Arduino", "connect", this);
		connectButton.setVisible(false);
		newButton = addImageButtonToButtonBar("Arduino", "new", this);
		newButton.setVisible(false);
		openButton = addImageButtonToButtonBar("Arduino", "open", this);
		openButton.setVisible(false);
		saveButton = addImageButtonToButtonBar("Arduino", "save", this);
		saveButton.setVisible(false);
		fullscreenButton = addImageButtonToButtonBar("Arduino", "fullscreen", this);
		fullscreenButton.setVisible(false);
		monitorButton = addImageButtonToButtonBar("Arduino", "monitor", this);
		monitorButton.setVisible(false);

		buttonBar.setBackground(new Color(0, 100, 104));
		sketchName.setForeground(new Color(255, 255, 255));
		buttonBar.add(sketchName);

		toolsMenu.add(boardsMenu);
		toolsMenu.add(serialDeviceMenu);
		toolsMenu.add(digitalPinMenu);

		digitalDebounce.setSelected(true);
		digitalDebounce.addActionListener(this);
		digitalPinMenu.add(digitalDebounce);

		digitalTriggerOnly.setSelected(true);
		digitalTriggerOnly.addActionListener(this);
		digitalPinMenu.add(digitalTriggerOnly);

		// add to help menu
		helpMenu.add(createMenuItem("Getting Started"));
		helpMenu.add(createMenuItem("Environment"));
		helpMenu.add(createMenuItem("Troubleshooting"));
		helpMenu.add(createMenuItem("Reference"));
		helpMenu.add(createMenuItem("Find in Reference", saveMenuMnemonic, "control+shift-F", null));
		helpMenu.add(createMenuItem("Frequently Asked Questions"));
		helpMenu.add(createMenuItem("Visit Arduino.cc"));
		*/

	}

	public void loadCommunicationFile() {
		loadResourceFile("MRLComm.ino");
	}

	public void loadResourceFile(String filename) {
		String resourcePath = String.format("Arduino/%s/%s", filename.substring(0, filename.indexOf(".")), filename);
		log.info(String.format("loadResourceFile %s", resourcePath));
		String sketch = FileIO.resourceToString(resourcePath);
		textArea.setText(sketch);
	}
	
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
	}

	

}
