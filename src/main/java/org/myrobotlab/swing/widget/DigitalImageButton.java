package org.myrobotlab.swing.widget;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class DigitalImageButton extends JButton {

  private static final long serialVersionUID = 1L;
  // public int ID = -1;
  public final Object parent;
  ImageIcon offIcon = null;
  ImageIcon onIcon = null;
  int type = -1;

  public DigitalImageButton(Object parent, ImageIcon offIcon, ImageIcon onIcon, int type) {
    super();

    this.parent = parent;
    this.type = type;
    this.onIcon = onIcon;
    this.offIcon = offIcon;

    // image button properties
    setOpaque(false);
    setBorder(null);
    // setBorderPainted(false);
    setContentAreaFilled(false);
    // setIcon(this.offIcon);
  }

}
