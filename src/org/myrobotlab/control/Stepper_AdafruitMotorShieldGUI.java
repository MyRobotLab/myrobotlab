package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.Pin;

public class Stepper_AdafruitMotorShieldGUI extends StepperControllerPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private GUIService myService;

	JLabel stepperPortLabel = new JLabel("stepper port");
	JComboBox stepperPort = new JComboBox();
	JButton attachButton = new JButton("attach");
	String arduinoName;
	String stepperName;

	ArrayList<Pin> pinList = null;

	public Stepper_AdafruitMotorShieldGUI(GUIService myService, String stepperName, String controllerName) {
		super();
		this.myService = myService;
		this.arduinoName = controllerName;
		this.stepperName = stepperName;

		for (int i = 1; i < 5; ++i) {
			stepperPort.addItem(String.format("m%d", i));
		}

		setBorder(BorderFactory.createTitledBorder("type - Adafruit Motor Shield"));
		add(stepperPortLabel);
		add(stepperPort);
		add(attachButton);
		setEnabled(true);

		attachButton.addActionListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == attachButton) {
			if ("attach".equals(attachButton.getText())) {
				Object[] stepperData = new Object[] { stepperPort.getSelectedItem() };
				myService.send(arduinoName, "stepperAttach", stepperName, stepperData);
				attachButton.setText("detach");
			} else {
				myService.send(arduinoName, "stepperDetach", stepperName);
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

	/**
	 * method to update the GUIService from StepperController data
	 */
	@Override
	public void setData(Object[] data) {
		// TODO Auto-generated method stub
		// stepperPort.setSelectedItem(data[0]);
	}

}
