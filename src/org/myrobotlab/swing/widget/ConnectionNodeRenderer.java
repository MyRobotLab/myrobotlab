package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.myrobotlab.image.Util;
import org.myrobotlab.net.Connection;

public class ConnectionNodeRenderer extends JLabel implements ListCellRenderer {
  private static final long serialVersionUID = 1L;

  public static final Color connected = Color.decode("#99FF99");
  public static final Color disconnected = Color.decode("#FFCCCC");

  public ConnectionNodeRenderer() {
    setOpaque(true);
    setIconTextGap(18);

  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

    Connection data = (Connection) value;

    /*
     * StringBuffer sb = new StringBuffer(); sb.append("<html>");
     * 
     * sb.append("</html>");
     */
    // String path = data.protocolKey.getPath().substring(1);

    setText(String.format("<html>%s %s %s<br/>RX %s.%s %d<br/>TX %s.%s %d</html>", data.prefix, data.protocolKey, data.state, data.rxName, data.rxMethod, data.rx, data.txName,
        data.txMethod, data.tx));// data.toString();//String.format("%s
                                 // connected rx %d tx %d ",
    // uri.toString(), data.rx,
    // data.tx);
    // setIcon(Util.getResourceIcon("instance.png"));
    setIcon(Util.getResourceIcon("connection.png"));

    // add(new JButton("BUTTON!"));

    //
    if (Connection.CONNECTED.equals(data.state)) {
      setBackground(connected);
    } else {
      setBackground(disconnected);
    }

    /*
     * if (isSelected) { setBackground(Color.lightGray);
     * setForeground(Color.white); } else { setBackground(Color.white);
     * setForeground(Color.black); }
     */

    return this;
  }

}
