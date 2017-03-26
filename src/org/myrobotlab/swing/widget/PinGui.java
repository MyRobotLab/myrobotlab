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

package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.interfaces.DisplayProvider;
import org.slf4j.Logger;

/**
 * graphical representation of a pin
 * with button capability
 * 
 * it contains a PinDefintion and offers controls on how to display
 * 
 * it is updated by PinData
 * 
 * it's display is retrieved through getDisplay()
 * 
 * @author GroG
 *
 */
public class PinGui implements DisplayProvider, ActionListener {
	public final static Logger log = LoggerFactory.getLogger(PinGui.class);

	PinDefinition pinDef;
	String boundServiceName;
	JSlider slider = null;
	boolean isVertical = false;
	JButton stateButton = new JButton();
	
	/**
	 * for lack of better idea and need the ability to support
	 * multiple states
	 */
	String state = "off"; 
	
	GridBagConstraints gc = new GridBagConstraints();
	
	JPanel display = new JPanel(new GridBagLayout());
	
	Color offFgColor = Color.WHITE;
	Color offBgColor = Color.LIGHT_GRAY;
	
	Color onFgColor = Color.BLACK;
	Color onBgColor = Color.GREEN;
	
	Rectangle size = new Rectangle(15, 15);
	
	ActionListener relay;	

	public PinGui(final PinDefinition pinDef) {
		this.pinDef = pinDef;
		this.isVertical = isVertical;

		//////////// button begin /////////////////
		
		stateButton.setBackground(offBgColor);
		stateButton.setForeground(offFgColor);
		stateButton.setBorder(null);
		stateButton.setOpaque(true);
		stateButton.setBorderPainted(false);
		stateButton.setBounds(size);
		// onOff.setText(pinDef.getName());
		// stateButton.setText("ab");
		stateButton.addActionListener(this);
		
		//////////// button end /////////////////		
		
		if (pinDef.isPwm()){
			int orientation = (isVertical) ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL;
			slider = new JSlider(orientation, 0, 255, 0);
			slider.setOpaque(false);
			slider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					// data.setText("" + slider.getValue());
					
					// 	myService.send(boundServiceName, "analogWrite", pinNumber, slider.getValue());
					
				}
			});
		}
		
		gc.weightx = gc.weighty = 1.0;
		gc.gridx = 0;
		gc.gridy = 0;
		stateButton.setPreferredSize(new Dimension(15,15));
		display.add(stateButton, gc);

	}
	
	public void update(PinData pinData){
		if (pinData.value > 0){
			// setOn();
		}
	}

	@Override
	public Component getDisplay() {
		return display;
	}

	public void addActionListener(ActionListener relay) {
		this.relay = relay;
	}
	
	public void setLocation(int x, int y){
		display.setLocation(x, y);
	}
	
	public void setBounds(int x, int y, int width, int height){
		display.setBounds(x, y, width, height);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("actionPerformed");
		Object o = e.getSource();
		// FIXME - state update can happen from 'user' or from 'board'
		/* 
		if (pinDef.getValue() > 0){
			
		}
		*/
		// simple 2 state at the moment ...
		if (o == stateButton){
			if ("off".equals(state)){
				setState("on");
			} else if ("on".equals(state)) {
				setState("off");
			}
		}
		e.setSource(this);
		relay.actionPerformed(e);
	}

	public void setState(String state) {
		this.state = state;
		if ("on".equals(state)){
			stateButton.setBackground(onBgColor);
			stateButton.setForeground(onFgColor);
		} else if ("off".equals(state)) {
			stateButton.setBackground(offBgColor);
			stateButton.setForeground(offFgColor);
		}
	}

	public PinDefinition getPinDef() {
		return pinDef;
	}

	public String getState() {
		return state;
	}

}