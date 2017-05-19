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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

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

  private class SliderListener implements ChangeListener {
    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

      boundPos.setText(String.format("%d", slider.getValue()));

      if (myService != null) {
        myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
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
  JButton updateLimitsButton = new JButton("update");

  JSlider slider = new JSlider(0, 180, 90);
  BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
  BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

  JComboBox<String> controller = new JComboBox<String>();
  JComboBox<Integer> pinList = new JComboBox<Integer>();

  JTextField posMin = new JTextField("0");
  JTextField posMax = new JTextField("180");

  // Servo myServox = null;

  SliderListener sliderListener = new SliderListener();

  public ServoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    // myServo = (Servo) Runtime.getService(boundServiceName);

    for (int i = 0; i < 54; i++) {
      pinList.addItem(i);
    }

    updateLimitsButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);
    controller.addActionListener(this);
    attachButton.addActionListener(this);
    pinList.addActionListener(this);
    boundPos.setFont(boundPos.getFont().deriveFont(32.0f));

    JPanel s = new JPanel();
    s.add(left);
    s.add(slider);
    s.add(right);
    addTop(2, boundPos, 3, s);
    // addLine(s);
    addTop("controller:", controller, "   pin:", pinList, attachButton);
    addTop("min:", posMin, "   max:", posMax, updateLimitsButton);

    refreshControllers();
  }

  // SwingGui's action processing section - data from user
  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
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

        if (o == attachButton) {
          if (attachButton.getText().equals("attach")) {
            send("attach", controller.getSelectedItem(), (int) pinList.getSelectedItem(), new Double(slider.getValue()));
          } else {
            lastControllerUsed = (String)controller.getSelectedItem();
            send("detach", controller.getSelectedItem());
          }
          return;
        }

        if (o == updateLimitsButton) {
          send("setMinMax", Integer.parseInt(posMin.getText()), Integer.parseInt(posMax.getText()));
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

        restoreListeners();
      }
    });

  }

  public void onRefreshControllers(final ArrayList<String> c) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        controller.removeActionListener((ServoGui)self);
        String currentControllerName = (String)controller.getSelectedItem();
        controller.removeAllItems();
        for (int i = 0; i < c.size(); ++i) {
          controller.addItem(c.get(i));
        }
        String controllerName = (currentControllerName != null)?currentControllerName:lastControllerUsed;
        controller.setSelectedItem(controllerName);
        controller.addActionListener((ServoGui)self);
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
  }

  public void restoreListeners() {
    controller.addActionListener(this);
    pinList.addActionListener(this);
    slider.addChangeListener(sliderListener);
  }

}