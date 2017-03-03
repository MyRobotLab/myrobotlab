package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
	}

	// NOT CALLED BY FRAMEWORK
	// in a way this is 'not' necessary
	// because ... the SerialGui has already
	// done subscriptions from SerialGui --to--> SwingGui
	public void subscribeGui() {
		subscribe("publishState");
		subscribe("getPortNames");
	}

	public void unsubscribeGui() {
		unsubscribe("publishState");
		unsubscribe("getPortNames");
	}

	public void onState(final PortPublisher portPublisher) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setPortName(portPublisher.getPortName());
				setPortNames(portPublisher.getPortNames());
				setConnected(portPublisher.isConnected());
				if (portPublisher.isConnected()) {
					setPortName(portPublisher.getPortName());
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (connect == o){
			if (connect.equals("connect")){
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

	}

	@Override
	public void onDisconnect(String portName) {

	}

	public void setPortNames(final List<String> inPorts) {
		ports.removeAllItems();
		for (int i = 0; i < inPorts.size(); ++i) {
			ports.addItem(inPorts.get(i));
		}
	}

	// FIXME - must remove itemListener ?? !!!
	public void setPortName(String portName) {
		log.info(String.format("displaying %s", portName));
		ports.setSelectedItem(portName);
	}

	public void setConnected(boolean isconnected) {
		if (isconnected) {
			connectLight.setIcon(Util.getImageIcon("green.png"));
			ports.setEnabled(false);
			connect.setText("disconnect");
		} else {
			connectLight.setIcon(Util.getImageIcon("red.png"));
			ports.setEnabled(true);
			connect.setText("connect");
		}
	}

}
