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

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.HobbyServo;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.VirtualArduino;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;
import org.myrobotlab.swing.widget.CheckBoxTitledBorder;
import org.slf4j.Logger;

import com.jidesoft.swing.RangeSlider;

/**
 * <pre>
 * HobbyServo SwingGui - displays details of HobbyServo state Lesson learned !
 * Servos to properly function need to be attached to a controller This gui
 * previously sent messages to the controller. To simplify things its important
 * to send messages only to the bound HobbyServo - and let it attach to the
 * controller versus sending messages directly to the controller. 1 display - 1
 * service - keep it simple
 * 
 * Operating Speed - is often calculated in time in 60 degrees
 * Avg speed seems to be about 0.12 seconds to rotate 60 degrees
 * 60 degrees/second
 * 0.5 degrees / ms
 * 1 degree per 2 ms
 * 
 * FIXME - iterate through controller types
 * FIXME - test "inverted" and changing max/min input/output
 * FIXME - stay true (especially to position) of position desired vs position reported (depending on encoder)
 * 
 * FIXME - too slow ... logging ??
 * FIXME - if integer resolution then if (Math.round(x) == currentPos) - don't send "new" pos
 * FIXME - global speed
 * FIXME - set all timer's values in one shot that is ServoControl agnostic
 *
 * </pre>
 */
public class HobbyServoGui extends ServiceGui implements ActionListener, ChangeListener {
  static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(HobbyServoGui.class);

  String lastController;

  Double lastSpeed = 60.0;

  JLabel targetPos = new JLabel();
  JLabel currentPos = new JLabel("90.0");

  JButton attach = new JButton("attach");
  JButton attachEncoder = new JButton("attach");
  // JButton export = new JButton("export"); - restore() ?
  JButton restButton = new JButton("rest");
  JTextField speed = new JTextField("         ");
  JTextField rest = new JTextField("");

  ImageIcon movingIcon = Util.getImageIcon("Servo/gifOk.gif");

  JLabel moving = new JLabel(movingIcon);

  JButton save = new JButton("save");
  JButton enable = new JButton("enable");
  CheckBoxTitledBorder speedControlTitle = new CheckBoxTitledBorder("speed control", false);
  JCheckBox speedControl = null;
  JCheckBox autoDisable = new JCheckBox("auto disable");
  JCheckBox setInverted = new JCheckBox("set inverted");
  JSlider moveTo = new JSlider(0, 180, 90);

  RangeSlider mapInput = new RangeSlider();
  RangeSlider mapOutput = new RangeSlider();

  BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
  BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

  JComboBox<String> controller = new JComboBox<>();
  JComboBox<String> encoder = new JComboBox<>();
  JComboBox<String> pinList = new JComboBox<>();

  JTextField min = new JTextField("0");
  JTextField max = new JTextField("180");
  JTextField minOutput = new JTextField("0");
  JTextField maxOutput = new JTextField("180");

  JButton sweepButton = new JButton("sweep");

  JLabel enabledIcon = new JLabel();

  JLabel speedLabel = new JLabel("speed");

  // JSlider powerSlider = new JSlider(JSlider.VERTICAL, 0, 20, 4);
  JSlider speedSlider = new JSlider(0, 60, 60);

  JPanel controllerPanel;
  JPanel encoderPanel;
  JPanel enablePanel;
  JPanel speedPanel;

  JLabel speedUnits = new JLabel("degrees/s");

  JTextField maxSpeed = new JTextField("   ");
  JButton setMaxSpeed = new JButton("set");
  JLabel maxSpeedLabel = new JLabel("max speed");

  JTextField idleTime = new JTextField("3000");

  JLabel idleUnits = new JLabel(" ms");

  JLabel idleTimeLabel = new JLabel("idle time ");

  public HobbyServoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    // FIXME - even though its a pain - this should come from the
    // ServoController
    for (int i = 0; i < 54; i++) {
      pinList.addItem(i + "");
    }

    mapInput.setMinimum(0);
    mapInput.setMaximum(180);

    mapOutput.setMinimum(0);
    mapOutput.setMaximum(180);

