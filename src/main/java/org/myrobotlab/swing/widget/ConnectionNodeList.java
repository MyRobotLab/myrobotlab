package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

public class ConnectionNodeList extends JPanel {

  private static final long serialVersionUID = 1L;

  public DefaultListModel model = new DefaultListModel();
  public JList nodeList;

  public static void main(String s[]) {
    LoggingFactory.init(Level.DEBUG);

    JFrame frame = new JFrame("List Example");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    ConnectionNodeList cl = new ConnectionNodeList();

    frame.setContentPane(cl);
    frame.pack();
    frame.setVisible(true);
  }

  public ConnectionNodeList() {
    setLayout(new BorderLayout());

    model = new DefaultListModel();
    nodeList = new JList(model);
    nodeList.setCellRenderer(new ConnectionNodeRenderer());
    nodeList.setVisibleRowCount(8);
    JScrollPane pane = new JScrollPane(nodeList);
    add(pane, BorderLayout.NORTH);
    // add(button, BorderLayout.SOUTH);
  }

}