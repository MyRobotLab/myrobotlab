package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;

public class OscopeTrace implements ActionListener {
  JButton b;
  PinDefinition pinDef;
  Color color;
  Color bgColor = Color.BLACK;
  Oscope oscope;
  ActionListener relay;

  OscopePinDisplay pinDisplay;
  // FIXME - this could be single display with multiple references to combine
  // signals
  JPanel screenDisplay = new JPanel(new BorderLayout());
  JLabel pinInfo = new JLabel();

  // default trace dimensions
  int width = 600;
  int height = 100;

  int screen0X;
  int screen1X;

  PinData pinData;
  PinData lastPinData;

  int lastX = 0;
  int lastY = 0;

  JButton pause;

  public OscopeTrace(Oscope oscope, PinDefinition pinDef, Color hsv) {
    this.oscope = oscope;
    this.pinDef = pinDef;
    b = new JButton(pinDef.getPinName());
    b.setMargin(new Insets(0, 0, 0, 0));
    b.setBorder(null);
    b.setPreferredSize(new Dimension(30, 30));
    b.setBackground(hsv);
    color = hsv;
    b.addActionListener(this);
    // relay <- FIXME - what needs relaying !?!?!?! and why ?!?!??
    addActionListener(oscope);

    pinDisplay = new OscopePinDisplay(this);

    screenDisplay.add(pinDisplay, BorderLayout.CENTER);

    JPanel flow = new JPanel();
    flow.add(new JLabel(pinDef.getPinName()));
    flow.add(pinInfo);
    pause = new JButton(Util.getScaledIcon(Util.getImage("pause.png"), 0.25));
    flow.add(pause);
    pause.addActionListener(this);

    screenDisplay.add(flow, BorderLayout.WEST);
    screenDisplay.setVisible(false);

  }

  public Component getButtonDisplay() {
    return b;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    e.setSource(this);
    Object o = e.getSource();
    if (o == pause) {
      pinDisplay.paused = !pinDisplay.paused;
    } else {
      // FIXME DO NOT DO THIS !!! - handle the event locally !!
      relay.actionPerformed(e);
    }
  }

  public void addActionListener(ActionListener relay) {
    this.relay = relay;
  }

  public PinDefinition getPinDef() {
    return pinDef;
  }

  public Component getScreenDisplay() {
    return screenDisplay;
  }

  public void update(PinData pinData) {
    pinDisplay.update(pinData);
  }

  public void setVisible(boolean visible) {
    screenDisplay.setVisible(visible);
  }

}
