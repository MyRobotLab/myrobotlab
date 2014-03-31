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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.CommunicationNodeList;
import org.myrobotlab.control.widget.ConnectDialog;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.RemoteAdapter;

public class RemoteAdapterGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	JLabel numClients = new JLabel("0");
	
	// TODO - got to get these from service
	public String lastHost = "127.0.0.1";
	public String lastPort = "6767";
	
	JButton connection = new JButton("new connection");
	
	CommunicationNodeList list = new CommunicationNodeList();

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
		
		updateNodeList(null);
		connection.addActionListener(this);
	}

	public void updateNodeList(RemoteAdapter remote) {
		if (remote != null)
		{
			/*
			// FIXME - handle this better !!!
			CommunicationInterface cf = remote.getComm();
			
			if (cf != null)
			{
			Communicator cm = cf.getComm();
			
			HashMap<URI, CommData> clients = cm.getClients();
			
			for (Map.Entry<URI,CommData> o : clients.entrySet())
			{
				//Map.Entry<String,SerializableImage> pairs = o;
				URI uri = o.getKey();
				CommData data = o.getValue();
				list.model.add(0, (Object) new CommunicationNodeEntry(uri, data));
			}
			
			numClients.setText(String.format("%d",clients.size()));
			}
			*/

		}
	}
	
	@Override
	public void actionPerformed(ActionEvent action){
		Object o = action.getSource();
		if (o == connection) {
			ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect", "message", myService, lastHost, lastPort);
			lastHost = dlg.host.getText();
			lastPort = dlg.port.getText();
			String uris = String.format("tcp://%s:%s", lastHost, lastPort);
			try {
				URI uri = new URI(uris);
				Message msg = myService.createMessage("", "register", myService);
				send("sendRemote", uri, msg);
			} catch(Exception e){
				myService.error(e);
			}
		} 
	}
	

	public void getState(final RemoteAdapter remote) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

					updateNodeList(remote);
			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RemoteAdapter.class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RemoteAdapter.class);
	}

}
