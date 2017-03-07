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
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.PortGui;
import org.python.netty.handler.codec.CodecException;
import org.slf4j.Logger;

public class SerialGui extends ServiceGui implements ActionListener, ItemListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SerialGui.class);
	
	// menu
	JComboBox<String> reqFormat = new JComboBox<String>(new String[] { "decimal", "hex", "ascii", "arduino" });

	// FIXME !!!
	JButton createVirtualPort = new JButton("create virtual uart");
	JButton clear = new JButton("clear");
	JButton record = new JButton();

	// recv data display
	JTextArea rx = new JTextArea(10, 10);
	JLabel rxTotal = new JLabel("0");
	JLabel txTotal = new JLabel("0");

	int rxCount = 0;
	int txCount = 0;

	// trasmit data display
	JTextArea tx = new JTextArea(5, 10);
	JTextArea toSend = new JTextArea(2, 10);
	JButton send = new JButton("send");
	JButton sendFile = new JButton("send file");

	Serial mySerial = null;
	final SerialGui self;

	// gui's formatters
	Codec rxFormatter = new DecimalCodec(myService);
	Codec txFormatter = new DecimalCodec(myService);
	
	PortGui portGui;

	public SerialGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;
		mySerial = (Serial) Runtime.getService(boundServiceName);
		rx.setEditable(false);
		tx.setEditable(false);
		autoScroll(true);
		
		portGui = new PortGui(boundServiceName, myService, tabs);
		addTop(portGui.getDisplay(), " ", reqFormat, clear, record);

		add(new JScrollPane(rx));
		add(new JScrollPane(tx));
		add("send");
		add(new JScrollPane(toSend));
		addBottomGroup(null, send, sendFile, "rx", rxTotal, "tx", txTotal);

		createVirtualPort.addActionListener(this);
		send.addActionListener(this);
		sendFile.addActionListener(this);
		record.addActionListener(this);
		reqFormat.addItemListener(this);
		clear.addActionListener(this);
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
		
		if (o == clear) {
			clear();
		}
		if (o == createVirtualPort) {
			send("connectVirtualUart", "COM88");
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
			String data = toSend.getText();
			send("write", data.getBytes());
			myService.info("sent [%s]", data);
		}
	}
	
	public void clear(){
		rx.setText("");
		tx.setText("");
	}

	@Override
	public void subscribeGui() {
		subscribe("publishRX");
		subscribe("publishTX");
		subscribe("publishState");
		// forces scan of ports		
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishRX");
		unsubscribe("publishTX");
		unsubscribe("publishState");
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
					mySerial = serial;					
					if (!serial.isRecording()) {
						record.setText("record");
					} else {
						record.setText("stop recording");
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
		/*
		if (o == ports && event.getStateChange() == ItemEvent.SELECTED) {
			String port = (String) ports.getSelectedItem();
			if (port.length() == 0) {
				// send("disconnect");
			} else if (!port.equals(mySerial.getPortName()) && port.length() > 0) {
				// send("disconnect");
				send("connect", port);
			}
		}
		*/

		if (o == reqFormat) {
			String newFormat = (String) reqFormat.getSelectedItem();
			// changing our display and the Service's format
			try {
				clear();
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
    if(this.myService.getInbox().size() > 500) {
      rx.append("... ");
      return;
    }
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

}
