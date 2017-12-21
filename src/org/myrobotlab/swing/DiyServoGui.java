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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

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
import org.myrobotlab.service.DiyServo;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

import com.jidesoft.swing.RangeSlider;

/**
 * DiyServo SwingGui - displays details of Servo state Lesson learned ! Servos
 * to properly function need to be attached to a controller This gui previously
 * sent messages to the controller. To simplify things its important to send
 * messages only to the bound Servo - and let it attach to the controller versus
 * sending messages directly to the controller. 1 display - 1 service - keep it
 * simple
 *
 */
public class DiyServoGui extends ServiceGui implements ActionListener {

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
      minInput.setText((double) mapInputSlider.getLowValue() + "");
      maxInput.setText((double) mapInputSlider.getHighValue() + "");
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
        myServo.map(Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()), Double.parseDouble(minOutput.getText()),
            Double.parseDouble(maxOutput.getText()));
      } else {
        log.error("can not send message myService is null");
      }
    }
  }

  private class MapOutputSliderListener implements ChangeListener, MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
      if (mapOutputSlider.getInverted()) {
        minOutput.setText((double) mapOutputSlider.getHighValue() + "");
        maxOutput.setText((double) mapOutputSlider.getLowValue() + "");
      } else {
        minOutput.setText((double) mapOutputSlider.getLowValue() + "");
        maxOutput.setText((double) mapOutputSlider.getHighValue() + "");

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
      // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (myService != null) {
        myServo.map(Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()), Double.parseDouble(minOutput.getText()),
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
  JTextField maxVelocity = new JTextField("-1");
  JLabel disableDelayGraceL = new JLabel("Extra delay ( ms ): ");

  JTextField disableDelayGrace = new JTextField("1000");

  JButton setMaxVelocity = new JButton("set");
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

  final String attachAnalog = "attach";
  final String detachAnalog = "detach";
  JComboBox<String> pinArrayControlList = new JComboBox<String>();
  JComboBox<Integer> analogInputPinList = new JComboBox<Integer>();
  JButton attachListenerButton = new JButton(attachAnalog);

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

  DiyServo myServo = null;
  boolean eventsEnabled;

  public DiyServoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    myServo = (DiyServo) Runtime.getService(boundServiceName);

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

    maxVelocity.setPreferredSize(new Dimension(50, 24));
    maxVelocity.setSize(new Dimension(50, 24));

    disableDelayGrace.setPreferredSize(new Dimension(40, 24));
    boundPos.setFont(boundPos.getFont().deriveFont(32.0f));
    boundPos.setHorizontalAlignment(JLabel.RIGHT);
    imageenabled.setIcon(enabled);
    velocityPic.setIcon(velocityPng);
    autoDisable.setSelected(false);
    setInverted.setSelected(false);
    disableDelayGraceL.setFont(new Font("Arial", Font.BOLD, 10));

    // not yet implemented
    /// setMaxVelocity.setEnabled(false);
    // enableButton.setEnabled(false);
    // autoDisable.setEnabled(false);
    // setDisableDelays.setEnabled(false);
    // sweepButton.setEnabled(false);
    eventsButton.setEnabled(false);

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

    setMaxVelocity.addActionListener(this);
    updateMinMaxButton.addActionListener(this);
    updateMapButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);
    attachButton.addActionListener(this);
    enableButton.addActionListener(this);
    autoDisable.addActionListener(this);
    setInverted.addActionListener(this);
    sweepButton.addActionListener(this);
    eventsButton.addActionListener(this);
    setDisableDelays.addActionListener(this);

    // addTopLeft(2, boundPos, 3, s,velocity,setMaxVelocity );

    JPanel controllerP = new JPanel();
    Border borderController = BorderFactory.createTitledBorder("Analog input");
    controllerP.setBorder(borderController);
    JLabel pinArrayControlListlabel = new JLabel("Controller : ");
    JLabel analogInputPinListabel = new JLabel("Pin : ");

    controllerP.add(pinArrayControlListlabel);
    controllerP.add(pinArrayControlList);
    controllerP.add(analogInputPinListabel);
    controllerP.add(analogInputPinList);
    controllerP.add(attachListenerButton);

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
    powerMain.add(disableDelayGrace);
    // powerMain.add(powerMainSub);

    JPanel extra = new JPanel(new GridLayout(1, 1));
    Border settingsborder = BorderFactory.createTitledBorder("Extra :");
    extra.setBorder(settingsborder);
    JPanel sweep = new JPanel();
    sweep.add(setInverted);
    sweep.add(sweepButton);
    sweep.add(eventsButton);
    sweep.setBackground(Color.WHITE);

    JPanel velocityP = new JPanel(new GridLayout(2, 1));
    Border borderVelocityP = BorderFactory.createTitledBorder("Max Velocity ( poc ) :");
    velocityP.setBorder(borderVelocityP);
    velocityP.setBackground(Color.WHITE);

    JPanel velocityPicP = new JPanel();
    velocityPicP.add(velocityPic);
    velocityPicP.setBackground(Color.WHITE);

    JPanel velocitySetings = new JPanel();
    velocitySetings.add(maxVelocity);
    velocitySetings.add(setMaxVelocity);
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

        if (o == pinArrayControlList) {
          String pinControlName = (String) pinArrayControlList.getSelectedItem();
          myServo.pinControlName = pinControlName;
          refreshAnalogPinList();
          log.debug(String.format("pinArrayControList event %s", pinControlName));
        }

        if (o == attachListenerButton) {
          if (attachListenerButton.getText().equals(attachAnalog)) {
            send("attach", pinArrayControlList.getSelectedItem(), analogInputPinList.getSelectedItem());
          } else {
            send("detach", pinArrayControlList.getSelectedItem());
          }
          return;
        }

        if (o == setMaxVelocity) {
          send("setMaxVelocity", Double.parseDouble(maxVelocity.getText()));
          return;
        }

        if (o == enableButton) {
          if (enableButton.getText().equals("enable")) {
            send("enable");

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

          try {
            delayIfV = Integer.parseInt(disableDelayGrace.getText());
          } catch (Exception e) {
            warn("Bad value for disableDelay !");
          }

          send("setDisableDelayGrace", delayIfV);
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

  synchronized public void onState(final DiyServo servo) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        removeListeners();
        refreshControllers();

        if (servo.isPinArrayControlSet()) {
          attachListenerButton.setText(detachAnalog);
          attachListenerButton.setEnabled(true);
          pinArrayControlList.setEnabled(false);
          analogInputPinList.setEnabled(false);
        } else {
          attachListenerButton.setText(attachAnalog);
          pinArrayControlList.setEnabled(true);
          analogInputPinList.setEnabled(true);
          if ((pinArrayControlList.getSelectedItem() != null) && (analogInputPinList.getSelectedItem()) != null) {
            attachListenerButton.setEnabled(true);
          } else
            attachListenerButton.setEnabled(false);
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
        maxVelocity.setText(servo.getMaxVelocity() + "");

        disableDelayGrace.setText(servo.disableDelayGrace + "");

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

  public void refreshControllers() {
    // Refresh the list of Analog inputs
    pinArrayControlList.removeAllItems();
    List<String> a = myServo.pinArrayControls;
    for (int i = 0; i < a.size(); ++i) {
      pinArrayControlList.addItem(a.get(i));
    }
    pinArrayControlList.setSelectedItem(myServo.pinControlName);

    // Refresh the list of Pins inputs
    refreshAnalogPinList();

  }

  public void refreshAnalogPinList() {

    // Refresh the list of Pins inputs
    analogInputPinList.removeAllItems();
    if (myServo.pinControlName != null) {
      PinArrayControl tmpControl = (PinArrayControl) Runtime.getService(myServo.pinControlName);
      if (tmpControl != null) {
        List<PinDefinition> mbl = tmpControl.getPinList();
        for (int i = 0; i < mbl.size(); i++) {
          PinDefinition pinData = mbl.get(i);
          // Removed the filtering on pins, because the Arduino logic for the
          // different is not complete
          // if (pinData.isAnalog()){
          analogInputPinList.addItem(pinData.getAddress());
          // }
        }
      }
      analogInputPinList.setSelectedItem(myServo.pin);
    }
  }

  public void removeListeners() {
    attachListenerButton.removeActionListener(this);
    pinArrayControlList.removeActionListener(this);
    slider.removeChangeListener(sliderListener);
    slider.removeMouseListener(sliderListener);
    mapInputSlider.removeChangeListener(mapInputSliderListener);
    mapInputSlider.removeMouseListener(mapInputSliderListener);
    mapOutputSlider.removeMouseListener(mapInputSliderListener);
    mapOutputSlider.removeChangeListener(mapOutputSliderListener);
  }

  public void restoreListeners() {
    attachListenerButton.addActionListener(this);
    pinArrayControlList.addActionListener(this);
    slider.addChangeListener(sliderListener);
    slider.addMouseListener(sliderListener);
    mapInputSlider.addChangeListener(mapInputSliderListener);
    mapInputSlider.addMouseListener(mapInputSliderListener);
    mapOutputSlider.addChangeListener(mapOutputSliderListener);
    mapOutputSlider.addMouseListener(mapOutputSliderListener);
  }
}