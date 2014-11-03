package org.myrobotlab.control.widget;

// ButtonsPanel.java
// Andrew Davison, October 2006, ad@fivedots.coe.psu.ac.th

/* A collection of Joystick.NUM_BUTTONS textfields 
 representing the buttons on the game pad, 
 divided into two rows

 When a button is pressed, the textfield's 
 background colour changes from OFF_COLOUR to ON_COLOUR
 */

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.myrobotlab.service.Joystick;

public class JoystickButtonsPanel extends JPanel {
	// background colours for the textfields (game pad buttons)
	private static final Color OFF_COLOUR = Color.LIGHT_GRAY;
	private static final Color ON_COLOUR = Color.YELLOW;

	private JTextField buttonTFs[]; // represents the game pad buttons

	/**
	 * Add the textfields to the panel and store references to them in a
	 * buttonTFs[] array.
	 * 
	 * Each textfield contains a number, is uneditable, and starts with an
	 * OFF_COLOUR background (meaning it's not pressed).
	 */
	public JoystickButtonsPanel() {
		setBackground(Color.white);

		// initialize buttonTFs[]
		buttonTFs = new JTextField[Joystick.NUM_BUTTONS];
		for (int i = 0; i < Joystick.NUM_BUTTONS; i++) {
			buttonTFs[i] = new JTextField("" + (i), 2);
			buttonTFs[i].setEditable(false);
			buttonTFs[i].setBackground(OFF_COLOUR);
		}

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // vertical box layout

		makeRow(0, Joystick.NUM_BUTTONS / 2); // 1st row
		makeRow(Joystick.NUM_BUTTONS / 2, Joystick.NUM_BUTTONS); // 2nd row
	} // end of ButtonsPanel()

	private void makeRow(int start, int end)
	// a row of textfields from buttonTFs[start] to buttonTFs[end-1]
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS)); // horiz
																		// box
																		// layout

		JPanel p;
		for (int i = start; i < end; i++) {
			p = new JPanel();
			p.setBackground(Color.white);
			p.add(buttonTFs[i]); // add button to its own panel p to stop
									// resizing
			rowPanel.add(p); // add panel p to row
		}
		add(rowPanel);
	} // end of makeRow()

	public void setButton(int buttonNum, Float value) {
		Color c = (value == Joystick.BUTTON_ON) ? ON_COLOUR : OFF_COLOUR;
		buttonTFs[buttonNum].setBackground(c);
		repaint();
	}

	public void setButtons(boolean bVals[])
	/*
	 * Use the bVals[] array to switch the buttonTFs on/off by changing the
	 * background colour of their textfields.
	 */
	{
		if (bVals.length != Joystick.NUM_BUTTONS)
			System.out.println("Wring number of button values");
		else {
			Color c;
			for (int i = 0; i < Joystick.NUM_BUTTONS; i++) {
				c = (bVals[i] == true) ? ON_COLOUR : OFF_COLOUR;
				buttonTFs[i].setBackground(c);
			}
			repaint();
		}
	} // end of setButtons()

} // end of ButtonsPanel class
