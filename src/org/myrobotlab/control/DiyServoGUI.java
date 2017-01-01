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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.DiyServo;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.slf4j.Logger;

/**
 * Servo GUIService - displays details of Servo state Lesson learned ! Servos to
 * properly function need to be attached to a controller This gui previously
 * sent messages to the controller. To simplify things its important to send
 * messages only to the bound Servo - and let it attach to the controller versus
 * sending messages directly to the controller. 1 display - 1 service - keep it
 * simple
 *
 */
public class DiyServoGUI extends ServiceGUI implements ActionListener {

	private class SliderListener implements ChangeListener {
		@Override
		public void stateChanged(javax.swing.event.ChangeEvent e) {

			boundPos.setText(String.format("%d", slider.getValue()));

			if (myService != null) {
				myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
			} else {
				log.error("can not send message myService is null");
			}
		}
	}

	public final static Logger log = LoggerFactory.getLogger(DiyServoGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	final String attachMotorController = "attach motor controller";
	final String detachMotorController = "detach motor controller";

	final String attachAnalog = "attach analog input";
	final String detachAnalog = "detach analog input";

	JLabel boundPos = null;
	JButton attachButton = new JButton(attachMotorController);
	JButton attachListenerButton = new JButton(attachAnalog);

	JButton updateLimitsButton = new JButton("update limits");

	JSlider slider = new JSlider(0, 180, 90);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	JComboBox<String> controllerList = new JComboBox<String>();
	JComboBox<String> pinArrayControlList = new JComboBox<String>();
	JComboBox<Integer> pinList = new JComboBox<Integer>();

	JTextField posMin = new JTextField("0");

	JTextField posMax = new JTextField("180");

	DiyServo myServo = null;

	SliderListener sliderListener = new SliderListener();

	public DiyServoGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myServo = (DiyServo) Runtime.getService(boundServiceName);

		// determine not worth querying the controller to its pin list
	}

