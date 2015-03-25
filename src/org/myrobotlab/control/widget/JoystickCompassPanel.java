package org.myrobotlab.control.widget;

// CompassPanel.java
// Andrew Davison, October 2006, ad@fivedots.coe.psu.ac.th

/* A canvas which draws a circle in the current compass position for
 the analog stick / hat (and a label as background).
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class JoystickCompassPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int PANEL_SIZE = 80;
	private static final int CIRCLE_RADIUS = 5;

	private int x, y;
	private JLabel XLabel = new JLabel();
	private JLabel YLabel = new JLabel();
	private JLabel screen = new JLabel();

	public JoystickCompassPanel(String label) {
		setLayout(new BorderLayout());
		setBackground(Style.listHighlight);
		screen.setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));
		add(screen, BorderLayout.CENTER);

		JPanel info = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;

		gc.gridwidth = 2;
		info.add(new JLabel(label), gc);

		gc.gridwidth = 1;
		++gc.gridy;
		info.add(new JLabel("X:"), gc);
		++gc.gridx;
		XLabel.setText((new Float(0.0)).toString());
		info.add(XLabel, gc);

		gc.gridx = 0;
		++gc.gridy;
		info.add(new JLabel("Y:"), gc);
		++gc.gridx;
		YLabel.setText((new Float(0.0)).toString());
		info.add(YLabel, gc);

		add(info, BorderLayout.PAGE_END);

	} // end of CompassPanel()

	@Override
	public void paintComponent(Graphics g)
	// draw the current compass position as a black circle
	{
		super.paintComponent(g);

		g.drawRect(1, 1, PANEL_SIZE - 2, PANEL_SIZE - 2); // a black border
		g.drawLine(x - 6, y, x + 6, y);
		g.drawLine(x, y - 6, x, y + 6);

	} // end of paintComponent()

	public void setDir(Float value) {
		int MARKER = 10;

		if (value == 0) {
			// 0 position
			x = PANEL_SIZE / 2;
			y = PANEL_SIZE / 2;
		} else if (value == 0.25) {
			// NORTH
			x = PANEL_SIZE / 2;
			y = MARKER;
		} else if (value == 0.375) { // NE
			x = PANEL_SIZE - MARKER;
			y = MARKER;
		} else if (value == 0.5) { // E
			x = PANEL_SIZE - MARKER;
			y = PANEL_SIZE / 2;
		} else if (value == 0.625) { // SE
			x = PANEL_SIZE - MARKER;
			y = PANEL_SIZE - MARKER;
		} else if (value == 0.75) { // S
			x = PANEL_SIZE / 2;
			y = PANEL_SIZE - MARKER;
		} else if (value == 0.875) { // SE
			x = 0 + MARKER;
			y = PANEL_SIZE - MARKER;
		} else if (value == 1.0) { // E
			x = 0 + MARKER;
			y = PANEL_SIZE / 2;
		} else if (value == 0.125) { // NE
			x = 0 + MARKER;
			y = 0 + MARKER;
		}
	}

	public void setX(Float value) {
		x = (int) (PANEL_SIZE / 2 * value + PANEL_SIZE / 2);
		XLabel.setText(String.format("%.3f", value));
	}

	public void setY(Float value) {
		y = (int) (PANEL_SIZE / 2 * value + PANEL_SIZE / 2);
		YLabel.setText(String.format("%.3f", value));
	}

} // end of CompassPanel class
