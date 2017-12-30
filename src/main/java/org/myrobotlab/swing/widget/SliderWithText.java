package org.myrobotlab.swing.widget;

import javax.swing.JLabel;
import javax.swing.JSlider;

public class SliderWithText extends JSlider {
  private static final long serialVersionUID = 1L;
  public JLabel value = new JLabel();

  public SliderWithText(int vertical, int i, int j, float k) {
    super(vertical, i, j, (int) k);
    value.setText(String.format("%d", (int) k));
  }

  public SliderWithText(int vertical, int i, int j, int k) {
    super(vertical, i, j, k);
    value.setText(String.format("%d", k));
  }

  public void setText(int i) {
    value.setText(String.format("%d", i));
  }

}