	// GUIService's action processing section - data from user
	@Override
	public void actionPerformed(final ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();
				if (o == controllerList) {
					String controllerName = (String) controllerList.getSelectedItem();
					myServo.controllerName = controllerName;
					log.debug(String.format("controllerList event %s", controllerName));
				}

				if (o == pinArrayControlList) {
					String pinControlName = (String) pinArrayControlList.getSelectedItem();
					myServo.pinControlName = pinControlName;
					log.debug(String.format("pinArrayControList event %s", pinControlName));
				}

				if (o == attachButton) {
					log.info("attachButton pressed");
					if (attachButton.getText().equals(attachMotorController)) {
						send("attach", controllerList.getSelectedItem());
					} else {
						send("detach", controllerList.getSelectedItem());
					}
					return;
				}

				if (o == attachListenerButton) {
					log.info("attachListnerButton pressed");
					if (attachListenerButton.getText().equals(attachAnalog)) {
						send("attach", pinArrayControlList.getSelectedItem(), pinList.getSelectedItem());
					} else {
						send("detach", pinArrayControlList.getSelectedItem());
					}
					return;
				}

				if (o == updateLimitsButton) {
					send("setMinMax", Integer.parseInt(posMin.getText()), Integer.parseInt(posMax.getText()));
					return;
				}

				if (o == right) {
					slider.setValue(slider.getValue() + 1);
					return;
				}

				if (o == left) {
					slider.setValue(slider.getValue() - 1);
					return;
				}

			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", DiyServo.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", DiyServo.class);

	}

	synchronized public void getState(final DiyServo servo) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				removeListeners();
				refreshControllers();
				ServoControl sc = (ServoControl) myServo;

				if (sc != null) {
					controllerList.setSelectedItem(sc.getName());

					Integer servoPin = servo.getPin();

					if (servoPin != null)
						pinList.setSelectedItem(servoPin);
				}

				if (servo.isControllerSet()) {
					attachButton.setText(detachMotorController);
					controllerList.setEnabled(false);
				} else {
					attachButton.setText(attachMotorController);
					controllerList.setEnabled(true);
				}

				if (servo.isPinArrayControlSet()) {
					attachListenerButton.setText(detachAnalog);
					pinArrayControlList.setEnabled(false);
					pinList.setEnabled(false);
				} else {
					attachListenerButton.setText(attachAnalog);
					pinArrayControlList.setEnabled(true);
					pinList.setEnabled(true);
				}

				
					double pos = servo.getPos();
					boundPos.setText(Double.toString(pos));
					slider.setValue((int)pos);
				

				slider.setMinimum((int)servo.getMin());
				slider.setMaximum((int)servo.getMax());

				posMin.setText(servo.getMin() + "");
				posMax.setText(servo.getMax() + "");

				restoreListeners();
			}
		});

	}

	@Override
	public void init() {

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		input.add(slider, gc);
		slider.addChangeListener(sliderListener);

		gc.gridwidth = 2;
		gc.gridx = 1;
		++gc.gridy;
		input.add(left, gc);
		++gc.gridx;

		input.add(right, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;

		JPanel control = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		control.add(attachButton, gc);
		++gc.gridx;

		control.add(controllerList, gc);

		JPanel control2 = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;
		// control2.add(new JLabel("TEST"));

		gc.gridx = 0;
		++gc.gridy;
		control2.add(attachListenerButton);

		++gc.gridx;
		control2.add(pinArrayControlList);

		++gc.gridx;
		control2.add(new JLabel("pin"), gc);

		++gc.gridx;
		control2.add(pinList, gc);

		display.add(control);
		display.add(control2);
		display.add(input);

		gc.gridx = 0;
		++gc.gridy;

		JPanel limits = new JPanel();
		limits.add(updateLimitsButton);
		limits.add(new JLabel("min "));
		limits.add(posMin);
		limits.add(new JLabel(" max "));
		limits.add(posMax);

		limits.add(new JLabel(" "));
		boundPos = new JLabel("90");

		limits.add(boundPos);

		display.add(limits, gc);

		updateLimitsButton.addActionListener(this);
		left.addActionListener(this);
		right.addActionListener(this);

		refreshControllers();
	}

	// a controller has been set
	/*
	 * public void displayController(final ServoController sc, final ServoGUI
	 * mygui) { SwingUtilities.invokeLater(new Runnable() { public void run() {
	 * controller.removeActionListener(mygui); pinModel.removeAllElements(); //
	 * FIXME - get Local services relative to the servo
	 * pinModel.addElement(null);
	 * 
	 * ArrayList<Pin> pinList = sc.getPinList(); for (int i = 0; i <
	 * pinList.size(); ++i) { pinModel.addElement(pinList.get(i).pin); }
	 * 
	 * pin.invalidate(); } });
	 * 
	 * }
	 */

	public void refreshControllers() {

		// Refresh the list of Motors
		controllerList.removeAllItems();
		List<String> c = myServo.controllers;
		for (int i = 0; i < c.size(); ++i) {
			controllerList.addItem(c.get(i));
		}
		controllerList.setSelectedItem(myServo.controllerName);

		// Refresh the list of Analog inputs
		pinArrayControlList.removeAllItems();
		List<String> a = myServo.pinArrayControls;
		for (int i = 0; i < a.size(); ++i) {
			pinArrayControlList.addItem(a.get(i));
		}
		pinArrayControlList.setSelectedItem(myServo.pinControlName);

		// Refresh the list of Pins inputs
		pinList.removeAllItems();
		if (myServo.pinControlName != null) {
			PinArrayControl tmpControl = (PinArrayControl) Runtime.getService(myServo.pinControlName);
			if (tmpControl != null) {
				List<PinDefinition> mbl = tmpControl.getPinList();
				for (int i = 0; i < mbl.size(); i++) {
					PinDefinition pinData = mbl.get(i);
					pinList.addItem(pinData.getAddress());
				}
			}
			pinList.setSelectedItem(myServo.pin);
		}
		restoreListeners();
	}

	public void removeListeners() {

		attachButton.removeActionListener(this);
		controllerList.removeActionListener(this);

		attachListenerButton.removeActionListener(this);
		pinArrayControlList.removeActionListener(this);
		pinList.removeActionListener(this);

		slider.removeChangeListener(sliderListener);
	}

	public void restoreListeners() {

		attachButton.addActionListener(this);
		controllerList.addActionListener(this);

		attachListenerButton.addActionListener(this);
		pinArrayControlList.addActionListener(this);

		slider.addChangeListener(sliderListener);
	}

}