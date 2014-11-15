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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.slf4j.Logger;

public class SerialGUI extends ServiceGUI implements ActionListener, ItemListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SerialGUI.class.getCanonicalName());

	// menu
	JComboBox<String> format = new JComboBox<String>(new String[] { Serial.FORMAT_DECIMAL, Serial.FORMAT_HEX, Serial.FORMAT_ASCII });
	JComboBox<String> ports = new JComboBox<String>();

	JButton createVirtualUART = new JButton("create virtual uart");
	JButton captureRX = new JButton();
	JButton captureTX = new JButton();
	//JButton sendTx = new JButton("send tx from file");

	JLabel connectLight = new JLabel();

	JTextArea rx = new JTextArea(20, 40);
	JLabel rxTotal = new JLabel("0");
	JLabel txTotal = new JLabel("0");
	String delimiter = " ";
	Integer width = 16;
	JTextField widthMenu = new JTextField("16");

	int rxCount = 0;
	int txCount = 0;

	//JTextField sendData = new JTextField(40);
	JTextArea sendData = new JTextArea(3, 40);
	JButton send = new JButton("send");
	JButton sendFile = new JButton("send file");

	Serial mySerial = null;

	// TODO
	// save data to file button
	// send file
	// create virtual port
	// create null modem cable

	public SerialGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		mySerial = (Serial) Runtime.getService(boundServiceName);
	}

	public void init() {
		display.setLayout(new BorderLayout());

		JPanel north = new JPanel();
		north.add(new JLabel("port "));
		north.add(ports);
		north.add(connectLight);
		north.add(new JLabel(" "));
		north.add(format);
		north.add(new JLabel("width "));
		north.add(widthMenu);
		north.add(createVirtualUART);
		north.add(captureRX);
		//north.add(sendTx);

		display.add(north, BorderLayout.NORTH);

		rx.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(rx);

		autoScroll(true);

		display.add(scrollPane, BorderLayout.CENTER);

		JPanel south = new JPanel();

		south.add(sendData);
		south.add(send);
		south.add(sendFile);
		south.add(new JLabel("rx"));
		south.add(rxTotal);
		south.add(new JLabel("tx"));
		south.add(txTotal);
		display.add(south, BorderLayout.SOUTH);

		createVirtualUART.addActionListener(this);
		send.addActionListener(this);
		sendFile.addActionListener(this);
		captureRX.addActionListener(this);
		ports.addItemListener(this);

	}

	public void autoScroll(boolean b) {
		DefaultCaret caret = (DefaultCaret) rx.getCaret();
		if (b) {
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	public void getState(final Serial serial) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mySerial = serial;
				setPortStatus();
				if (serial.isRXRecording()){
					captureRX.setText(serial.getRXFileName());
				} else {
					captureRX.setText("capture rx to file");
				}
				/*
				if (serial.isTXRecording()){
					captureTX.setText(serial.getTXFileName());
				}
				*/
			}
		});
	}

	public void setPortStatus() {
		ports.removeItemListener((ItemListener) self);
		if (mySerial.isConnected()) {
			connectLight.setIcon(Util.getImageIcon("green.png"));
			log.info(String.format("displaying %s", mySerial.getPortName()));
			ports.setSelectedItem(mySerial.getPortName());
		} else {
			connectLight.setIcon(Util.getImageIcon("red.png"));
			ports.setSelectedItem("");
		}
		ports.addItemListener((ItemListener) self);
	}

	public void getPortNames(final ArrayList<String> inPorts) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				ports.removeItemListener((ItemListener) self);
				ports.removeAllItems();
				ports.addItem("");
				for (int i = 0; i < inPorts.size(); ++i) {
					ports.addItem(inPorts.get(i));
				}
				ports.addItemListener((ItemListener) self);
				setPortStatus();
			}
		});
	}

	public void publishByte(final Integer b) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// FIXME - normalize with Serial.format
				++rxCount;
				String f = (String) format.getSelectedItem();
				if (f.equals(Serial.FORMAT_DECIMAL)) {
					rx.append(String.format("%03d%s", b, delimiter));
				} else if (f.equals(Serial.FORMAT_HEX)) {
					rx.append(String.format("%02x%s", (int) b & 0xff, delimiter));
				} else if (f.equals(Serial.FORMAT_ASCII)) {
					rx.append(String.format("%c%s", (int) b & 0xff, delimiter));
				}
				if (width != null && rxCount % width == 0) {
					rx.append("\n");
				}
				rxTotal.setText(String.format("%d", rxCount));
			}
		});

	}

	@Override
	public void attachGUI() {
		subscribe("publishByte", "publishByte", Integer.class);
		subscribe("publishState", "getState", Serial.class);
		subscribe("getPortNames", "getPortNames", ArrayList.class);

		send("publishState");
		send("getPortNames");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishByte", "publishByte", Integer.class);
		unsubscribe("publishState", "getState", Serial.class);
		unsubscribe("getPortNames", "getPortNames", ArrayList.class);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == captureRX) {
			if (captureRX.getText().startsWith("capture")){
				send("recordRX");
				send("broadcastState");
			} else {
				send("stopRXRecording");
				send("broadcastState");
			}
		}

		if (o == captureTX) {
			if (captureTX.getText().startsWith("capture")){
			send("recordTX");
			send("broadcastState");
			} else {
				send("stopTXRecording");
			}
		}

		if (o == createVirtualUART){
			send("createVirtualUART");
		}
		
		if (o == sendFile) {
			JFileChooser fileChooser = new JFileChooser();
			// set current directory
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
			int result = fileChooser.showOpenDialog(this.getDisplay());
			if (result == JFileChooser.APPROVE_OPTION) {
				// user selects a file
				File selectedFile = fileChooser.getSelectedFile();
				send("writeFile", selectedFile.getAbsolutePath());
			}
		}

		if (o == ports) {
			String selected = (String) ports.getSelectedItem();
			if (selected == null || "".equals(selected)) {
				send("stopPolling");
			} else {
				log.info(String.format("changed to %s ", selected));
				send("setController", selected);
				send("startPolling");
			}
		}
		
		if (o == send) {
			String data = sendData.getText();
			send("write", data.getBytes());
			myService.info("sent [%s]", data);
		}

	}

	// onChange of ports
	@Override
	public void itemStateChanged(ItemEvent event) {
		Object o = event.getSource();
		if (o == ports){
			String port = (String)ports.getSelectedItem();
			if (port != null && !port.equals(mySerial.getPortName()) && port.length() > 0){
				send("disconnect");
				send("connect", port);
			} else if (port.length() == 0){
				send("disconnect");
			}
		}
	}

}
