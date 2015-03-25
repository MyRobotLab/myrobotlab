package org.myrobotlab.control.widget;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class DigitalButton extends JButton {

	private static final long serialVersionUID = 1L;
	// public int ID = -1;
	public final Object parent;
	public String offText = null;
	public String onText = null;
	String offCMD = "off";
	String onCMD = "on";

	public Color offBGColor = null;
	Color offFGColor = null;
	Color onBGColor = null;
	Color onFGColor = null;

	Icon onIcon = null;
	Icon offIcon = null;

	public int type = -1;
	public boolean isOn = false;

	public DigitalButton(Object parent, String offText, Color offBGColor, Color offFGColor, String onText, Color onBGColor, Color onFGColor, int type) {
		this(parent, offText, offText, null, offBGColor, offFGColor, onText, onText, null, onBGColor, onFGColor, type);
	}

	public DigitalButton(Object parent, String offCMD, ImageIcon offIcon, String onCMD, ImageIcon onIcon, int type) {
		this(parent, null, offCMD, offIcon, null, null, null, onCMD, onIcon, null, null, type);
	}

	public DigitalButton(Object parent, String offText, String offCMD, Color offBGColor, Color offFGColor, String onText, String onCMD, Color onBGColor, Color onFGColor, int type) {
		this(parent, offText, offCMD, null, offBGColor, offFGColor, onText, onCMD, null, onBGColor, onFGColor, type);
	}

	public DigitalButton(Object parent, String offText, String offCMD, Icon offIcon, Color offBGColor, Color offFGColor, String onText, String onCMD, Icon onIcon, Color onBGColor,
			Color onFGColor, int type) {
		super(offText);

		this.parent = parent;
		this.type = type;
		this.onText = onText;
		this.offText = offText;
		this.offBGColor = offBGColor;
		this.offFGColor = offFGColor;
		this.onBGColor = onBGColor;
		this.onFGColor = onFGColor;
		this.onIcon = onIcon;
		this.offIcon = offIcon;
		if (offIcon != null) {
			setIcon(offIcon);
		}

		// setPreferredSize(new Dimension(35,15));

		setBackground(offBGColor);
		setForeground(offFGColor);
		// setOpaque(false);
		setBorder(null);
		setOpaque(true);
		setBorderPainted(false);
		// setContentAreaFilled(false);
		// setIcon(this.offIcon);
	}

	public boolean isOn() {
		return isOn;
	}

	public void setOff() {
		if (offIcon != null) {
			setIcon(offIcon);
		} else {
			setBackground(offBGColor);
			setForeground(offFGColor);
			setText(offText);
		}
		setActionCommand(offCMD);
		isOn = false;
	}

	public void setOn() {
		if (onIcon != null) {
			setIcon(onIcon);
		} else {
			setBackground(onBGColor);
			setForeground(onFGColor);
			setText(onText);
		}
		setActionCommand(onCMD);
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
