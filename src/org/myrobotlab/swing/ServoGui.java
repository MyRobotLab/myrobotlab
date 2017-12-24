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

import com.jidesoft.swing.RangeSlider;

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

  boolean mousePressed;

  private class SliderListener implements ChangeListener, MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
      if (mousePressed) {
        if (myService != null) {
          myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
        } else {
          log.error("can not send message myService is null");
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      mousePressed = true;
      send("setOverrideAutoDisable", true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      mousePressed = false;
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

  private class MapInputSliderListener implements ChangeListener, MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

      minInput.setText(String.format("%d", mapInputSlider.getLowValue()));
      maxInput.setText(String.format("%d", mapInputSlider.getHighValue()));

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

    @Override
    public void mousePressed(MouseEvent e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {

      if (myService != null) {
        send("map", Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()), Double.parseDouble(minOutput.getText()),
            Double.parseDouble(maxOutput.getText()));
      } else {
        log.error("can not send message myService is null");
      }

    }
  }

  private class MapOutputSliderListener implements ChangeListener, MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
      if (mousePressed) {
        if (mapOutputSlider.getInverted()) {
          minOutput.setText(String.format("%d", mapOutputSlider.getHighValue()));
          maxOutput.setText(String.format("%d", mapOutputSlider.getLowValue()));
        } else {
          minOutput.setText(String.format("%d", mapOutputSlider.getLowValue()));
          maxOutput.setText(String.format("%d", mapOutputSlider.getHighValue()));
        }
      }
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

    @Override
    public void mousePressed(MouseEvent e) {
      mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      mousePressed = false;
      if (myService != null) {
        send("map", Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()), Double.parseDouble(minOutput.getText()),
            Double.parseDouble(maxOutput.getText()));
      } else {
        log.error("can not send message myService is null");
      }

    }
  }

  public final static Logger log = LoggerFactory.getLogger(ServoGui.class);
  private String lastControllerUsed;
  static final long serialVersionUID = 1L;

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
  JCheckBox setInverted = new JCheckBox("setInverted");
  JSlider slider = new JSlider(0, 180, 90);
  RangeSlider mapInputSlider = new RangeSlider();
  JLabel InputL = new JLabel("Input MAP :");
  JLabel OutputL = new JLabel("Output MAP : ");
  Integer mapInputSliderMinValue = 0;
  Integer mapInputSliderMaxValue = 180;
  Integer mapOutputSliderMinValue = 0;
  Integer mapOutputSliderMaxValue = 180;
  RangeSlider mapOutputSlider = new RangeSlider();

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
  MapInputSliderListener mapInputSliderListener = new MapInputSliderListener();
  MapOutputSliderListener mapOutputSliderListener = new MapOutputSliderListener();

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

    minInput.setBackground(new Color(188, 208, 244));
    maxInput.setBackground(new Color(188, 208, 244));
    minOutput.setBackground(new Color(200, 238, 206));
    maxOutput.setBackground(new Color(200, 238, 206));

    mapInputSlider.setMinimum(0);
    mapInputSlider.setMaximum(180);

    mapOutputSlider.setMinimum(0);
    mapOutputSlider.setMaximum(180);

    velocity.setPreferredSize(new Dimension(50, 24));
    velocity.setSize(new Dimension(50, 24));
    defaultDisableDelayNoVelocity.setPreferredSize(new Dimension(40, 24));
    disableDelayIfVelocity.setPreferredSize(new Dimension(40, 24));
    boundPos.setFont(boundPos.getFont().deriveFont(32.0f));
    boundPos.setHorizontalAlignment(JLabel.RIGHT);
    imageenabled.setIcon(enabled);
    velocityPic.setIcon(velocityPng);
    autoDisable.setSelected(false);
    setInverted.setSelected(false);
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

    mapInputSlider.setBackground(new Color(188, 208, 244));
    mapOutputSlider.setBackground(new Color(200, 238, 206));

    setVelocity.addActionListener(this);
    updateMinMaxButton.addActionListener(this);
    updateMapButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);
    controller.addActionListener(this);
    attachButton.addActionListener(this);
    enableButton.addActionListener(this);
    autoDisable.addActionListener(this);
    setInverted.addActionListener(this);
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

    JPanel map = new JPanel(new GridLayout(5, 2));
    Border bordermap = BorderFactory.createTitledBorder("Servo limits :");
    map.setBorder(bordermap);
    map.add(mapInputSlider);
    map.add(mapOutputSlider);
    map.add(InputL);
    map.add(OutputL);
    map.add(minInput);
    map.add(minOutput);
    map.add(maxInput);
    map.add(maxOutput);

    map.add(updateMapButton);
    // map.add(updateMapButton);

    // powerSettings.add(disableDelayIfVelocityL);

    // powerSettings.add(defaultDisableDelayNoVelocityL);
    // powerSettings.add(defaultDisableDelayNoVelocity);

    JPanel powerMain = new JPanel();
    powerMain.add(enableButton);
    powerMain.add(autoDisable);
    powerMain.add(setDisableDelays);
    powerMain.add(disableDelayIfVelocity);
    // powerMain.add(powerMainSub);

    JPanel extra = new JPanel(new GridLayout(1, 1));
    Border settingsborder = BorderFactory.createTitledBorder("Extra :");
    extra.setBorder(settingsborder);
    JPanel sweep = new JPanel();
    sweep.add(setInverted);
    sweep.add(sweepButton);
    sweep.add(eventsButton);
    sweep.setBackground(Color.WHITE);

    JPanel velocityP = new JPanel();
    Border borderVelocityP = BorderFactory.createTitledBorder("Velocity :");
    velocityP.setBorder(borderVelocityP);
    velocityP.setBackground(Color.WHITE);

    JPanel velocityPicP = new JPanel();
    velocityPicP.add(velocityPic);
    velocityPicP.setBackground(Color.WHITE);

    JPanel velocitySetings = new JPanel();
    velocitySetings.add(velocity);
    velocitySetings.add(setVelocity);
    velocitySetings.setBackground(Color.WHITE);
    velocityP.add(velocitySetings);
    velocityP.add(velocityPicP);

    extra.add(sweep);
    extra.setBackground(Color.WHITE);

    JPanel power = new JPanel(new GridLayout(1, 1));
    Border extraborder = BorderFactory.createTitledBorder("Power");
    power.setBorder(extraborder);
    power.add(powerMain);

    JPanel northPanel = new JPanel(new GridLayout());
    northPanel.add(controllerP);
    northPanel.add(power);

    display.add(northPanel, BorderLayout.NORTH);
    display.add(right, BorderLayout.EAST);

    JPanel centerPanel = new JPanel(new GridLayout(2, 1));

    JPanel centerPanelStatus = new JPanel(new GridLayout(1, 4));
    centerPanelStatus.setBackground(Color.white);
    centerPanelStatus.add(boundPos);
    centerPanelStatus.add(imageenabled);
    centerPanelStatus.add(velocityP);
    centerPanelStatus.add(extra);

    centerPanel.add(centerPanelStatus);
    centerPanel.add(slider);
    centerPanel.setMinimumSize(new Dimension(50, 200));
    centerPanel.setSize(new Dimension(50, 200));
    display.add(centerPanel, BorderLayout.CENTER);
    display.add(left, BorderLayout.WEST);

    display.add(map, BorderLayout.SOUTH);

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
            send("setAutoDisable", true);
          } else {
            send("setAutoDisable", false);
          }
          return;
        }

        if (o == setInverted) {
          if (setInverted.isSelected()) {
            send("setInverted", true);
          } else {
            send("setInverted", false);
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

        if (servo.isInverted()) {
          setInverted.setSelected(true);
        } else {
          setInverted.setSelected(false);
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

        if (servo.getMinInput() < mapInputSliderMinValue) {
          mapInputSliderMinValue = (int) servo.getMinInput();
          mapInputSlider.setMinimum(mapInputSliderMinValue);
        }

        if (servo.getMaxInput() > mapInputSliderMaxValue) {
          mapInputSliderMaxValue = (int) servo.getMaxInput();
          mapInputSlider.setMaximum(mapInputSliderMaxValue);
        }

        double minOutputTmp = servo.getMinOutput();
        double maxOutputTmp = servo.getMaxOutput();

        if (servo.isInverted()) {
          minOutputTmp = servo.getMaxOutput();
          maxOutputTmp = servo.getMinOutput();
        }

        if (servo.getMinOutput() < mapOutputSliderMinValue) {
          mapOutputSliderMinValue = (int) servo.getMinOutput();
          mapOutputSlider.setMinimum(mapOutputSliderMinValue);
        }

        if (servo.getMaxOutput() > mapOutputSliderMaxValue) {
          mapOutputSliderMaxValue = (int) servo.getMaxOutput();
          mapOutputSlider.setMaximum(mapOutputSliderMaxValue);
        }

        mapOutputSlider.setInverted(servo.isInverted());

        minInput.setText(servo.getMinInput() + "");
        maxInput.setText(servo.getMaxInput() + "");
        minOutput.setText(minOutputTmp + "");
        maxOutput.setText(maxOutputTmp + "");

        mapInputSlider.setLowValue((int) servo.getMinInput());
        mapInputSlider.setHighValue((int) servo.getMaxInput());
        mapOutputSlider.setLowValue((int) servo.getMinOutput());
        mapOutputSlider.setHighValue((int) servo.getMaxOutput());

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
    mapInputSlider.removeChangeListener(mapInputSliderListener);
    mapOutputSlider.removeChangeListener(mapOutputSliderListener);
    mapInputSlider.removeMouseListener(mapInputSliderListener);
    mapOutputSlider.removeMouseListener(mapOutputSliderListener);
  }

  public void restoreListeners() {
    controller.addActionListener(this);
    pinList.addActionListener(this);
    slider.addChangeListener(sliderListener);
    slider.addMouseListener(sliderListener);
    mapInputSlider.addChangeListener(mapInputSliderListener);
    mapOutputSlider.addChangeListener(mapOutputSliderListener);
    mapInputSlider.addMouseListener(mapInputSliderListener);
    mapOutputSlider.addMouseListener(mapOutputSliderListener);
  }
}