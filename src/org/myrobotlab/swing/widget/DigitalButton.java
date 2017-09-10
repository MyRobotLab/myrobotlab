package org.myrobotlab.swing.widget;

import java.awt.Color;

import javax.swing.JButton;

public class DigitalButton extends JButton {

	private static final long serialVersionUID = 1L;

	String onText = null;
	String offText = null;

	Color offFgColor = null;
	Color offBgColor = null;
	
	Color onFgColor = null;
	Color onBgColor = null;

	public boolean isOn = false;

	public DigitalButton() {
		// super(offText);
		setBackground(offBgColor);
		setForeground(offFgColor);
		setBorder(null);
		setOpaque(true);
		setBorderPainted(false);
		
	}

	public boolean isOn() {
		return isOn;
	}

	public void setOff() {
		setBackground(offBgColor);
		setForeground(offFgColor);
		setText(offText);
		// setActionCommand(offCMD);
		isOn = false;
	}

	public void setOn() {
		setBackground(onBgColor);
		setForeground(onFgColor);
		setText(onText);
		// setActionCommand(onCMD);
		isOn = true;
	}

	public void toggle() {
		if (isOn) {
			setOff();
		} else {
			setOn();
		}
	}

}
