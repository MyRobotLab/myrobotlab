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

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.CommunicationNodeEntry;
import org.myrobotlab.control.widget.CommunicationNodeList;
import org.myrobotlab.image.Util;
import org.myrobotlab.net.CommData;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.RemoteAdapter;

public class RemoteAdapterGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	JLabel numClients = new JLabel("0");

	JButton connection = new JButton("connect");

	// display of the CommData getClients
	CommunicationNodeList list = new CommunicationNodeList();
	String lastProtoKey;

	RemoteAdapter myRemote = null;

	public RemoteAdapterGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;

		display.add(connection, gc);
		++gc.gridy;
		display.add(new JLabel("number of connections :"), gc);
		++gc.gridy;
		display.add(new JLabel("last activity : "), gc);
		++gc.gridy;
		display.add(new JLabel("number of messages : "), gc);
		++gc.gridy;
		// list.setPreferredSize(new Dimension(arg0, arg1))
		gc.gridwidth = 4;
		gc.fill = GridBagConstraints.HORIZONTAL;
		display.add(list, gc);
		connection.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent action) {
		Object o = action.getSource();
		if (o == connection) {
			String newProtoKey = (String) JOptionPane.showInputDialog(myService.getFrame(), "<html>connect to a remote MyRobotLab</html>", "connect", JOptionPane.WARNING_MESSAGE,
					Util.getResourceIcon("RemoteAdapter/connect.png"), null, lastProtoKey);
			
			if (newProtoKey == null || newProtoKey == "") {
				return;
			}

			send("connect", newProtoKey);
			lastProtoKey = newProtoKey;
		}
	}

	public void getState(final RemoteAdapter remote) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				myRemote = remote;
				lastProtoKey = remote.lastProtoKey;
				if (remote.getClients() == null){
					return;
				}
				for (Map.Entry<URI, CommData> o : remote.getClients().entrySet()) {
					// Map.Entry<String,SerializableImage> pairs = o;
					URI uri = o.getKey();
					CommData data = o.getValue();
					list.model.add(0, (Object) new CommunicationNodeEntry(uri, data));
				}
			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RemoteAdapter.class);
		send("broadcastState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RemoteAdapter.class);
	}

}
