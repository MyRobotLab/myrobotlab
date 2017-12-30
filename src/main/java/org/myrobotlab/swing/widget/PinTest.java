package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

public class PinTest extends JPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  static public void main(String[] args) {
    PinTest pt = new PinTest();
    System.out.println(pt);
  }

  public PinTest() {

    JProgressBar progressBar = new JProgressBar();
    add(progressBar);

    JSlider slider = new JSlider();
    slider.setValue(1);

    slider.setMaximum(1);

    Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
    table.put(0, new JLabel("out"));
    table.put(1, new JLabel("in"));
    slider.setLabelTable(table);
    slider.setSnapToTicks(true);
    slider.setPaintLabels(true);
    slider.setPaintTicks(true);
    add(slider);

    JToggleButton tglbtnNewToggleButton = new JToggleButton("out");
    tglbtnNewToggleButton.setBackground(Color.DARK_GRAY);
    tglbtnNewToggleButton.setForeground(Color.GREEN);
    add(tglbtnNewToggleButton);
    slider.setPreferredSize(new Dimension(50, 30));
    // String title = (args.length == 0 ? "Sample Slider" : args[0]);
    String title = "e";
    JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JSlider js4 = new JSlider(SwingConstants.VERTICAL);
    table = new Hashtable<Integer, JLabel>();
    table.put(0, new JLabel("O"));
    table.put(10, new JLabel("Ten"));
    table.put(25, new JLabel("Twenty-Five"));
    table.put(34, new JLabel("Thirty-Four"));
    table.put(52, new JLabel("Fifty-Two"));
    table.put(70, new JLabel("Seventy"));
    table.put(82, new JLabel("Eighty-Two"));
    table.put(100, new JLabel("100"));
    js4.setLabelTable(table);
    js4.setPaintLabels(true);
    js4.setSnapToTicks(true);
    frame.add(js4, BorderLayout.EAST);
    // frame.add(slider, BorderLayout.EAST);
    // frame.add(tglbtnNewToggleButton, BorderLayout.WEST);

    frame.setSize(300, 200);
    frame.setVisible(true);

  }

}
