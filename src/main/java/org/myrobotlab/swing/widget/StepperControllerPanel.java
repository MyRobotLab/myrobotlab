package org.myrobotlab.swing.widget;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * @author GroG interface to update a MotorGUI's Controller Panel
 * 
 */
abstract class StepperControllerPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  StepperControllerPanel() {
    setBorder(BorderFactory.createTitledBorder("type"));
  }

  abstract void setAttached(boolean state);

  abstract public void setData(Object[] data);

}
