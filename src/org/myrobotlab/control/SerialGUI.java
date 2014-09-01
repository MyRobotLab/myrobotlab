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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Serial;
import org.slf4j.Logger;

public class SerialGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SerialGUI.class.getCanonicalName());
	public final String FORMAT_DECIMAL = "decimal";
	public final String FORMAT_HEX = "hex";
	public final String FORMAT_ASCII = "ascii";

	// menu
	JComboBox<String> format = new JComboBox<String>(new String[] { FORMAT_DECIMAL, FORMAT_HEX, FORMAT_ASCII });

	JComboBox<String> port = new JComboBox<String>();

	JButton createNullModemCabel = new JButton("create null modem cable");
	JButton captureRX = new JButton("capture rx to file");
	JButton sendTx = new JButton("send tx from file");

	JTextArea rx = new JTextArea(20, 40);
	JLabel rxTotal = new JLabel("0");
	JLabel txTotal = new JLabel("0");
	String delimiter = " ";
	Integer width = 16;
	JTextField widthMenu = new JTextField("16");

	// String format = FORMAT_DECIMAL; // HEX, ASCII

	int rxCount = 0;
	int txCount = 0;

	int bufferSize = 999;
	JTextField sendData = new JTextField(40);
	JButton sendButton = new JButton("send");

	// TODO
	// save data to file button
	// send file
	// create virtual port
	// create null modem cable

	public SerialGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void init() {
		display.setLayout(new BorderLayout());

		// JPanel toolbar = new JPanel(new BorderLayout());
		JPanel north = new JPanel();
		north.add(new JLabel("format "));
		north.add(format);
		north.add(new JLabel("width "));
		north.add(widthMenu);
		north.add(createNullModemCabel);
		north.add(captureRX);
		north.add(sendTx);

		display.add(north, BorderLayout.NORTH);

		rx.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(rx);

		autoScroll(true);

		display.add(scrollPane, BorderLayout.CENTER);

		JPanel south = new JPanel();

		south.add(sendData);
		south.add(sendButton);
		south.add(new JLabel("rx"));
		south.add(rxTotal);
		south.add(new JLabel("tx"));
		south.add(txTotal);
		display.add(south, BorderLayout.SOUTH);

	}

	public void autoScroll(boolean b) {
		DefaultCaret caret = (DefaultCaret) rx.getCaret();
		if (b) {
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	public void getState(Serial template) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

			}
		});
	}

	public void publishByte(final Byte b) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				++rxCount;
				String f = (String) format.getSelectedItem();
				if (f.equals(FORMAT_DECIMAL)) {
					rx.append(String.format("%03d%s", (int) b & 0xff, delimiter));
				} else if (f.equals(FORMAT_HEX)) {
					rx.append(String.format("%02x%s", (int) b & 0xff, delimiter));
				} else if (f.equals(FORMAT_ASCII)) {
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

		subscribe("publishByte", "publishByte", Byte.class);

		subscribe("publishState", "getState", Serial.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Serial.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

}