    targetPos.setFont(targetPos.getFont().deriveFont(32.0f));
    targetPos.setHorizontalAlignment(JLabel.RIGHT);

    enabledIcon.setIcon(Util.getImageIcon("enabled.png"));
    currentPos.setFont(targetPos.getFont().deriveFont(32.0f));
    currentPos.setForeground(Color.LIGHT_GRAY);

    autoDisable.setSelected(false);
    setInverted.setSelected(false);

    moveTo.setForeground(Color.white);
    moveTo.setBackground(Color.DARK_GRAY);
    left.setForeground(Color.white);
    left.setBackground(Color.DARK_GRAY);
    right.setForeground(Color.white);
    right.setBackground(Color.DARK_GRAY);
    moveTo.setMajorTickSpacing(30);
    moveTo.setPaintTicks(true);
    moveTo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    moveTo.setPaintLabels(true);

    speedControl = speedControlTitle.getCheckBox();

    // JPanel north = new JPanel(new GridLayout(0, 3));
    // north.setLayout(new FlowLayout(FlowLayout., 0, 0));
    // JPanel controllerPanel = new JPanel(new GridLayout(0, 4));

    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));

    controllerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    controllerPanel.setBorder(BorderFactory.createTitledBorder("controller"));
    controllerPanel.add(attach);
    controllerPanel.add(controller);
    controllerPanel.add(new JLabel(" pin"));
    controllerPanel.add(pinList);

    encoderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    encoderPanel.setBorder(BorderFactory.createTitledBorder("encoder"));
    encoderPanel.add(attachEncoder);
    encoderPanel.add(encoder);

    enablePanel = new JPanel(new GridLayout(0, 2));
    enablePanel.setBorder(BorderFactory.createTitledBorder("enable"));

    enablePanel.add(enable);
    enablePanel.add(autoDisable);

    JPanel flow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    flow.add(idleTimeLabel);
    flow.add(idleTime);
    flow.add(idleUnits);
    enablePanel.add(new JLabel(" "));
    enablePanel.add(flow);

    setIdleTimeEnabled(false);

    speedPanel = new JPanel(new BorderLayout());
    speedPanel.setBorder(speedControlTitle);
    JPanel top = new JPanel();

    // top.add(powerControl);
    top.add(speedLabel);
    top.add(speed);
    top.add(maxSpeedLabel);
    top.add(maxSpeed);
    top.add(speedUnits);
    top.add(setMaxSpeed);

    // top.add(autoDisable);
    // JPanel flow = new JPanel();
    
    speedPanel.add(top, BorderLayout.NORTH);
    speedPanel.add(speedSlider, BorderLayout.CENTER);
    speedPanel.add(new JLabel("     "), BorderLayout.SOUTH);

    setSpeedControlEnabled(false);

    north.add(controllerPanel);
    north.add(encoderPanel);
    north.add(enablePanel);
    north.add(speedPanel);

    //////////////////////////

    south.setLayout(new GridLayout(0, 2));
    Border bordermap = BorderFactory.createTitledBorder("limits");
    south.setBorder(bordermap);
    south.add(mapInput);
    south.add(mapOutput);
    south.add(new JLabel("input map"));
    south.add(new JLabel("output map"));
    south.add(min);
    south.add(minOutput);
    south.add(max);
    south.add(maxOutput);
    south.add(save);
    // south.add(export);

    JPanel centerPanelStatus = new JPanel(new GridLayout(0, 5));
    centerPanelStatus.setBackground(Color.WHITE);
    centerPanelStatus.add(targetPos);
    centerPanelStatus.add(enabledIcon);
    centerPanelStatus.add(moving);
    centerPanelStatus.add(currentPos);
    // centerPanelStatus.add(powerSlider);

    center.setLayout(new GridLayout(0, 1));
    center.add(centerPanelStatus);
    center.add(moveTo);

    // FIXUP -----------------
    south.add(setInverted);
    south.add(sweepButton);
    south.add(new JSeparator(), BorderLayout.PAGE_END);
    south.add(restButton);
    south.add(rest);

    // FIXUP -----------------

    display.add(right, BorderLayout.EAST);
    display.add(left, BorderLayout.WEST);

    speedSlider.setEnabled(false);
    speedLabel.setEnabled(false);

    refreshControllers();
    refreshEncoders();
    addListeners();
  }

  private void setIdleTimeEnabled(boolean b) {
    idleTime.setEnabled(b);
    idleUnits.setEnabled(b);
    idleTimeLabel.setEnabled(b);
  }

  public void setSpeedControlEnabled(boolean b) {
    speed.setEnabled(b);
    maxSpeed.setEnabled(b);
    setMaxSpeed.setEnabled(b);
    maxSpeedLabel.setEnabled(b);
    speedSlider.setEnabled(b);
    speedLabel.setEnabled(b);
    speedUnits.setEnabled(b);
  }

  // SwingGui's action processing section - data from user
  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        Object o = event.getSource();

        if (o == speedControl) {
          if (speedControl.isSelected()) {
            setSpeedControlEnabled(true);
            if (lastSpeed != null) {
              speed.setText(lastSpeed + "");
            }
            send("setMaxSpeed", Double.parseDouble(maxSpeed.getText()));
          } else {
            setSpeedControlEnabled(false);
          }
        }

        if (o == attach) {
          if (attach.getText().equals("attach")) {
            // send("attach", controller.getSelectedItem(), (int)
            // pinList.getSelectedItem() + "", new Double(moveTo.getValue()));
            send("setPin", pinList.getSelectedItem()); // FIXME - get pinList
                                                       // from pinArrayControl
            send("attach", controller.getSelectedItem());
          } else {
            send("detach");
          }
          send("broadcastState");
          return;
        }

        if (o == enable) {
          if (enable.getText().equals("enable")) {
            send("enable");
            enable.setText("disable");
          } else {
            send("disable");
            enable.setText("enable");
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

        /**
         * <pre>
         * if (o == export) {
         *   send("saveCalibration");
         *   JOptionPane.showMessageDialog(null, "HobbyServo file generated");
         *   return;
         * }
         * </pre>
         */

        if (o == save) {
          send("map", Double.parseDouble(min.getText()), Double.parseDouble(max.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
          send("setVelocity", Double.parseDouble(speed.getText()));
          send("save");
          send("setRest", Double.parseDouble(rest.getText()));
          info("HobbyServo config saved !");
          return;
        }

        if (o == right) {
          moveTo.setValue(moveTo.getValue() + 1);
          return;
        }

        if (o == restButton) {
          send("setRest", Double.parseDouble(rest.getText()));
          send("moveTo", Double.parseDouble(rest.getText()));
          return;
        }

        if (o == left) {
          moveTo.setValue(moveTo.getValue() - 1);
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
      }
    });
  }

  @Override
  public void subscribeGui() {
    subscribe("publishMoveTo");
    subscribe("publishServoData");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishMoveTo");
    unsubscribe("publishServoData");
  }

  public void onMoveTo(final HobbyServo servo) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        targetPos.setText(servo.getTargetPos() + "");
      }
    });
  }

  public void onServoData(final ServoData data) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        currentPos.setText(data.pos + "");
        if (ServoStatus.SERVO_POSITION_UPDATE.equals(data.state)) {
          moving.setVisible(true);
        } else {
          moving.setVisible(false);
        }
      }
    });
  }

  synchronized public void onState(final HobbyServo servo) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        removeListeners();
        String controllerName = servo.getControllerName();
        lastController = controllerName;

        enabledIcon.setVisible(servo.isEnabled());

        maxSpeed.setText(servo.getMaxSpeed() == null ? "60.0" : servo.getMaxSpeed() + "");

        Double currentSpeed = servo.getVelocity();
        if (currentSpeed == null) {
          speed.setText("");
        } else {
          speed.setText(currentSpeed + "");
          lastSpeed = currentSpeed;
        }

        // refresh(); - infinite loop

        if (controllerName != null) {
          controller.setSelectedItem(controllerName);
        } else {

        }

        EncoderControl inEncoder = servo.getEncoder();
        if (inEncoder != null) {
          String encoderName = inEncoder.getName();
          if (encoderName != null) {
            encoder.setSelectedItem(encoderName);
            encoder.setEnabled(false);
            attachEncoder.setText("detach");
          }
        } else {
          encoder.setSelectedItem("");
          encoder.setEnabled(false);
          attachEncoder.setText("attach");
        }

        String servoPin = servo.getPin();

        if (servoPin != null)
          pinList.setSelectedItem(servoPin);
        if (servo.isAttached()) {
          attach.setText("detach");
          controller.setEnabled(false);
          pinList.setEnabled(false);
          moveTo.setEnabled(true);
        } else {
          attach.setText("attach");
          controller.setEnabled(true);
          pinList.setEnabled(true);
          moveTo.setEnabled(false);
        }

        if (servo.isEnabled()) {
          enable.setText("disable");
          enabledIcon.setVisible(true);
        } else {
          enable.setText("enable");
          enabledIcon.setVisible(false);
        }

        if (servo.getVelocity() != null) {

        } else {

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

        rest.setText(servo.getRest() + "");
        Double pos = servo.getPos();
        if (pos != null) {
          targetPos.setText(Double.toString(pos));
          moveTo.setValue(pos.intValue());
        }

        // In the inverted case, these are reversed
        moveTo.setMinimum(servo.getMin().intValue());
        moveTo.setMaximum(servo.getMax().intValue());

        speed.setText((servo.getSpeed() == null) ? "           " : servo.getSpeed() + "");

        if (mapInput.getLowValue() != servo.getMin().intValue()) {
          mapInput.setLowValue(servo.getMin().intValue());
        }

        if (mapInput.getHighValue() != servo.getMax().intValue()) {
          mapInput.setHighValue(servo.getMax().intValue());
        }

        double minOutputTmp = servo.getMinOutput();
        double maxOutputTmp = servo.getMaxOutput();

        if (servo.isInverted()) {
          minOutputTmp = servo.getMaxOutput();
          maxOutputTmp = servo.getMinOutput();
        }

        // FIXME - invert gui components so the next moveTo will not go crazy
        // !!!
        if (servo.isInverted() != mapOutput.getInverted()) {
          mapOutput.setInverted(servo.isInverted());
        }

        if (mapOutput.getLowValue() != servo.getMinOutput().intValue()) {
          mapOutput.setLowValue(servo.getMinOutput().intValue());
        }

        if (mapOutput.getHighValue() != servo.getMaxOutput().intValue()) {
          mapOutput.setHighValue(servo.getMaxOutput().intValue());
        }

        min.setText(servo.getMin() + "");
        max.setText(servo.getMax() + "");
        minOutput.setText(minOutputTmp + "");
        maxOutput.setText(maxOutputTmp + "");

        mapInput.setLowValue(servo.getMin().intValue());
        mapInput.setHighValue(servo.getMax().intValue());
        mapOutput.setLowValue(servo.getMinOutput().intValue());
        mapOutput.setHighValue(servo.getMaxOutput().intValue());

        addListeners();
      }
    });
  }

  public void enableSpeedControl(boolean b) {

  }

  public void refreshControllers() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        List<String> c = Runtime.getServiceNamesFromInterface(ServoController.class);
        controller.removeActionListener((HobbyServoGui) self);
        String currentControllerName = (String) controller.getSelectedItem();
        controller.removeAllItems();
        for (int i = 0; i < c.size(); ++i) {
          controller.addItem(c.get(i));
        }
        String controllerName = (currentControllerName != null) ? currentControllerName : lastController;
        controller.setSelectedItem(controllerName);
        controller.addActionListener((HobbyServoGui) self);
      }
    });
  }

  public void refreshEncoders() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        encoder.removeActionListener((HobbyServoGui) self);
        String currentEncoderName = (String) encoder.getSelectedItem();
        encoder.removeAllItems();
        List<String> c = Runtime.getServiceNamesFromInterface(EncoderControl.class);
        for (int i = 0; i < c.size(); ++i) {
          encoder.addItem(c.get(i));
        }
        String encoderName = (currentEncoderName != null) ? currentEncoderName : lastController;
        encoder.setSelectedItem(encoderName);
        encoder.addActionListener((HobbyServoGui) self);
      }
    });
  }

  public void removeListeners() {
    attach.removeActionListener(this);
    autoDisable.removeActionListener(this);
    controller.removeActionListener(this);
    enable.removeActionListener(this);
    left.removeActionListener(this);
    mapInput.removeChangeListener(this);
    mapOutput.removeChangeListener(this);
    moveTo.removeChangeListener(this);
    pinList.removeActionListener(this);
    speedControlTitle.removeActionListener(this);
    right.removeActionListener(this);
    save.removeActionListener(this);
    setInverted.removeActionListener(this);
    speedSlider.removeChangeListener(this);
    sweepButton.removeActionListener(this);
    restButton.removeActionListener(this);
  }

  public void addListeners() {
    attach.addActionListener(this);
    autoDisable.addActionListener(this);
    controller.addActionListener(this);
    enable.addActionListener(this);
    left.addActionListener(this);
    mapInput.addChangeListener(this);
    mapOutput.addChangeListener(this);
    moveTo.addChangeListener(this);
    pinList.addActionListener(this);
    speedControlTitle.addActionListener(this);
    right.addActionListener(this);
    save.addActionListener(this);
    setInverted.addActionListener(this);
    speedSlider.addChangeListener(this);
    sweepButton.addActionListener(this);
    restButton.addActionListener(this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    Object o = e.getSource();
    /* if (!((JSlider) o).getValueIsAdjusting()) */

    if (speedSlider.equals(o)) {
      // speedSlider.setVisible(true);
      send("setVelocity", (double) speedSlider.getValue());
      speed.setText(speedSlider.getValue() + "");
    }

    // isAdjusting prevent incremental values coming from the slider
    if (!((JSlider) o).getValueIsAdjusting()) {

      if (moveTo.equals(o)) {
        moving.setVisible(true);
        send("moveTo", (double) moveTo.getValue());
      }

      if (mapInput.equals(o)) {
        min.setText(String.format("%d", mapInput.getLowValue()));
        max.setText(String.format("%d", mapInput.getHighValue()));
        send("map", Double.parseDouble(min.getText()), Double.parseDouble(max.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
      }

      if (mapOutput.equals(o)) {
        if (mapOutput.getInverted()) {
          minOutput.setText(String.format("%d", mapOutput.getHighValue()));
          maxOutput.setText(String.format("%d", mapOutput.getLowValue()));
        } else {
          minOutput.setText(String.format("%d", mapOutput.getLowValue()));
          maxOutput.setText(String.format("%d", mapOutput.getHighValue()));
        }

        send("map", Double.parseDouble(min.getText()), Double.parseDouble(max.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
      }
    } // if adjusting
  }

  /**
   * call back to hand new services registered we want to update our list of
   * possible controllers & encoders
   * 
   * @param s
   */
  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    refreshEncoders();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Platform.setVirtual(false);

      // Runtime.start("webgui", "WebGui");
      Runtime.start("gui", "SwingGui");
      // EncoderControl encoder = (EncoderControl) Runtime.start("encoder", "TimeEncoderFactory");

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      if (mega.isVirtual()) {
        VirtualArduino vmega = mega.getVirtual();
        vmega.setBoardMega();
      }
      // mega.getBoardTypes();
      // mega.setBoardMega();
      // mega.setBoardUno();
      mega.connect("COM7");

      ServoControl servo = null;
      boolean useHobbyServo = false;

      if (useHobbyServo) {
        servo = (ServoControl) Runtime.start("hobbyservo", "HobbyServo");
      } else {
        servo = (ServoControl) Runtime.start("servo", "Servo");
      }

      // servo.load();
      servo.setPin(13);
      servo.setPosition(90.0);
      log.info("rest is {}", servo.getRest());
      servo.save();
      // servo.setPin(8);
      servo.attach(mega);
      // servo.attach(encoder);
      servo.moveTo(120.0);
      log.info("here");
      servo.moveTo(90.0);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
