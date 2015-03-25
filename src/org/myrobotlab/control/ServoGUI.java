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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
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
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.ServoController;
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
public class ServoGUI extends ServiceGUI implements ActionListener, MouseListener {

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

	public final static Logger log = LoggerFactory.getLogger(ServoGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	JLabel boundPos = null;
	JButton attachButton = new JButton("attach");

	JButton updateLimitsButton = new JButton("update limits");

	JSlider slider = new JSlider(0, 180, 90);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);
	JComboBox<String> controller = new JComboBox<String>();

	JComboBox<Integer> pin = new JComboBox<Integer>();
	DefaultComboBoxModel<String> controllerModel = new DefaultComboBoxModel<String>();

	DefaultComboBoxModel<Integer> pinModel = new DefaultComboBoxModel<Integer>();
	JTextField posMin = new JTextField("0");

	JTextField posMax = new JTextField("180");

	Servo myServo = null;

	SliderListener sliderListener = new SliderListener();

	public ServoGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myServo = (Servo) Runtime.getService(boundServiceName);

		pinModel.addElement(null);
		for (int i = 2; i < 54; i++) {
			pinModel.addElement(i);
		}
		// determine not worth querying the controller to its pin list
	}

	// GUIService's action processing section - data from user
	@Override
	public void actionPerformed(final ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();
				if (o == controller) {
					String controllerName = (String) controller.getSelectedItem();
					log.info(String.format("controller event %s", controllerName));
					if (controllerName != null && controllerName.length() > 0) {

						// NOT WORTH IT - JUST BUILD 48 PINS !!!
						// ServoController sc = (ServoController)
						// Runtime.getService(controllerName);

						// NOT WORTH THE TROUBLE !!!!
						// @SuppressWarnings("unchecked")
						// ArrayList<Pin> pinList = (ArrayList<Pin>)
						// myService.sendBlocking(controllerName, "getPinList");
						// log.info("{}", pinList.size());

						// FIXME - get Local services relative to the servo
						// pinModel.removeAllElements();
						// pinModel.addElement(null);

						// for (int i = 0; i < pinList.size(); ++i) {
						// pinModel.addElement(pinList.get(i).pin);
						// }

						// pin.invalidate();

					}
				}

				if (o == attachButton) {
					if (attachButton.getText().equals("attach")) {
						send("attach", controller.getSelectedItem(), pin.getSelectedItem());
					} else {
						send("detach");
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
		subscribe("publishState", "getState", Servo.class);
		// subscribe("controllerSet", "controllerSet");
		// subscribe("pinSet", "pinSet");
		// subscribe("attached", "attached");
		// subscribe("detached", "detached");
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Servo.class);
		// unsubscribe("controllerSet", "controllerSet");
		// unsubscribe("pinSet", "pinSet");
		// subscribe("attached", "attached");
		// subscribe("detached", "detached");
	}

	synchronized public void getState(final Servo servo) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				removeListeners();
				controller.setSelectedItem(servo.getControllerName());
				pin.setSelectedItem(servo.getPin());

				if (servo.isAttached()) {
					attachButton.setText("detach");
					controller.setEnabled(false);
					pin.setEnabled(false);
				} else {
					attachButton.setText("attach");
					controller.setEnabled(true);
					pin.setEnabled(true);
				}

				if (servo.getPosFloat() == null) {
					boundPos.setText("");
				} else {
					boundPos.setText(servo.getPosFloat().toString());
					slider.setValue(Math.round(servo.getPosFloat()));
				}

				slider.setMinimum((int) servo.getMinInput());
				slider.setMaximum((int) servo.getMaxInput());

				posMin.setText(servo.getMin().toString());
				posMax.setText(servo.getMax().toString());

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

		control.add(controller, gc);

		++gc.gridx;
		control.add(new JLabel("pin"), gc);

		++gc.gridx;
		control.add(pin, gc);

		display.add(control);
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
		controller.addActionListener(this);
		attachButton.addActionListener(this);
		pin.addActionListener(this);

		// http://stackoverflow.com/questions/6205433/jcombobox-focus-and-mouse-click-events-not-working
		// jComboBox1.getEditor().getEditorComponent().addMouseListener(...);
		// have to add mouse listener to the MetalComboButton embedded in the
		// JComboBox
		Component[] comps = controller.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].addMouseListener(this); // JComboBox composite listener -
												// have to get all the sub
												// components
			/*
			 * comps[i].addMouseListener(new MouseAdapter() { public void
			 * mouseClicked(MouseEvent me) { System.out.println("clicked"); }
			 * });
			 */
		}
		// controller.getEditor().getEditorComponent().addMouseListener(this);
		controller.setModel(controllerModel);
		pin.setModel(pinModel);

		refreshControllers();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("clicked");

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("entered");

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

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("exited");

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("controller pressed");
		refreshControllers();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("released");
	}

	public void refreshControllers() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// FIXME - would newing? a new DefaultComboBoxModel be better?
				controllerModel.removeAllElements();
				// FIXME - get Local services relative to the servo
				controllerModel.addElement("");
				ArrayList<String> v = Runtime.getServiceNamesFromInterface(ServoController.class);
				for (int i = 0; i < v.size(); ++i) {
					controllerModel.addElement(v.get(i));
				}
				controller.invalidate();
				// if isAttached() - select the correct one
			}
		});
	}

	public void removeListeners() {
		controller.removeActionListener(this);
		pin.removeActionListener(this);
		slider.removeChangeListener(sliderListener);
	}

	public void restoreListeners() {
		controller.addActionListener(this);
		pin.addActionListener(this);
		slider.addChangeListener(sliderListener);
	}

}