package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class DirectionWidget extends JPanel implements KeyListener {

  public class ButtonActionListener implements ActionListener {
    ActionListener chainedListener;

    @Override
    public void actionPerformed(ActionEvent ae) {
      // nJButton button = (JButton)ae.getSource();

      if (chainedListener != null) {
        chainedListener.actionPerformed(ae);
      }
    }

    public void setActionListener(ActionListener al) {
      chainedListener = al;
    }

    /*
     * TODO - replaceListener public ComponentListener[] getListeners() { return
     * getComponentListeners(); }
     */
  }

  ButtonActionListener bal = new ButtonActionListener();

  public JButton btnNw;
  public JButton btnN;
  public JButton btnNe;
  public JButton btnE;
  public JButton btnSe;
  public JButton btnS;
  public JButton btnSw;
  public JButton btnW;

  public JButton btnStop;

  // TODO - Spin CW Spin CCW
  // TODO - config - 4 dir 8 dir - spin (other -> globe)
  // TODO - forward driving turn left spin left - differential drive
  // TODO - configurable key mapping

  private static final long serialVersionUID = 1L;

  public DirectionWidget() {
    // setForeground(Color.GREEN);
    /*
     * BevelBorder widgetTitle; widgetTitle = (BevelBorder)
     * BorderFactory.createBevelBorder(BevelBorder.RAISED);
     * this.setBorder(widgetTitle); TitledBorder title; title =
     * BorderFactory.createTitledBorder("direction"); this.setBorder(title);
     */

    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0 };
    gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0 };
    gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
    gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
    setLayout(gridBagLayout);

    btnNw = new JButton("");
    btnNw.setBackground(new Color(173, 255, 47));
    btnNw.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_left_grey.png")));
    btnNw.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_left_green.png")));
    btnNw.addActionListener(bal);
    GridBagConstraints gbc_btnNw = new GridBagConstraints();
    gbc_btnNw.insets = new Insets(0, 0, 0, 0);
    gbc_btnNw.gridx = 0;
    gbc_btnNw.gridy = 0;
    add(btnNw, gbc_btnNw);
    btnNw.addKeyListener(this);
    btnNw.setActionCommand("nw");
    btnNw.setVisible(false);

    btnN = new JButton("");
    btnN.setBackground(new Color(173, 255, 47));
    btnN.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_grey.png")));
    btnN.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_green.png")));
    btnN.addActionListener(bal);
    GridBagConstraints gbc_btnN = new GridBagConstraints();
    gbc_btnN.insets = new Insets(0, 0, 0, 0);
    gbc_btnN.gridx = 1;
    gbc_btnN.gridy = 0;
    add(btnN, gbc_btnN);
    btnN.addKeyListener(this);
    btnN.setActionCommand("n");

    btnNe = new JButton("");
    btnNe.setBackground(new Color(173, 255, 47));
    btnNe.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_right_grey.png")));
    btnNe.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_up_right_green.png")));
    btnNe.addActionListener(bal);
    GridBagConstraints gbc_btnNe = new GridBagConstraints();
    gbc_btnNe.insets = new Insets(0, 0, 0, 0);
    gbc_btnNe.gridx = 2;
    gbc_btnNe.gridy = 0;
    add(btnNe, gbc_btnNe);
    btnNe.addKeyListener(this);
    btnNe.setActionCommand("listener");
    btnNe.setVisible(false);

    btnW = new JButton("");
    btnW.setBackground(new Color(173, 255, 47));
    btnW.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_left_grey.png")));
    btnW.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_left_green.png")));
    btnW.addActionListener(bal);
    GridBagConstraints gbc_btnW = new GridBagConstraints();
    gbc_btnW.insets = new Insets(0, 0, 0, 0);
    gbc_btnW.gridx = 0;
    gbc_btnW.gridy = 1;
    add(btnW, gbc_btnW);
    btnW.addKeyListener(this);
    btnW.setActionCommand("w");

    btnStop = new JButton("");
    btnStop.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/bullet_square_grey.png")));
    btnStop.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/bullet_square_green.png")));
    btnStop.setBackground(new Color(173, 255, 47));
    btnStop.addActionListener(bal);
    GridBagConstraints gbc_btnSt = new GridBagConstraints();
    gbc_btnSt.insets = new Insets(0, 0, 0, 0);
    gbc_btnSt.gridx = 1;
    gbc_btnSt.gridy = 1;
    add(btnStop, gbc_btnSt);
    btnStop.addKeyListener(this);
    btnStop.setActionCommand("stop");

    btnE = new JButton("");
    btnE.setBackground(new Color(173, 255, 47));
    btnE.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_right_grey.png")));
    btnE.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_right_green.png")));
    btnE.addActionListener(bal);
    GridBagConstraints gbc_btnE = new GridBagConstraints();
    gbc_btnE.insets = new Insets(0, 0, 0, 0);
    gbc_btnE.gridx = 2;
    gbc_btnE.gridy = 1;
    add(btnE, gbc_btnE);
    btnE.addKeyListener(this);
    btnE.setActionCommand("e");

    btnSw = new JButton("");
    btnSw.setBackground(new Color(173, 255, 47));
    btnSw.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_left_grey.png")));
    btnSw.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_left_green.png")));
    btnSw.addActionListener(bal);
    GridBagConstraints gbc_btnSw = new GridBagConstraints();
    gbc_btnSw.insets = new Insets(0, 0, 0, 0);
    gbc_btnSw.gridx = 0;
    gbc_btnSw.gridy = 2;
    add(btnSw, gbc_btnSw);
    btnSw.addKeyListener(this);
    btnSw.setActionCommand("sw");
    btnSw.setVisible(false);

    btnS = new JButton("");
    btnS.setBackground(new Color(173, 255, 47));
    btnS.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_grey.png")));
    btnS.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_green.png")));
    btnS.addActionListener(bal);
    GridBagConstraints gbc_btnS = new GridBagConstraints();
    gbc_btnS.insets = new Insets(0, 0, 0, 0);
    gbc_btnS.gridx = 1;
    gbc_btnS.gridy = 2;
    add(btnS, gbc_btnS);
    btnS.addKeyListener(this);
    btnS.setActionCommand("s");

    btnSe = new JButton("");
    btnSe.setBackground(new Color(173, 255, 47));
    btnSe.setIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_right_grey.png")));
    btnSe.setPressedIcon(new ImageIcon(DirectionWidget.class.getResource("/resource/arrow_down_right_green.png")));
    btnSe.addActionListener(bal);
    GridBagConstraints gbc_btnSe = new GridBagConstraints();
    gbc_btnSe.insets = new Insets(0, 0, 0, 0);
    gbc_btnSe.gridx = 2;
    gbc_btnSe.gridy = 2;
    add(btnSe, gbc_btnSe);
    btnSe.addKeyListener(this);
    btnSe.setActionCommand("se");
    btnSe.setVisible(false);

    /*
     * JToggleButton tglbtnT = new JToggleButton("use keyboard");
     * tglbtnT.setBackground(new Color(173, 255, 47)); GridBagConstraints
     * gbc_tglbtnT = new GridBagConstraints(); gbc_tglbtnT.gridwidth = 2;
     * gbc_tglbtnT.insets = new Insets(0, 0, 0, 0); gbc_tglbtnT.gridx = 0;
     * gbc_tglbtnT.gridy = 3; add(tglbtnT, gbc_tglbtnT);
     */
    // this.addKeyListener(this);
    //
  }

  @Override
  public void keyPressed(KeyEvent keyEvent) {
    int code = keyEvent.getKeyCode();

    if (code == KeyEvent.VK_Q) {
      btnNw.doClick();
    } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
      btnN.doClick();
    } else if (code == KeyEvent.VK_E) {
      btnNe.doClick();
    } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
      btnE.doClick();
    } else if (code == KeyEvent.VK_C) {
      btnSe.doClick();
    } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_X) {
      btnS.doClick();
    } else if (code == KeyEvent.VK_Z) {
      btnSw.doClick();
    } else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
      btnW.doClick();
    } else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_S) {
      btnStop.doClick();
    }

  }

  @Override
  public void keyReleased(KeyEvent arg0) {
    // btnN.doClick();
  }

  @Override
  public void keyTyped(KeyEvent arg0) {
    // btnN.doClick();
  }

  public void setDirectionListener(ActionListener a) {
    bal.setActionListener(a);
  }

  /*
   * public void replaceListener(ActionListener al) { ComponentListener[] b =
   * bal.getListeners(); for (int i = 0; i < b.length; ++i) { b[i]. } }
   */
}
