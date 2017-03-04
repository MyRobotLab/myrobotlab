package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.swing.ServiceGui;

public class PortGui extends ServiceGui implements ActionListener, PortListener {
	JLabel connectLight = new JLabel();
	JComboBox<String> ports = new JComboBox<String>();
	JButton connect = new JButton("connect");
	JButton refresh = new JButton("refresh");

	public PortGui(String boundServiceName, SwingGui myService, JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		ports.setEditable(true);
		add(connectLight, ports, connect, refresh);
		onState((PortPublisher) Runtime.getService(boundServiceName));
		subscribeGui();
		connect.addActionListener(this);
		refresh.addActionListener(this);
		
		myService.subscribeToServiceMethod(boundServiceName, this);
	}

	/**
	 * make note !!! - this can 'conflict' with other UI subscribed to the same
	 * boundServiceName !! - if one UI makes mapping which who's callback does not
	 * match the other UI
	 */
	@Override
	public void subscribeGui() {
		subscribe("publishState");
		subscribe("getPortNames");
		subscribe("publishConnect");
		subscribe("publishDisconnect");
		send("refresh");
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishState");
		unsubscribe("getPortNames");
		unsubscribe("publishConnect");
		unsubscribe("publishDisconnect");
	}

	public void onState(final PortPublisher portPublisher) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onPortNames(portPublisher.getPortNames());				
				// set light if connected
				if (portPublisher.isConnected()) {
					connectLight.setIcon(Util.getImageIcon("green.png"));
					ports.setEnabled(false);
					connect.setText("disconnect");
					ports.setSelectedItem(portPublisher.getPortName());	
				} else {
					connectLight.setIcon(Util.getImageIcon("red.png"));
					ports.setEnabled(true);
					connect.setText("connect");
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (connect == o){
			if (connect.getText().equals("connect")){
				send("connect", ports.getSelectedItem());
			} else {
				send("disconnect");
			}
		}
		
		if (refresh == o){
			send("refresh");
		}
	}

	@Override
	public String getName() {
		return boundServiceName;
	}

	@Override
	public void onConnect(String portName) {
		log.info("onConnect - {}", portName);
	}

	@Override
	public void onDisconnect(String portName) {
		log.info("onDisconnect - {}", portName);
	}

	public void onPortNames(final List<String> inPorts) {
		ports.removeAllItems();
		for (int i = 0; i < inPorts.size(); ++i) {
			ports.addItem(inPorts.get(i));
		}
	}

}
