package org.myrobotlab.mapper.sim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

import org.myrobotlab.mapper.gui.Simbad;

public class KeyInputSensor extends SensorDevice implements KeyListener {

  /*
   * a JPanel for displaying the eye image.
   */
  public class KeyInputSensorJPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    Font font;
    Color color;
    final static int HEIGHT = 12;

    public KeyInputSensorJPanel(KeyListener listener) {
      color = new Color(0, 0, 0);
      Dimension d = new Dimension(50, HEIGHT);
      setPreferredSize(d);
      setMinimumSize(d);
      font = new Font("Arial", Font.PLAIN, HEIGHT - 2);

      /*
       * // receive key event in parent this.addKeyListener(parent);
       * 
       * // this.requestFocusInWindow(true); this.requestFocus();
       * setFocusable(true);
       */
      Simbad.getSimbadInstance().getDesktopPane().addKeyListener(listener);

    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setFont(font);
      g.drawString("last key :" + KeyEvent.getKeyText(lastKey), HEIGHT + 10, HEIGHT - 1);

    }

  }

  /** Last input , -1 if none */
  int lastKey;

  JPanel panel;

  public KeyInputSensor() {
    super.create3D(true);
    lastKey = -1;

    // KeyboardFocusManager kfm =
    // KeyboardFocusManager.getCurrentKeyboardFocusManager();
    // Component comp = kfm.getPermanentFocusOwner();
    // comp.addKeyListener(this);
  }

  @Override
  public JPanel createInspectorPanel() {
    panel = new KeyInputSensorJPanel(this);
    return panel;
  }

  public int getLastKey() {
    return lastKey;
  }

  @Override
  public void keyPressed(KeyEvent event) {
    lastKey = event.getKeyCode();
    panel.repaint();

  }

  @Override
  public void keyReleased(KeyEvent arg0) {
    lastKey = -1;
    panel.repaint();

  }

  @Override
  public void keyTyped(KeyEvent event) {

  }

}
