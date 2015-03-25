package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.Pin;

public class Stepper_ArduinoGUI extends StepperControllerPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private GUIService myService;

	JLabel powerPinLabel = new JLabel("<html>power pin<br><font color=white bgcolor=green>speed control</font></html>");
	JLabel directionPinLabel = new JLabel("direction pin");
	JComboBox powerPin = new JComboBox();
	JComboBox directionPin = new JComboBox();
	JButton attachButton = new JButton("attach");
	String arduinoName;
	String motorName;

	ArrayList<Pin> pinList = null;

	public Stepper_ArduinoGUI(GUIService myService, String motorName, String controllerName) {
		super();
		this.myService = myService;
		this.arduinoName = controllerName;
		this.motorName = motorName;
		Arduino o = (Arduino) myService.sendBlocking(controllerName, "publishState", (Object[]) null);
		pinList = o.getPinList();

		for (int i = 0; i < pinList.size(); ++i) {
			Pin pin = pinList.get(i);
			if (pin.type == Pin.PWM_VALUE) {
				powerPin.addItem(String.format("<html><font color=white bgcolor=green>%d</font></html>", pin.pin));
			} else {
				powerPin.addItem(String.format("%d", pin.pin));
			}
		}

		for (int i = 0; i < pinList.size(); ++i) {
			Pin pin = pinList.get(i);
			directionPin.addItem(String.format("%d", pin.pin));
		}

		setBorder(BorderFactory.createTitledBorder("type - Arduino with Simple 2 bit H-bridge"));
		add(powerPinLabel);
		add(powerPin);
		add(directionPinLabel);
		add(directionPin);
		add(attachButton);
		attachButton.addActionListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == attachButton) {
			if ("attach".equals(attachButton.getText())) {
				Object[] motorData = new Object[] { new Integer(powerPin.getSelectedIndex()), new Integer(directionPin.getSelectedIndex()) };
				myService.send(arduinoName, "motorAttach", motorName, motorData);
				attachButton.setText("detach");
			} else {
				myService.send(arduinoName, "motorDetach", motorName);
				attachButton.setText("attach");
			}

		}

	}

	@Override
	void setAttached(boolean state) {
		if (state) {
			attachButton.setText("detach");
		} else {
			attachButton.setText("attach");
		}

	}

	@Override
	public void setData(Object[] data) {
		if (data != null && data[0] != null && data[1] != null) {
			powerPin.setSelectedItem(data[0]);
			directionPin.setSelectedItem(data[1]);
		}
	}

}
