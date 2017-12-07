/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * Servo SwingGui - displays details of Servo state Lesson learned ! Servos to
 * properly function need to be attached to a controller This gui previously
 * sent messages to the controller. To simplify things its important to send
 * messages only to the bound Servo - and let it attach to the controller versus
 * sending messages directly to the controller. 1 display - 1 service - keep it
 * simple
 *
 */
public class ServoGui extends ServiceGui implements ActionListener {

  private class SliderListener implements ChangeListener, MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

      boundPos.setText(String.format("%d", slider.getValue()));

      if (myService != null) {
        myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
      } else {
        log.error("can not send message myService is null");
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      send("setOverrideAutoDisable", true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      send("setOverrideAutoDisable", false);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
      // TODO Auto-generated method stub

    }
  }

  public final static Logger log = LoggerFactory.getLogger(ServoGui.class);
  private String lastControllerUsed;
  static final long serialVersionUID = 1L;
  protected static final AbstractButton disableDelayIfnoVelocity = null;

  JLabel boundPos = new JLabel("90");
  JButton attachButton = new JButton("attach");
  JButton updateMinMaxButton = new JButton("set");
  JTextField velocity = new JTextField("-1");
  JLabel disableDelayIfVelocityL = new JLabel("Extra delay ( ms ): ");
  JLabel defaultDisableDelayNoVelocityL = new JLabel("Max velocity delay ( ms ) : ");
  
  JTextField disableDelayIfVelocity = new JTextField("1000");
  JTextField defaultDisableDelayNoVelocity = new JTextField("10000");
  JButton setVelocity = new JButton("set");
  JButton setDisableDelays = new JButton("save");

  JButton updateMapButton = new JButton("set");
  JButton enableButton = new JButton("enable");
  JCheckBox autoDisable = new JCheckBox("autoDisable");
  JSlider slider = new JSlider(0, 180, 90);
  BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
  BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

  JComboBox<String> controller = new JComboBox<String>();
  JComboBox<Integer> pinList = new JComboBox<Integer>();

  JTextField posMin = new JTextField("0");
  JTextField posMax = new JTextField("180");
  JTextField minInput = new JTextField("0");
  JTextField maxInput = new JTextField("180");
  JTextField minOutput = new JTextField("0");
  JTextField maxOutput = new JTextField("180");

  JButton sweepButton = new JButton("sweep");
  JButton eventsButton = new JButton("events");

  JLabel imageenabled = new JLabel();
  JLabel velocityPic = new JLabel();
  ImageIcon enabled = Util.getImageIcon("enabled.png");
  ImageIcon velocityPng = Util.getImageIcon("velocity.png");;

  // Servo myServox = null;

  SliderListener sliderListener = new SliderListener();

  boolean eventsEnabled;

  public ServoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    // myServo = (Servo) Runtime.getService(boundServiceName);

    for (int i = 0; i < 54; i++) {
      pinList.addItem(i);
    }

    posMin.setPreferredSize(new Dimension(50, 24));
    posMax.setPreferredSize(new Dimension(50, 24));
    minInput.setPreferredSize(new Dimension(50, 24));
    maxInput.setPreferredSize(new Dimension(50, 24));
    minOutput.setPreferredSize(new Dimension(50, 24));
    maxOutput.setPreferredSize(new Dimension(50, 24));
    
    minInput.setBackground(new Color(188,208,244));
    maxInput.setBackground(new Color(188,208,244));
    minOutput.setBackground(new Color(200,238,206));
    maxOutput.setBackground(new Color(200,238,206));
    
