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
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.control.widget.ImageButton;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.ServiceInterface;

public class MotorGUI extends ServiceGUI implements ActionListener, ChangeListener {

	public class FloatJSlider extends JSlider {

		private static final long serialVersionUID = 1L;
		final int scale;

		public FloatJSlider(int min, int max, int value, int scale) {
			super(min, max, value);
			this.scale = scale;

			Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			labelTable.put(new Integer(min), new JLabel(String.format("%.2f", (float) min / scale)));
			labelTable.put(new Integer(min / 2), new JLabel(String.format("%.2f", (float) min / scale / 2)));
			labelTable.put(new Integer(value), new JLabel(String.format("%.2f", (float) value / scale)));
			labelTable.put(new Integer(max / 2), new JLabel(String.format("%.2f", (float) max / scale / 2)));
			labelTable.put(new Integer(max), new JLabel(String.format("%.2f", (float) max / scale)));
			setLabelTable(labelTable);
			setPaintTrack(false);
		}

		public float getScaledValue() {
			return ((float) super.getValue()) / this.scale;
		}
	}

	// controller
	JPanel controllerPanel = new JPanel(new BorderLayout());
	MotorControllerPanel controllerTypePanel = new Motor_UnknownGUI();
	JComboBox<String> controllerSelect = new JComboBox<String>();
	MotorController controller = null;

	JCheckBox invert = new JCheckBox("invert");
	// power
	JPanel powerPanel = new JPanel(new BorderLayout());
	private FloatJSlider power = null;
	private JLabel powerValue = new JLabel("0.00");
	ImageButton stopButton;
	ImageButton clockwiseButton;

	ImageButton counterclockwiseButton;
	// position
	JPanel positionPanel = null;

	// private JLabel posValue = new JLabel("0.00");
	JLabel currentPosition = new JLabel("0");

	// TODO - make MotorPanel - for 1 motor - for shared embedded widget
	// TODO - stop sign button for panic stop
	// TODO - tighten up interfaces
	// TODO - DIRECT calls ! - motor & controller HAVE to be on the same
	// computer
	// TODO - cw ccw buttons enabled

	Motor myMotor;

	// FIXME put in sub gui
	ArrayList<Pin> pinList = null;

	public MotorGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == controllerSelect) {

			String newController = (String) controllerSelect.getSelectedItem();

			if (newController != null && newController.length() > 0) {
				// myService.send(boundServiceName, "setPort", newPort);
				ServiceInterface sw = Runtime.getService(newController);
				controller = (MotorController) Runtime.getService(newController);

				String type = sw.getSimpleName();

				// build gui for appropriate motor controller type -
				// the gui needs to be able to do a Motor.attach(name, data)
				// with appropriate data
				String attachGUIName = String.format("org.myrobotlab.control.Motor_%sGUI", type);

				controllerPanel.remove(controllerTypePanel);
				controllerTypePanel = Reflector.getNewInstance(attachGUIName, new Object[] { myService, boundServiceName, newController });
				controllerPanel.add(controllerTypePanel, BorderLayout.CENTER);
				// setEnabled(true);

			} else {
				controllerPanel.remove(controllerTypePanel);
				controllerTypePanel = new Motor_UnknownGUI();
				controllerPanel.add(controllerTypePanel, BorderLayout.CENTER);
				// setEnabled(false);
			}

			controllerTypePanel.setBorder(BorderFactory.createTitledBorder("type"));
			controllerPanel.revalidate();

		} else if (source == stopButton) {
			power.setValue(0);
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Motor.class);
		subscribe("publishChangePos", "onChangePos", Double.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Arduino.class);
		unsubscribe("publishChangePos", "onChangePos", Double.class);
	}

	public void getState(Motor motor) {
		myMotor = motor;
		setEnabled(motor.isAttached());
		// FIXED - can't use a reference - because it changes mid-stream through
		// this method
		// MotorControllerPanel subpanel =
		// ((MotorControllerPanel)controllerTypePanel);
		if (motor.isAttached()) {
			// !!!!! - This actually fires the (makes a new
			// MotorControllerPanel) !!!!!
			controllerSelect.setSelectedItem(motor.getControllerName());
			controllerTypePanel.set(motor);
		}
		controllerTypePanel.setAttached(motor.isAttached());
		if (motor.isInverted()) {
			invert.setSelected(true);
		} else {
			invert.setSelected(false);
		}
	}

	@Override
	public void init() {
		// controllerPanel begin ------------------

		controllerPanel.setBorder(BorderFactory.createTitledBorder("controller"));

		ArrayList<String> v = Runtime.getServiceNamesFromInterface(MotorController.class);
		v.add(0, "");
		controllerSelect = new JComboBox(v.toArray());
		controllerPanel.add(controllerSelect, BorderLayout.WEST);
		controllerPanel.add(controllerTypePanel, BorderLayout.CENTER);

		// controllerPanel end ------------------

		// powerPanel begin ------------------
		powerPanel.setBorder(BorderFactory.createTitledBorder("power"));

		JPanel north = new JPanel();
		north.add(invert);
		north.add(powerValue);
		powerPanel.add(north, BorderLayout.NORTH);

		counterclockwiseButton = new ImageButton("Motor", "counterclockwise", this);
		stopButton = new ImageButton("Motor", "stop", this);
		clockwiseButton = new ImageButton("Motor", "clockwise", this);
		powerPanel.add(counterclockwiseButton, BorderLayout.WEST);
		powerPanel.add(stopButton, BorderLayout.CENTER);
		powerPanel.add(clockwiseButton, BorderLayout.EAST);

		power = new FloatJSlider(-100, 100, 0, 100);
		power.setMajorTickSpacing(25);
		// power.setMinorTickSpacing(10);
		power.setPaintTicks(true);
		power.setPaintLabels(true);
		powerPanel.add(power, BorderLayout.SOUTH);
		// powerValue.setPreferredSize(new Dimension(100,50));
		// powerPanel end ------------------

		// positionPanel begin ------------------
		positionPanel = new JPanel();
		positionPanel.setBorder(BorderFactory.createTitledBorder("position"));
		positionPanel.add(new JLabel("current position "));
		positionPanel.add(currentPosition);

		// positionPanel end ------------------

		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		display.add(controllerPanel, gc);
		++gc.gridy;
		display.add(powerPanel, gc);
		++gc.gridy;
		display.add(positionPanel, gc);

		controllerSelect.addActionListener(this);
		power.addChangeListener(this);

		// TODO - motor could come into graphics already attached - handle it...

	}

	public void onChangePos(Double newPos) {
		currentPosition.setText(String.format("%3.2f", newPos));
	}

	public void setEnabled(boolean enable) {
		stopButton.setEnabled(enable);
		clockwiseButton.setEnabled(enable);
		counterclockwiseButton.setEnabled(enable);
		power.setEnabled(enable);
		invert.setEnabled(enable);
		powerValue.setEnabled(enable);

	}

	@Override
	public void stateChanged(ChangeEvent ce) {
		Object source = ce.getSource();
		if (power == source) {
			// powerValue.setText(power.getValue() + "%");
			powerValue.setText(String.format("in %3.2f out %3.0f", power.getScaledValue(), myMotor.getPowerMap().calc(power.getScaledValue())));
			myService.send(boundServiceName, "move", power.getScaledValue());
		}
	}

}
