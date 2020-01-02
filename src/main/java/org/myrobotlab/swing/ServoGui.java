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
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
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
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.TestCatcher;
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
 * Servo SwingGui - displays details of Servo state Lesson learned !
 * Servos to properly function need to be attached to a controller This gui
 * previously sent messages to the controller. To simplify things its important
 * to send messages only to the bound Servo - and let it attach to the
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
public class ServoGui extends ServiceGui implements ActionListener, ChangeListener {
  static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(ServoGui.class);

  String lastController;

  Double lastSpeed = 60.0; // a ui "guess"

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
  CheckBoxTitledBorder blockingTitle = new CheckBoxTitledBorder("blocking", false);

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

  JTextField minPos = new JTextField("0");
  JTextField maxPos = new JTextField("180");
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

  // FIXME - this should be read default form servo !!!
  JTextField idleTime = new JTextField("3000");

  JLabel idleUnits = new JLabel(" ms");

  JLabel idleTimeLabel = new JLabel("idle time ");

  private JCheckBox blocking;

  public ServoGui(final String boundServiceName, final SwingGui myService) throws IOException {
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
    blocking = blockingTitle.getCheckBox();

    // JPanel north = new JPanel(new GridLayout(0, 3));
    // north.setLayout(new FlowLayout(FlowLayout., 0, 0));
    // JPanel controllerPanel = new JPanel(new GridLayout(0, 4));

    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));

    JPanel controllerMainPanel = new JPanel(new GridLayout(0, 1));

    controllerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    controllerPanel.setBorder(BorderFactory.createTitledBorder("controller"));
    controllerPanel.add(attach);
    controllerPanel.add(controller);
    controllerPanel.add(new JLabel(" pin"));
    controllerPanel.add(pinList);
    controllerMainPanel.add(controllerPanel);

    encoderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    encoderPanel.setBorder(BorderFactory.createTitledBorder("encoder"));
    encoderPanel.add(attachEncoder);
    encoderPanel.add(encoder);
    controllerMainPanel.add(encoderPanel);

    JPanel blockingPanel = new JPanel();
    // blockingPanel.setBorder(BorderFactory.createTitledBorder("blocking"));
    blockingPanel.setBorder(blockingTitle);
    ImageIcon icon = new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "green.png")));
    JLabel isBlocking = new JLabel();
    isBlocking.setIcon(icon);
    blockingPanel.add(new JLabel("is blocking"));
    blockingPanel.add(isBlocking);

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
    JPanel top = new JPanel(new GridLayout(0, 6));

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

    north.add(controllerMainPanel);
    north.add(blockingPanel);
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
    south.add(minPos);
    south.add(minOutput);
    south.add(maxPos);
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
    speedControl.setSelected(b);
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
              speed.setText(String.format("%.1f", lastSpeed));
              send("setSpeed", Double.parseDouble(String.format("%.1f", lastSpeed)));
            }
          } else {
            setSpeedControlEnabled(false);
            // disabling speed control
            send("setSpeed", (Double) null);
          }
          send("broadcastState");
        }

        if (o == setMaxSpeed) {
          send("setMaxSpeed", Double.parseDouble(maxSpeed.getText()));
          send("setSpeed", Double.parseDouble(speed.getText()));
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

        // The correct way to do "command" msgs
        // these never set gui attributes like .setText("x")
        // because all those functions are in onState ... instead
        // we send a command - then send a broadcast request - and onState
        // will do the appropriate display
        if (o == enable) {
          if (enable.getText().equals("enable")) {
            send("enable");
          } else {
            send("disable");
          }
          send("broadcastState");
          return;
        }

        if (o == autoDisable) {
          if (autoDisable.isSelected()) {
            send("setAutoDisable", true);
          } else {
            send("setAutoDisable", false);
          }
          send("broadcastState");
          return;
        }

        if (o == setInverted) {
          if (setInverted.isSelected()) {
            send("setInverted", true);
          } else {
            send("setInverted", false);
          }
          send("broadcastState");
          return;
        }

        /**
         * <pre>
         * if (o == export) {
         *   send("saveCalibration");
         *   JOptionPane.showMessageDialog(null, "Servo file generated");
         *   return;
         * }
         * </pre>
         */

        if (o == save) {
          send("map", Double.parseDouble(minPos.getText()), Double.parseDouble(maxPos.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
          send("setVelocity", Double.parseDouble(speed.getText()));
          send("save");
          send("setRest", Double.parseDouble(rest.getText()));
          info("Servo config saved !");
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

  /**
   * publish of the "moveTo" from servo
   * 
   * @param servo
   */
  public void onMoveTo(final Servo servo) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        targetPos.setText(String.format("%.1f", servo.getTargetPos()));
      }
    });
  }

  public void onServoData(final ServoData data) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        currentPos.setText(String.format("%.1f", data.pos));
        if (ServoStatus.SERVO_POSITION_UPDATE.equals(data.state)) {
          moving.setVisible(true);
        } else {
          moving.setVisible(false);
        }
      }
    });
  }

  synchronized public void onState(final Servo servo) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        removeListeners();

        currentPos.setText(String.format("%.1f", servo.getPos()));

        // FIXME - Servo supports multiple controllers - the UI needs a
        // multi-select perhaps
        String controllerName = null;
        for (String controller : servo.getControllers()) {
        lastController = controller;
        controllerName = controller;
        }

        moving.setVisible(servo.isMoving());

        enabledIcon.setVisible(servo.isEnabled());

        double maxSpd = (servo.getMaxSpeed() == null) ? 500.0 : servo.getMaxSpeed();
        maxSpeed.setText(String.format("%.1f", maxSpd));
        speedSlider.setMaximum((int) maxSpd);

        Double currentSpeed = servo.getSpeed();
        if (currentSpeed == null) {
          speed.setText("");
        } else {
          speed.setText(String.format("%.1f", currentSpeed));
          speedSlider.setValue(currentSpeed.intValue());
          lastSpeed = currentSpeed;
        }

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
        

        if (servo.isEnabled()) {
          enable.setText("disable");
          enabledIcon.setVisible(true);
        } else {
          enable.setText("enable");
          enabledIcon.setVisible(false);
        }

        if (servo.getAutoDisable()) {
          autoDisable.setSelected(true);
          setIdleTimeEnabled(true);
        } else {
          autoDisable.setSelected(false);
          setIdleTimeEnabled(false);
        }
        
        if (servo.isSweeping()) {
          sweepButton.setText("stop");
        } else {
          sweepButton.setText("sweep");
        }

        if (servo.isInverted()) {
          setInverted.setSelected(true);
        } else {
          setInverted.setSelected(false);
        }

        rest.setText(String.format("%.1f", servo.getRest()));

        // TARGET POSITION
        // target position - is a "command" where I "want" to go - not to be
        // confused with "where I am"
        Double inTargetPos = servo.getTargetPos();
        if (inTargetPos != null) {
          targetPos.setText(Double.toString(inTargetPos));
          moveTo.setValue(inTargetPos.intValue());
        }

        // SPEED CONTROL
        Double servoSpeed = servo.getSpeed();
        if (servoSpeed == null) {
          setSpeedControlEnabled(false);
        } else {
          speed.setText(String.format("%.1f", servoSpeed));
          setSpeedControlEnabled(true);
        }

        // MAP MIN/MAX INPUT/OUTPUT
        // In the inverted case, these are reversed
        moveTo.setMinimum(servo.getMin().intValue());
        moveTo.setMaximum(servo.getMax().intValue());

        if (mapInput.getLowValue() != servo.getMin().intValue()) {
          mapInput.setLowValue(servo.getMin().intValue());
        }

        if (mapInput.getHighValue() != servo.getMax().intValue()) {
          mapInput.setHighValue(servo.getMax().intValue());
        }


        // FIXME - invert gui components so the next moveTo will not go crazy
        // !!!
        if (servo.isInverted() != mapOutput.getInverted()) {
          mapOutput.setInverted(servo.isInverted());
        }

      

        minPos.setText(String.format("%.1f", servo.getMin()));
        maxPos.setText(String.format("%.1f", servo.getMax()));
       
        mapInput.setLowValue(servo.getMin().intValue());
        mapInput.setHighValue(servo.getMax().intValue());
       
        addListeners();
      }
    });
  }

  public void refreshControllers() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        List<String> c = Runtime.getServiceNamesFromInterface(ServoController.class);
        controller.removeActionListener((ServoGui) self);
        String currentControllerName = (String) controller.getSelectedItem();
        controller.removeAllItems();
        for (int i = 0; i < c.size(); ++i) {
          controller.addItem(c.get(i));
        }
        String controllerName = (currentControllerName != null) ? currentControllerName : lastController;
        controller.setSelectedItem(controllerName);
        controller.addActionListener((ServoGui) self);
      }
    });
  }

  public void refreshEncoders() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        encoder.removeActionListener((ServoGui) self);
        String currentEncoderName = (String) encoder.getSelectedItem();
        encoder.removeAllItems();
        List<String> c = Runtime.getServiceNamesFromInterface(EncoderControl.class);
        for (int i = 0; i < c.size(); ++i) {
          encoder.addItem(c.get(i));
        }
        // add self for the default time encoder (even though its not a
        // "service")
        encoder.addItem(boundServiceName);
        String encoderName = (currentEncoderName != null) ? currentEncoderName : lastController;
        encoder.setSelectedItem(encoderName);
        encoder.addActionListener((ServoGui) self);
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
    setMaxSpeed.removeActionListener(this);
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
    setMaxSpeed.addActionListener(this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    Object o = e.getSource();
    /* if (!((JSlider) o).getValueIsAdjusting()) */

    if (speedSlider.equals(o)) {
      // speedSlider.setVisible(true);
      send("setVelocity", (double) speedSlider.getValue());
      speed.setText(String.format("%.1f", (double) speedSlider.getValue()));
    }

    if (moveTo.equals(o)) {
      // moving.setVisible(true);
      send("moveTo", (double) moveTo.getValue());
    }

    // isAdjusting prevent incremental values coming from the slider
    if (!((JSlider) o).getValueIsAdjusting()) {

      if (mapInput.equals(o)) {
        minPos.setText(String.format("%d", mapInput.getLowValue()));
        maxPos.setText(String.format("%d", mapInput.getHighValue()));
        send("map", Double.parseDouble(minPos.getText()), Double.parseDouble(maxPos.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
      }

      if (mapOutput.equals(o)) {
        if (mapOutput.getInverted()) {
          minOutput.setText(String.format("%d", mapOutput.getHighValue()));
          maxOutput.setText(String.format("%d", mapOutput.getLowValue()));
        } else {
          minOutput.setText(String.format("%d", mapOutput.getLowValue()));
          maxOutput.setText(String.format("%d", mapOutput.getHighValue()));
        }

        send("map", Double.parseDouble(minPos.getText()), Double.parseDouble(maxPos.getText()), Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
      }
    } // if adjusting
  }

  /**
   * call back to hand new services registered we want to update our list of
   * possible controllers and encoders
   * 
   * @param s
   */
  public void onRegistered(Registration s) {
    refreshControllers();
    refreshEncoders();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      log.info("{}", Serial.getPorts());
      Platform.setVirtual(false);
      String port = "COM7";
      int pin = 22;
      boolean useHobbyServo = true;

      // Runtime.start("webgui", "WebGui");
      SwingGui gui = (SwingGui) Runtime.start("gui", "SwingGui");
      // EncoderControl encoder = (EncoderControl) Runtime.start("encoder",
      // "TimeEncoderFactory");
      // FIXME - perhaps InMoov should just override the framework to provided
      // the exception needed to work
      // Runtime.getInstance().startPeers();

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      ServoControl servo = null;

      servo = (ServoControl) Runtime.start("servo", "Servo");
      Service.sleep(500);
      gui.setActiveTab("servo");

      // FIXME - check mixing and matching speed autoDisable enable/disable
      if (mega.isVirtual()) {
        VirtualArduino vmega = mega.getVirtual();
        vmega.setBoardMega();
      }
      // mega.getBoardTypes();
      // mega.setBoardMega();
      // mega.setBoardUno();
      mega.connect(port);

      // servo.load();
      servo.setPin(pin);
      // servo.setPosition(90.0);
      log.info("rest is {}", servo.getRest());
      // servo.save();

      // servo.setPin(8);
      servo.attach(mega);
      // servo.attach(encoder);
      servo.moveTo(10.3);
      servo.moveTo(110.3);
      servo.moveToBlocking(113.0);
      servo.setSpeed(2.0);
      servo.moveTo(140.0);
      // Service.sleep(500);
      // servo.moveTo(90.0);
      // Service.sleep(1000);
      // String python = LangUtils.toPython();
      // Files.write(Paths.get("export.py"), python.toString().getBytes());
      TestCatcher catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
      /// servo.attach((ServoDataListener) catcher);

      catcher.exportAll("export.py");

      // FIXME - junit for testing return values of moveTo when a blocking call
      // is in progress

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
