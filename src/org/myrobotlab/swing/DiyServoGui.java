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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.DiyServo;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

/**
 * Servo SwingGui - displays details of Servo state Lesson learned ! Servos to
 * properly function need to be attached to a controller This gui previously
 * sent messages to the controller. To simplify things its important to send
 * messages only to the bound Servo - and let it attach to the controller versus
 * sending messages directly to the controller. 1 display - 1 service - keep it
 * simple
 *
 */
public class DiyServoGui extends ServiceGui implements ActionListener {

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

	public final static Logger log = LoggerFactory.getLogger(DiyServoGui.class);

	static final long serialVersionUID = 1L;
    
	/*
	final String attachMotorController = "attach motor controller";
	final String detachMotorController = "detach motor controller";
    */
	
	final String attachAnalog = "attach analog input";
	final String detachAnalog = "detach analog input";

	JLabel boundPos = new JLabel("90");
	// JButton attachButton = new JButton(attachMotorController);
	JButton attachListenerButton = new JButton(attachAnalog);

	JButton updateLimitsButton = new JButton("update limits");

	JSlider slider = new JSlider(0, 180, 90);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	// JComboBox<String> controllerList = new JComboBox<String>();
	JComboBox<String> pinArrayControlList = new JComboBox<String>();
	JComboBox<Integer> pinList = new JComboBox<Integer>();

	// JComboBox<Integer> pwmPinList = new JComboBox<Integer>();
	JComboBox<Integer> dirPinList = new JComboBox<Integer>();
	
	JTextField posMin = new JTextField("0");

	JTextField posMax = new JTextField("180");

	DiyServo myServo = null;

	SliderListener sliderListener = new SliderListener();

	public DiyServoGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);
		myServo = (DiyServo) Runtime.getService(boundServiceName);
		slider.addChangeListener(sliderListener);
		boundPos.setFont(boundPos.getFont().deriveFont(32.0f));
		
		JPanel s = new JPanel();
		s.add(left);
		s.add(slider);
		s.add(right);
		addTop(2, boundPos, 3, s);
		
    // addLine(left, slider, right, boundPos);
    // addLine(attachButton, controllerList, attachListenerButton, pinArrayControlList, "pin", pinList);
    // addLine(updateLimitsButton, "min", posMin, "max", posMax);
	//	addTop("Motor       :", controllerList,  " Pwm pin:", pwmPinList, " Dir pin:", dirPinList, attachButton);
		addTop("Analog input:", pinArrayControlList, " Analog input pin:", pinList, attachListenerButton);
		addTop("min:", posMin, "   max:", posMax, updateLimitsButton);

    updateLimitsButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);

    refreshControllers();
  
	}

	// SwingGui's action processing section - data from user
	@Override
	public void actionPerformed(final ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();

				if (o == pinArrayControlList) {
					String pinControlName = (String) pinArrayControlList.getSelectedItem();
					myServo.pinControlName = pinControlName;
					log.debug(String.format("pinArrayControList event %s", pinControlName));
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
	public void subscribeGui() {
	}

	@Override
	public void unsubscribeGui() {
	}

	synchronized public void onState(final DiyServo servo) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			  /* TODO GroG, Why this ? I removed it because it seems to always cause this to exit /Mats
			  boolean done = true;
			  if (done){
			    return;
			  }
			  */
				removeListeners();
				refreshControllers();
					
				/*
				if (servo.isControllerSet()) {
					attachButton.setText(detachMotorController);
					controllerList.setEnabled(false);
				} else {
					attachButton.setText(attachMotorController);
					controllerList.setEnabled(true);
				}
				*/

				if (servo.isPinArrayControlSet()) {
					attachListenerButton.setText(detachAnalog);
					pinArrayControlList.setEnabled(false);
					pinList.setEnabled(false);
				} else {
					attachListenerButton.setText(attachAnalog);
					pinArrayControlList.setEnabled(true);
					pinList.setEnabled(true);
				}

				/* TODO servo.getPos returns null in it's initial state causing Null pointer excepition, but I can't test for it since double is a primitive
					double pos = servo.getPos();
					boundPos.setText(Double.toString(pos));
					slider.setValue((int)pos);
				}
				*/

				slider.setMinimum((int)servo.getMin());
				slider.setMaximum((int)servo.getMax());

				posMin.setText(servo.getMin() + "");
				posMax.setText(servo.getMax() + "");

				restoreListeners();
			}
		});

	}


	public void refreshControllers() {

		// Refresh the list of Motors
        /*
		controllerList.removeAllItems();
		List<String> c = myServo.controllers;
		for (int i = 0; i < c.size(); ++i) {
			controllerList.addItem(c.get(i));
		}
		controllerList.setSelectedItem(myServo.motorControlName);
		*/
		
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

		// attachButton.removeActionListener(this);
		// controllerList.removeActionListener(this);

		attachListenerButton.removeActionListener(this);
		pinArrayControlList.removeActionListener(this);
		pinList.removeActionListener(this);

		slider.removeChangeListener(sliderListener);
	}

	public void restoreListeners() {

		// attachButton.addActionListener(this);
		// controllerList.addActionListener(this);

		attachListenerButton.addActionListener(this);
		pinArrayControlList.addActionListener(this);

		slider.addChangeListener(sliderListener);
	}

}