package org.myrobotlab.swing.widget;

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

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class JoystickCompassPanel extends JPanel {
  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JoystickCompassPanel.class);
  
  static final int PANEL_SIZE = 80;

  int x, y;
  String xId;
  String yId;
  JLabel xLabel = new JLabel();
  JLabel yLabel = new JLabel();
  JLabel xValueLabel = new JLabel();
  JLabel yValueLabel = new JLabel();
  JLabel screen = new JLabel();

  public JoystickCompassPanel() {
    setLayout(new BorderLayout());
    setBackground(Style.listHighlight);
    screen.setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));
    add(screen, BorderLayout.CENTER);

    JPanel info = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;

    gc.gridwidth = 1;
    ++gc.gridy;
    info.add(xLabel, gc);
    ++gc.gridx;
    xValueLabel.setText((new Float(0.0)).toString());
    info.add(xValueLabel, gc);

    gc.gridx = 0;
    ++gc.gridy;
    info.add(yLabel, gc);
    ++gc.gridx;
    yValueLabel.setText((new Float(0.0)).toString());
    info.add(yValueLabel, gc);

    add(info, BorderLayout.PAGE_END);

  }

  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);

    g.drawRect(1, 1, PANEL_SIZE - 2, PANEL_SIZE - 2); // a black border
    g.drawLine(x - 6, y, x + 6, y);
    g.drawLine(x, y - 6, x, y + 6);

  }

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
  
  public void setXid(String xId){
	  this.xId = xId;
	  xLabel.setText(xId + ":");
  }
  
  public void setYid(String yId){
	  this.yId = yId;
	  yLabel.setText(yId + ":");
  }

  public void setX(Float value) {
    x = (int) (PANEL_SIZE / 2 * value + PANEL_SIZE / 2);
    xValueLabel.setText(String.format("%.3f", value));
    repaint();
  }

  public void setY(Float value) {
    y = (int) (PANEL_SIZE / 2 * value + PANEL_SIZE / 2);
    yValueLabel.setText(String.format("%.3f", value));
    repaint();
  }

public void set(String id, Float value) {
	if (id.equals(yId)){
		setY(value);
	} else if (id.equals(xId)) {
		setX(value);
	} else {
		log.error("{} is not found", id);
	}
}

} // end of CompassPanel class
