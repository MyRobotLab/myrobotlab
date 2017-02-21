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

package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import org.myrobotlab.codec.serial.Codec;
import org.myrobotlab.codec.serial.DecimalCodec;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.myrobotlab.service.SwingGui;
import org.python.netty.handler.codec.CodecException;
import org.slf4j.Logger;

public class SerialGui extends ServiceGui implements ActionListener, ItemListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SerialGui.class);

	// menu
	JComboBox<String> reqFormat = new JComboBox<String>(new String[] { "decimal", "hex", "ascii", "arduino" });
	JComboBox<String> ports = new JComboBox<String>();
	JButton connect = new JButton("connect");
	JButton refresh = new JButton("refresh");

	JButton createVirtualPort = new JButton("create virtual uart");
	JButton record = new JButton();
	

	JLabel connectLight = new JLabel();

	JTextArea rx = new JTextArea(10, 10);
	JLabel rxTotal = new JLabel("0");
	JLabel txTotal = new JLabel("0");

	int rxCount = 0;
	int txCount = 0;

	JTextArea tx = new JTextArea(10, 10);
	JButton send = new JButton("send");
	JButton sendFile = new JButton("send file");

	Serial mySerial = null;
	final SerialGui myself;

	// gui's formatters
	Codec rxFormatter = new DecimalCodec(myService);
	Codec txFormatter = new DecimalCodec(myService);

	public SerialGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myself = this;
		mySerial = (Serial) Runtime.getService(boundServiceName);
		rx.setEditable(false);
		autoScroll(true);

		addTop(null, connectLight, "  port: ", ports, connect, refresh, " ", reqFormat, createVirtualPort, record);

		addLine(new JScrollPane(rx));
		addLine(new JScrollPane(tx));
		
		addBottomGroup(null, send, sendFile, "rx", rxTotal, "tx", txTotal);

		createVirtualPort.addActionListener(this);
		send.addActionListener(this);
		sendFile.addActionListener(this);
		record.addActionListener(this);
		connect.addActionListener(this);
		reqFormat.addItemListener(this);
		refresh.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == record) {
			if (record.getText().startsWith("record")) {
				send("record");
				send("broadcastState");
			} else {
				send("stopRecording");
				send("broadcastState");
			}
		}
		if (o == connect) {
			// TODO: make this connect/disconnect
			if (mySerial.isConnected()) {
				mySerial.disconnect();
				connect.setText("connect");
			} else {
				try {
					mySerial.open((String) ports.getSelectedItem());
					connect.setText("disconnect");
				} catch (Exception e2) {
					myService.error("could not connect");
					log.error("connect in gui threw", e2);
				}
			}

		}
		if (o == createVirtualPort) {
			send("connectVirtualUart", "COM88");
		}
		if (o == refresh) {
			send("refresh");
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

		if (o == send) {
			String data = tx.getText();
			send("write", data.getBytes());
			myService.info("sent [%s]", data);
		}

	}

	@Override
	public void subscribeGui() {
		subscribe("publishRX");
		subscribe("publishTX");
		subscribe("publishState");
		subscribe("getPortNames");
		// forces scan of ports
		send("refresh");
		send("getPortNames");
	}

	public void autoScroll(boolean b) {
		DefaultCaret caret = (DefaultCaret) rx.getCaret();
		if (b) {
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}

		DefaultCaret caretTX = (DefaultCaret) tx.getCaret();
		if (b) {
			caretTX.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			caretTX.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishRX");
		unsubscribe("publishTX");
		unsubscribe("publishState");
	}

	public void onPortNames(final List<String> inPorts) {
		ports.removeAllItems();
		for (int i = 0; i < inPorts.size(); ++i) {
			ports.addItem(inPorts.get(i));
		}
	}

	/**
	 * the gui is no simplified - a single broadcastState() -> onState(Serial)
	 * is used to propegate all data which needs updating. Since that is the
	 * case a single invokeLater is used. It is unadvised to have more
	 * invokeLater in other methods as race conditions are possible
	 * 
	 * @param serial
	 */
	public void onState(final Serial serial) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {

					// prevent re-firing the event :P
					reqFormat.removeItemListener(myself);
					// ports.removeItemListener(myself);

					mySerial = serial;
					// refresh all the ports in the combo box
					onPortNames(serial.getPortNames());

					// set the appropriate status
					// ie connection value and current port
					setPortStatus();

					// ports.addItemListener(myself);
					reqFormat.addItemListener(myself);

					if (!serial.isRecording()) {
						record.setText("record");
					} else {
						record.setText("stop recording");
					}
					if (serial.isConnected()) {
						connect.setText("disconnect");
						ports.setEnabled(false);
					} else {
						connect.setText("connect");
						ports.setEnabled(true);
					}
				} catch (Exception e) {
					log.error("onState threw", e);
				}
			}
		});
	}

	// onChange of ports
	@Override
	public void itemStateChanged(ItemEvent event) {
		Object o = event.getSource();
		if (o == ports && event.getStateChange() == ItemEvent.SELECTED) {
			String port = (String) ports.getSelectedItem();
			if (port.length() == 0) {
				// send("disconnect");
			} else if (!port.equals(mySerial.getPortName()) && port.length() > 0) {
				// send("disconnect");
				send("connect", port);
			}
		}

		if (o == reqFormat) {
			String newFormat = (String) reqFormat.getSelectedItem();
			// changing our display and the Service's format
			try {
				rxFormatter = Codec.getDecoder(newFormat, myService);
				txFormatter = Codec.getDecoder(newFormat, myService);
				send("setFormat", newFormat);
			} catch (Exception e) {
				Logging.logError(e);
			}
		}
	}

	/**
	 * onRX displays the "interpreted" byte it is interpreted by the Serial's
	 * service selected "format"
	 * 
	 * FORMAT_DECIMEL is a 3 digit decimal in ascii FORMAT_RAW is interpreted as
	 * 1 byte = 1 ascii char FORMAT_HEX is 2 digit asci hex
	 * 
	 * @param data
	 * @throws BadLocationException
	 * @throws CodecException
	 */
	public final void onRX(final Integer data) throws BadLocationException {
		++rxCount;
		String formatted = rxFormatter.decode(data);
		rx.append(formatted);
		if (formatted != null && rx.getLineCount() > 50) {
			Document doc = rx.getDocument();
			doc.remove(0, formatted.length());
		}

		rxTotal.setText(String.format("%d", rxCount));
	}

	public final void onTX(final Integer data) {
		++txCount;
		tx.append(txFormatter.decode(data));
		txTotal.setText(String.format("%d", txCount));
	}

	public void setPortStatus() {
		ports.removeItemListener(myself);
		if (mySerial.isConnected()) {
			connectLight.setIcon(Util.getImageIcon("green.png"));
			log.info(String.format("displaying %s", mySerial.getPortName()));
			ports.setSelectedItem(mySerial.getPortName());
		} else {
			connectLight.setIcon(Util.getImageIcon("red.png"));
			ports.setSelectedItem("");
		}
		// zod ports.addItemListener(myself);
	}

}
