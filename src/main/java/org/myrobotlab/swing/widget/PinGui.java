/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.interfaces.DisplayProvider;
import org.slf4j.Logger;

/**
 * graphical representation of a pin with button capability
 * 
 * it contains a PinDefintion and offers controls on how to display
 * 
 * it is updated by PinData
 * 
 * it's display is retrieved through getDisplay()
 * 
 * @author GroG
 *
 */
public class PinGui implements DisplayProvider, ActionListener, ChangeListener {
  public final static Logger log = LoggerFactory.getLogger(PinGui.class);

  PinDefinition pinDef;
  JSlider slider;
  JLabel sliderOutput;
  boolean isVertical = false;
  JButton popupLauncher = new JButton();

  // enable disable popup
  JPopupMenu popup = new JPopupMenu();
  JButton digitalWrite0 = new JButton("0");
  JButton digitalWrite1 = new JButton("1");
  // JButton read = new JButton(" read ");

  int height = 15;
  int width = 15;

  // GridBagConstraints gc = new GridBagConstraints();

  JPanel display = new JPanel(new GridBagLayout());

  Color popupLauncherFg = Color.BLACK;
  Color popupLauncherBg = Color.LIGHT_GRAY;

  Color digitalWrite0Fg = Color.BLACK;
  Color digitalWrite0Bg = Color.LIGHT_GRAY;

  Color digitalWrite1Fg = Color.BLACK;
  Color digitalWrite1Bg = Color.GREEN;// Color.decode("#80ed7d");

  Color readFg = Color.BLACK;
  Color readBg = Color.orange;

  JPanel popupPanel;

  ActionListener relay;

  boolean isReading = false;

  MessageSender sender;

  // boolean displayAddress = true;

  public PinGui(MessageSender service, final PinDefinition pinDef) {
    this.sender = service;
    this.pinDef = pinDef;

    digitalWrite0.setPreferredSize(new Dimension(30, 30));
    digitalWrite1.setPreferredSize(new Dimension(30, 30));

    popupPanel = new JPanel(new GridLayout(0, 2));
    popup.add(popupPanel);

    // if (pinDef.canWrite()) {
    digitalWrite0.setBackground(digitalWrite0Bg);
    digitalWrite0.setForeground(digitalWrite0Fg);
    digitalWrite0.setBorder(null);
    digitalWrite0.setOpaque(true);
    digitalWrite0.setBorderPainted(false);
    digitalWrite0.addActionListener(this);
    popupPanel.add(digitalWrite0);

    digitalWrite1.setBackground(digitalWrite1Bg);
    digitalWrite1.setForeground(digitalWrite1Fg);
    digitalWrite1.setBorder(null);
    digitalWrite1.setOpaque(true);
    digitalWrite1.setBorderPainted(false);
    digitalWrite1.addActionListener(this);
    popupPanel.add(digitalWrite1);
    // }

    /*
     * - PinGui is used by pin maps ... pin maps are nice ways to do
     * "control/writes" Oscope is the better way to do "status/reads" - lets
     * make this a clear division so we won't give the option to read
     *
     */
    /*
     * if (pinDef.canRead()) { read.setBackground(readBg);
     * read.setForeground(readFg); read.setBorder(null); read.setOpaque(true);
     * read.setBorderPainted(false); read.addActionListener(this);
     * popupPanel.add(read); }
     */

    //////////// button begin /////////////////

    popupLauncher.setBackground(popupLauncherBg);
    popupLauncher.setForeground(popupLauncherFg);
    popupLauncher.setBorder(null);
    popupLauncher.setOpaque(true);
    popupLauncher.setBorderPainted(false);
    popupLauncher.setPreferredSize(new Dimension(30, 30));
    // popupLauncher.setBounds(size);
    // onOff.setText(pinDef.getName());
    // stateButton.setText("ab");
    /*
    if (displayAddress) {
      popupLauncher.setText(pinDef.getAddress() + "");
    } else {
      popupLauncher.setText("");
    }
    */
    popupLauncher.addActionListener(this);

    //////////// button end /////////////////

    if (pinDef.isPwm()) {
      int orientation = (isVertical) ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL;
      slider = new JSlider(orientation, 0, 255, 0);
      slider.setOpaque(false);
      slider.addChangeListener(this);
      
      popupPanel.add(slider);
      sliderOutput = new JLabel("0");
      popupPanel.add(sliderOutput);
    }

    popupLauncher.setPreferredSize(new Dimension(width, height));
    display.add(popupLauncher);

  }

  public void send(String method) {
    send(method, (Object[]) null);
  }

  public void send(String method, Object... params) {
    sender.send(sender.getName(), method, params);
  }

  // FIXME - display vs control ---> setDisplayOn
  // in fact display is 'always' callbacks (mostly)
  public void update(PinData pinData) {
    if (pinData.value > 0) {
      // setOn();
    }
  }

  @Override
  public Component getDisplay() {
    return display;
  }

  public void addActionListener(ActionListener relay) {
    this.relay = relay;
  }

  public void setBounds(int x, int y, int width, int height) {
    display.setBounds(x, y, width, height);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("actionPerformed");
    Object o = e.getSource();
    // FIXME - state update can happen from 'user' or from 'board'
    /*
     * if (pinDef.getValue() > 0){
     * 
     * }
     */
    // simple 2 state at the moment ...
    if (o == popupLauncher) {
      /*
      Color activeSelectingColor = Color.CYAN;
      popupLauncher.setBackground(activeSelectingColor); 
      */     
      popup.show(popupLauncher, popupLauncher.getBounds().x, popupLauncher.getBounds().y + popupLauncher.getBounds().height);
      return;
    }

    if (o == digitalWrite0) {
      send("pinMode", pinDef.getAddress(), Arduino.OUTPUT);
      send("digitalWrite", pinDef.getAddress(), 0);
      popupLauncher.setText("0");
      popupLauncher.setBackground(Color.LIGHT_GRAY);
    }

    if (o == digitalWrite1) {
      send("pinMode", pinDef.getAddress(), Arduino.OUTPUT);
      send("digitalWrite", pinDef.getAddress(), 1);
      popupLauncher.setText("1");
      popupLauncher.setBackground(Color.GREEN);
    }

    /**
     * <pre>
     * 
     * if (o == read) {
     *   if (!isReading) {
     *     send("enablePin", pinDef.getAddress());
     *     popupLauncher.setBackground(readBg);
     *     popupLauncher.setText("");
     *     popup.setVisible(false);
     *     isReading = true;
     *   } else {
     *     send("disablePin", pinDef.getAddress());
     *     popupLauncher.setBackground(popupLauncherBg);
     *     popupLauncher.setText("");
     *     isReading = false;
     *   }
     * }
     * </pre>
     */

    popup.setVisible(false);
    // relaying the event upwards
    e.setSource(this);
    relay.actionPerformed(e);
  }

  public PinDefinition getPinDef() {
    return pinDef;
  }

  public void showName() {
    popupLauncher.setText(pinDef.getPinName());
  }

  public void showAddress() {
    popupLauncher.setText(pinDef.getAddress() + "");
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    sliderOutput.setText(slider.getValue() + "");
    send("analogWrite", pinDef.getAddress(), slider.getValue());
  }

  public void setLocation(int i, int j) {
    setBounds(i, j, width, height);
  }
}