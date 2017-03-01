package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.swing.ServiceGui;

public class PortGui extends ServiceGui implements ActionListener, PortListener {
	JLabel connectLight = new JLabel();
	JComboBox<String> ports = new JComboBox<String>();
	JButton connect = new JButton("connect");
	JMenuItem refresh = new JMenuItem("refresh");

	public PortGui(String boundServiceName, SwingGui myService, JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);		
	}
	
	/*
	public PortGui(PortPublisher portPublisher, ServiceGui serviceGui) {
		this.serviceGui = serviceGui;
		serviceGui.send(portPublisher.getName(), "addListener","");
	}
	*/

	/*
	public Object[] getDisplay() {
		Object[] components = new Object[]{connectLight, " port:", ports, connect};
		return components;
	}
	*/


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void onConnect(String portName) {

	}

	@Override
	public void onDisconnect(String portName) {

	}
}
