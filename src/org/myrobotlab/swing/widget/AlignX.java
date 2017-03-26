package org.myrobotlab.swing.widget;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;




public class AlignX {

  private static Container makeIt(String labelChar, float alignment) {
    Box box = Box.createVerticalBox();

    for (int i=1; i<6; i++) {
      String label = makeLabel(labelChar, i*2);
      JLabel button = new JLabel(label);
      button.setAlignmentX(alignment);
      box.add(button);
    }
    return box;
  }

  private static String makeLabel(String s, int length) {
    StringBuffer buff = new StringBuffer(length);
    for (int i=0; i<length; i++) {
      buff.append(s);
    }
    return buff.toString();
  }

  public static void main(String args[]) {
    JFrame frame = new JFrame("X Alignment");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container panel1 = makeIt("L", Component.LEFT_ALIGNMENT);
    Container panel2 = makeIt("C", Component.LEFT_ALIGNMENT);
    Container panel3 = makeIt("R", Component.LEFT_ALIGNMENT);
    
    JPanel x = new JPanel();

    x.setLayout(new FlowLayout());
    x.add(panel1);
    x.add(panel2);
    x.add(panel3);
    
    frame.add(x);

    frame.pack();
    frame.setVisible(true);
  }
}
