package org.myrobotlab.swing.widget;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.myrobotlab.framework.interfaces.NameTypeProvider;
import org.myrobotlab.image.Util;

public class ImageNameRenderer extends JLabel implements ListCellRenderer {

  private static final long serialVersionUID = 1L;

  public ImageNameRenderer() {
    setOpaque(true);
    setIconTextGap(12);
  }

  @Override
  public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    NameTypeProvider entry = (NameTypeProvider) value;
    setText("<html><font color=#" + Style.listBackground + ">" + entry.getName() + "</font></html>");

    ImageIcon icon = Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50);
    setIcon(icon);

    if (isSelected) {
      setBackground(Style.listHighlight);
      setForeground(Style.listBackground);
    } else {
      setBackground(Style.listBackground);
      setForeground(Style.listForeground);
    }

    // log.info("getListCellRendererComponent - end");
    return this;
  }
}

