package org.myrobotlab.swing.widget;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.myrobotlab.image.Util;

/**
 * @author GroG .rollover are done with originals + gimp -&gt; colorize -&gt; adjust
 *         color balance -&gt; 100 green .activated are done with originals + gimp
 *         -&gt; colorize -&gt; adjust color balance -&gt; 100 red
 * 
 */
public class ImageButton extends JButton {

  private static final long serialVersionUID = 1L;
  public final Object parent;
  ImageIcon icon = null;
  ImageIcon rolloverIcon = null;
  ImageIcon activatedIcon = null;
  int type = -1;

  public ImageButton(Object parent, ImageIcon icon, ImageIcon rolloverIcon, ImageIcon activatedIcon, String tooltip, int type, ActionListener listener) {
    super();

    this.parent = parent;
    this.type = type;

    // images
    this.icon = icon;
    this.rolloverIcon = rolloverIcon;
    this.activatedIcon = activatedIcon;

    // image button properties
    setOpaque(false);
    setBorder(null);
    // setBorderPainted(false);
    setContentAreaFilled(false);
    setIcon(icon);
    setToolTipText(tooltip);

    if (rolloverIcon != null) {
      setRolloverEnabled(true);
      setRolloverIcon(rolloverIcon);
    }
    if (activatedIcon != null) {
      setSelectedIcon(activatedIcon);
    }

    if (listener != null) {
      addActionListener(listener);
    }
    /*
     * b.setMargin(new Insets(0, 0, 0, 0)); b.setBorderPainted(false);
     * b.setToolTipText(name); b.setBackground(new Color(0x006468));
     */
  }

  public ImageButton(String serviceType, String name) {
    this(name, Util.getImageIcon(serviceType + "/" + name + ".png"), Util.getImageIcon(serviceType + "/" + name + ".rollover.png"),
        Util.getImageIcon(serviceType + "/" + name + ".activated.png"), name, -1, null);
  }

  public ImageButton(String serviceType, String name, ActionListener listener) {
    this(name, Util.getImageIcon(serviceType + "/" + name + ".png"), Util.getImageIcon(serviceType + "/" + name + ".rollover.png"),
        Util.getImageIcon(serviceType + "/" + name + ".activated.png"), name, -1, listener);
  }

  public void activate() {
    setIcon(activatedIcon);
  }

  public void deactivate() {
    setIcon(icon);
  }

  public boolean isActive() {
    return getIcon() == activatedIcon;
  }

  public void press() {
    // FIXME - implement SwingUtils.whatever
  }

}