    velocity.setPreferredSize(new Dimension(50, 24));
    velocity.setSize(new Dimension(50, 24));
    defaultDisableDelayNoVelocity.setPreferredSize(new Dimension(40, 24));
    disableDelayIfVelocity.setPreferredSize(new Dimension(40, 24));
    boundPos.setFont(boundPos.getFont().deriveFont(32.0f));
    boundPos.setHorizontalAlignment(JLabel.RIGHT);
    imageenabled.setIcon(enabled);
    velocityPic.setIcon(velocityPng);
    autoDisable.setSelected(false);
    defaultDisableDelayNoVelocityL.setFont(new Font("Arial", Font.BOLD, 10));
    disableDelayIfVelocityL.setFont(new Font("Arial", Font.BOLD, 10));
 
    slider.setForeground(Color.white);
    slider.setBackground(Color.DARK_GRAY);
    left.setForeground(Color.white);
    left.setBackground(Color.DARK_GRAY);
    right.setForeground(Color.white);
    right.setBackground(Color.DARK_GRAY);
    slider.setMajorTickSpacing(30);
    slider.setPaintTicks(true);
    slider.setPaintTicks(true);
    slider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    slider.setPaintLabels(true);

    setVelocity.addActionListener(this);
    updateMinMaxButton.addActionListener(this);
    updateMapButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);
    controller.addActionListener(this);
    attachButton.addActionListener(this);
    enableButton.addActionListener(this);
    autoDisable.addActionListener(this);
    sweepButton.addActionListener(this);
    eventsButton.addActionListener(this);
    pinList.addActionListener(this);
    setDisableDelays.addActionListener(this);

    // addTopLeft(2, boundPos, 3, s,velocity,setVelocity );

    JPanel controllerP = new JPanel();
    Border borderController = BorderFactory.createTitledBorder("Controller");
    controllerP.setBorder(borderController);
    JLabel controllerlabel = new JLabel("controller : ");
    JLabel pinlabel = new JLabel("pin : ");
    controllerP.add(controllerlabel);
    controllerP.add(controller);
    controllerP.add(pinlabel);
    controllerP.add(pinList);
    controllerP.add(attachButton);

    JPanel map = new JPanel();
    Border bordermap = BorderFactory.createTitledBorder("map( minInput, maxInput, minOutput, maxOutput )");
    map.setBorder(bordermap);
    map.add(minInput);
    map.add(maxInput);
    map.add(minOutput);
    map.add(maxOutput);
    map.add(updateMapButton);

    JPanel powerSettings = new JPanel(new GridLayout(4, 1));
    powerSettings.add(disableDelayIfVelocityL);
    powerSettings.add(disableDelayIfVelocity);
    powerSettings.add(defaultDisableDelayNoVelocityL);
    powerSettings.add(defaultDisableDelayNoVelocity);

    JPanel powerMain = new JPanel(new GridLayout(2, 1));
    JPanel powerMainSub = new JPanel();
    powerMainSub.add(autoDisable);
    powerMainSub.add(setDisableDelays);
    powerMain.add(enableButton);
    powerMain.add(powerMainSub);

    JPanel power = new JPanel(new GridLayout(1, 2));
    Border extraborder = BorderFactory.createTitledBorder("Power");
    power.setBorder(extraborder);
    power.add(powerMain);
    power.add(powerSettings);

    JPanel sweep = new JPanel();
    Border sweepborder = BorderFactory.createTitledBorder("Sweep and Events");
    sweep.setBorder(sweepborder);
    sweep.add(sweepButton);
    sweep.add(eventsButton);

    JPanel northPanel = new JPanel(new GridLayout());
    northPanel.add(controllerP);
    northPanel.add(power);
    display.add(northPanel, BorderLayout.NORTH);
    display.add(right, BorderLayout.EAST);

    JPanel centerPanel = new JPanel(new GridLayout(2, 1));

    JPanel centerPanelStatus = new JPanel(new GridLayout(1, 3));
    centerPanelStatus.setBackground(Color.white);
    centerPanelStatus.add(boundPos);
    centerPanelStatus.add(imageenabled);

    JPanel velocityP = new JPanel(new GridLayout(2, 1));
    Border borderVelocityP = BorderFactory.createTitledBorder("Velocity");
    velocityP.setBorder(borderVelocityP);
    velocityP.setBackground(Color.WHITE);

    JPanel velocityPicP = new JPanel(new FlowLayout());
    velocityPicP.add(velocityPic);
    velocityPicP.setBackground(Color.WHITE);

    JPanel velocitySetings = new JPanel();
    velocitySetings.add(velocity);
    velocitySetings.add(setVelocity);
    velocitySetings.setBackground(Color.WHITE);

    velocityP.add(velocityPicP);
    velocityP.add(velocitySetings);

    centerPanelStatus.add(velocityP);
    centerPanel.add(centerPanelStatus);

    centerPanel.add(slider);
    display.add(centerPanel, BorderLayout.CENTER);
    display.add(left, BorderLayout.WEST);

    JPanel southPanel = new JPanel(new GridLayout(2, 2));
    southPanel.add(map);
    southPanel.add(sweep);
    display.add(southPanel, BorderLayout.SOUTH);

    // addTopLeft(" ");
    // addTopLeft(controllerP);
    // addTopLeft(power);
    // addTopLeft(" ");
    // addTopLeft(map);
    // addTopLeft(minMax);
    // addTopLeft(sweep);

    refreshControllers();
  }

  // SwingGui's action processing section - data from user
  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        // log.error(o.toString());
        if (o == controller) {
          String controllerName = (String) controller.getSelectedItem();
          log.debug(String.format("controller event %s", controllerName));
          if (controllerName != null && controllerName.length() > 0) {

            // NOT WORTH IT - JUST BUILD 48 PINS !!!
            // ServoController sc = (ServoController)
            // Runtime.getService(controllerName);

            // NOT WORTH THE TROUBLE !!!!
            // @SuppressWarnings("unchecked")
            // ArrayList<Pin> pinList = (ArrayList<Pin>)
            // myService.sendBlocking(controllerName, "getPinList");
            // log.info("{}", pinList.size());

            // FIXME - get Local services relative to the servo
            // pinModel.removeAllElements();
            // pinModel.addElement(null);

            // for (int i = 0; i < pinList.size(); ++i) {
            // pinModel.addElement(pinList.get(i).pin);
            // }

            // pin.invalidate();

          }
        }
        if (o == setVelocity) {
          send("setVelocity", Double.parseDouble(velocity.getText()));
          return;
        }

        if (o == attachButton) {
          if (attachButton.getText().equals("attach")) {
            send("attach", controller.getSelectedItem(), (int) pinList.getSelectedItem(), new Double(slider.getValue()));
          } else {
            lastControllerUsed = (String) controller.getSelectedItem();
            send("detach", controller.getSelectedItem());
          }
          return;
        }

        if (o == enableButton) {
          if (enableButton.getText().equals("enable")) {
            if (!attachButton.getText().equals("attach")) {
              send("enable");
              imageenabled.setVisible(true);
            } else {
              log.error("Servo is not attached");
            }
          } else {
            send("disable");
            imageenabled.setVisible(false);
          }
          return;
        }

        if (o == autoDisable) {
          if (autoDisable.isSelected()) {
            send("enableAutoDisable", true);
          } else {
            send("enableAutoDisable", false);
          }
          return;
        }

        if (o == updateMinMaxButton) {
          send("setMinMax", Double.parseDouble(posMin.getText()), Double.parseDouble(posMax.getText()));
          return;
        }

        if (o == updateMapButton) {
          send("map", Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()), Double.parseDouble(minOutput.getText()),
              Double.parseDouble(maxOutput.getText()));
          return;
        }

        if (o == right) {
          slider.setValue(slider.getValue() + 1);
          return;
        }

        if (o == left) {
          slider.setValue(slider.getValue() - 1);
          return;
        }

        if (o == sweepButton) {
          if (sweepButton.getText().equals("sweep")) {
            send("sweep");
          } else {
            send("stop");
          }
          return;
        }

        if (o == eventsButton) {
          send("eventsEnabled", !eventsEnabled);
          return;
        }

        if (o == setDisableDelays) {
          Integer delayIfV = 1000;
          Integer delayNoV = 10000;

          try {
            delayIfV = Integer.parseInt(disableDelayIfVelocity.getText());
            delayNoV = Integer.parseInt(defaultDisableDelayNoVelocity.getText());
          } catch (Exception e) {
            warn("Bad value for disableDelay !");
          }

          send("setDisableDelayIfVelocity", delayIfV);
          send("setDefaultDisableDelayNoVelocity", delayNoV);

          return;
        }

      }
    });
  }

  @Override
  public void subscribeGui() {
    subscribe("refreshControllers");
  }

  // FIXME - runtime should handle all unsubscribe of teardown
  @Override
  public void unsubscribeGui() {
    unsubscribe("refreshControllers");
  }

  synchronized public void onState(final Servo servo) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        removeListeners();
        refreshControllers();

        ServoController sc = servo.getController();

        if (sc != null) {
          controller.setSelectedItem(sc.getName());

          Integer servoPin = servo.getPin();

          if (servoPin != null)
            pinList.setSelectedItem(servoPin);
        }

        if (servo.isAttached()) {
          attachButton.setText("detach");
          controller.setEnabled(false);
          pinList.setEnabled(false);
        } else {
          attachButton.setText("attach");
          controller.setEnabled(true);
          pinList.setEnabled(true);
        }

        if (servo.isEnabled()) {
          enableButton.setText("disable");
          imageenabled.setVisible(true);
        } else {
          enableButton.setText("enable");
          imageenabled.setVisible(false);
        }

        if (servo.getAutoDisable()) {
          autoDisable.setSelected(true);
        } else {
          autoDisable.setSelected(false);
        }

        Double pos = servo.getPos();
        if (pos != null) {
          boundPos.setText(Double.toString(pos));
          slider.setValue(pos.intValue());
        }

        // In the inverted case, these are reversed
        slider.setMinimum((int) servo.getMin());
        slider.setMaximum((int) servo.getMax());

        posMin.setText(servo.getMin() + "");
        posMax.setText(servo.getMax() + "");
        velocity.setText(servo.getVelocity() + "");

        disableDelayIfVelocity.setText(servo.disableDelayIfVelocity + "");
        defaultDisableDelayNoVelocity.setText(servo.defaultDisableDelayNoVelocity + "");

        minInput.setText(servo.getMinInput() + "");
        maxInput.setText(servo.getMaxInput() + "");
        minOutput.setText(servo.getMinOutput() + "");
        maxOutput.setText(servo.getMaxOutput() + "");

        if (servo.isSweeping()) {
          sweepButton.setText("stop");
        } else {
          sweepButton.setText("sweep");
        }

        eventsEnabled = servo.isEventsEnabled();

        restoreListeners();
      }
    });

  }

  public void onRefreshControllers(final ArrayList<String> c) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        controller.removeActionListener((ServoGui) self);
        String currentControllerName = (String) controller.getSelectedItem();
        controller.removeAllItems();
        for (int i = 0; i < c.size(); ++i) {
          controller.addItem(c.get(i));
        }
        String controllerName = (currentControllerName != null) ? currentControllerName : lastControllerUsed;
        controller.setSelectedItem(controllerName);
        controller.addActionListener((ServoGui) self);
      }
    });
  }

  public void refreshControllers() {
    send("refreshControllers");
  }

  public void removeListeners() {
    controller.removeActionListener(this);
    pinList.removeActionListener(this);
    slider.removeChangeListener(sliderListener);
    slider.removeMouseListener(sliderListener);
  }

  public void restoreListeners() {
    controller.addActionListener(this);
    pinList.addActionListener(this);
    slider.addChangeListener(sliderListener);
    slider.addMouseListener(sliderListener);
  }
}