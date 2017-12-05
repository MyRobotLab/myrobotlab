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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
	
	final String attachAnalog = "attach analog input";
	final String detachAnalog = "detach analog input";

	JLabel boundPos = new JLabel("90");
	JButton attachListenerButton = new JButton(attachAnalog);



	JSlider slider = new JSlider(0, 180, 90);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	JComboBox<String> pinArrayControlList = new JComboBox<String>();
	JComboBox<Integer> analogInputPinList = new JComboBox<Integer>();
	
  JTextField minInput = new JTextField("0");
  JTextField maxInput = new JTextField("180");
  JTextField minOutput = new JTextField("0");
  JTextField maxOutput = new JTextField("180");
  JTextField velocity = new JTextField("-1");
  JButton setVelocity = new JButton("Velocity");
  JButton updateMapButton = new JButton("set");

	DiyServo myServo = null;

	SliderListener sliderListener = new SliderListener();

	public DiyServoGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);
		myServo = (DiyServo) Runtime.getService(boundServiceName);
		slider.addChangeListener(sliderListener);
		boundPos.setFont(boundPos.getFont().deriveFont(32.0f));
		velocity.setPreferredSize(new Dimension( 50, 24 ));
		setVelocity.addActionListener(this);
		updateMapButton.addActionListener(this);
		JPanel s = new JPanel();
		s.add(left);
		s.add(slider);
		s.add(right);
    addTopLeft(2, boundPos, 3, s,velocity,setVelocity );
		
    JPanel controllerP = new JPanel();
    Border borderController = BorderFactory.createTitledBorder("Controller");
    controllerP.setBorder(borderController);
    JLabel pinArrayControlListlabel = new JLabel("Analog input : ");
    JLabel analogInputPinListabel = new JLabel("Analog input pin : ");
    
    controllerP.add(pinArrayControlListlabel);
    controllerP.add(pinArrayControlList);
    controllerP.add(analogInputPinListabel);
    controllerP.add(analogInputPinList);
    controllerP.add(attachListenerButton);

    JPanel map = new JPanel();
    Border bordermap = BorderFactory.createTitledBorder("map(minInput, maxInput, minOutput, maxOutput)");
    map.setBorder(bordermap);
    map.add(minInput);
    map.add(maxInput);
    map.add(minOutput);
    map.add(maxOutput);
    map.add(updateMapButton);
    
    addTopLeft(" ");
    addTopLeft(controllerP);
    addTopLeft(" ");
    addTopLeft(map);

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
					refreshAnalogPinList();	
					log.debug(String.format("pinArrayControList event %s", pinControlName));
				}

				if (o == attachListenerButton) {
					if (attachListenerButton.getText().equals(attachAnalog)) {
						send("attach", pinArrayControlList.getSelectedItem(), analogInputPinList.getSelectedItem());
					} else {
						send("detach", pinArrayControlList.getSelectedItem());
					}
					return;
				}

				if (o == updateMapButton) {
				  send("map", Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()),Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
					return;
				}

				if (o == right) {
					slider.setValue(slider.getValue() + 1);
					return;
				}
				
        if (o == setVelocity) {
          send("setVelocity", Double.parseDouble(velocity.getText()));
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

				if (servo.isPinArrayControlSet()) {
					attachListenerButton.setText(detachAnalog);
	        attachListenerButton.setEnabled(true);
					pinArrayControlList.setEnabled(false);
					analogInputPinList.setEnabled(false);
				} else {
					attachListenerButton.setText(attachAnalog);
					pinArrayControlList.setEnabled(true);
					analogInputPinList.setEnabled(true);
					if ((pinArrayControlList.getSelectedItem() != null) && (analogInputPinList.getSelectedItem()) != null){
	          attachListenerButton.setEnabled(true);					  
					}
					else
	           attachListenerButton.setEnabled(false);      
				}
				

				/* TODO servo.getPos returns null in it's initial state causing Null pointer excepition, but I can't test for it since double is a primitive
					double pos = servo.getPos();
					boundPos.setText(Double.toString(pos));
					slider.setValue((int)pos);
				}
				*/

				slider.setMinimum((int)servo.getMin());
				slider.setMaximum((int)servo.getMax());
				
        velocity.setText(servo.getVelocity() + "");

        minInput.setText(servo.getMinInput() + "");
        maxInput.setText(servo.getMaxInput() + "");
        minOutput.setText(servo.getMinOutput() + "");
        maxOutput.setText(servo.getMaxOutput() + "");

				restoreListeners();
			}
		});

	}

	public void refreshControllers() {
		
		// Refresh the list of Analog inputs
		pinArrayControlList.removeAllItems();
		List<String> a = myServo.pinArrayControls;
		for (int i = 0; i < a.size(); ++i) {
			pinArrayControlList.addItem(a.get(i));
		}
		pinArrayControlList.setSelectedItem(myServo.pinControlName);

		// Refresh the list of Pins inputs
		refreshAnalogPinList();	
		
		restoreListeners();
	}
    
	public void refreshAnalogPinList() {
		
		// Refresh the list of Pins inputs
		analogInputPinList.removeAllItems();
		if (myServo.pinControlName != null) {
			PinArrayControl tmpControl = (PinArrayControl) Runtime.getService(myServo.pinControlName);
			if (tmpControl != null) {
				List<PinDefinition> mbl = tmpControl.getPinList();
				for (int i = 0; i < mbl.size(); i++) {
					PinDefinition pinData = mbl.get(i);
					// Removed the filtering on pins, because the Arduino logic for the different is not complete
					// if (pinData.isAnalog()){
					    analogInputPinList.addItem(pinData.getAddress());
					// }
				}
			}
			analogInputPinList.setSelectedItem(myServo.pin);
		}		
	}
    
	public void removeListeners() {

		attachListenerButton.removeActionListener(this);
		pinArrayControlList.removeActionListener(this);

		slider.removeChangeListener(sliderListener);
	}

	public void restoreListeners() {

		attachListenerButton.addActionListener(this);
		pinArrayControlList.addActionListener(this);

		slider.addChangeListener(sliderListener);
	}

}