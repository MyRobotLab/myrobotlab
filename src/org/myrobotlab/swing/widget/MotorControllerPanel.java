package org.myrobotlab.swing.widget;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.myrobotlab.service.Motor;

/**
 * @author GroG interface to update a MotorGUI's Controller Panel
 * 
 */
public abstract class MotorControllerPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  MotorControllerPanel() {
    setBorder(BorderFactory.createTitledBorder("type"));
  } 

  abstract public void set(Motor motor);

  public abstract void setAttached(boolean state);

}
