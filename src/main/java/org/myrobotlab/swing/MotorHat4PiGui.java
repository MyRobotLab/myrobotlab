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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.service.MotorHat4Pi;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.swing.widget.ImageButton;

public class MotorHat4PiGui extends ServiceGui implements ActionListener, ChangeListener {

  public class FloatJSlider extends JSlider {

    private static final long serialVersionUID = 1L;
    final int scale;

    public FloatJSlider(int min, int max, int value, int scale) {
      super(min, max, value);
      this.scale = scale;

      Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
      labelTable.put(new Integer(min), new JLabel(String.format("%.2f", (float) min / scale)));
      labelTable.put(new Integer(min / 2), new JLabel(String.format("%.2f", (float) min / scale / 2)));
      labelTable.put(new Integer(value), new JLabel(String.format("%.2f", (float) value / scale)));
      labelTable.put(new Integer(max / 2), new JLabel(String.format("%.2f", (float) max / scale / 2)));
      labelTable.put(new Integer(max), new JLabel(String.format("%.2f", (float) max / scale)));
      setLabelTable(labelTable);
      setPaintTrack(false);
    }

    public float getScaledValue() {
      return ((float) super.getValue()) / this.scale;
    }
  }

  // controller
  JPanel controllerPanel = new JPanel(new BorderLayout());
  JComboBox<String> controllerList = new JComboBox<String>();
  MotorController controller = null;

  JCheckBox invert = new JCheckBox("invert");
  // power
  JPanel powerPanel = new JPanel(new BorderLayout());
  private FloatJSlider power = null;
  private JLabel powerValue = new JLabel("0.00");
  ImageButton stopButton;
  ImageButton clockwiseButton;
  ImageButton counterclockwiseButton;

  // TODO - make MotorPanel - for 1 motor - for shared embedded widget
  // TODO - stop sign button for panic stop
  // TODO - tighten up interfaces
  // TODO - DIRECT calls ! - motor & controller HAVE to be on the same
  // computer
  // TODO - cw ccw buttons enabled

  String attach = "attach";
  String detach = "detach";
  JButton attachButton = new JButton(attach);
  
  String setMotor = "setMotor";

  JComboBox<String> motorList = new JComboBox<String>();

  MotorHat4Pi myMotor;

  public MotorHat4PiGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    myMotor = (MotorHat4Pi) Runtime.getService(boundServiceName);

    // controllerPanel begin ------------------
    addTopLine(createFlowPanel("Controller", attachButton, "Controller", controllerList, "Motor", motorList));

    counterclockwiseButton = new ImageButton("Motor", "counterclockwise", this);
    stopButton = new ImageButton("Motor", "stop", this);
    clockwiseButton = new ImageButton("Motor", "clockwise", this);

    power = new FloatJSlider(-100, 100, 0, 100);
    power.setMajorTickSpacing(25);
    power.setPaintTicks(true);
    power.setPaintLabels(true);

    addLine(createFlowPanel("power", invert, powerValue, counterclockwiseButton, stopButton, clockwiseButton, power));

    refreshControllers();
    refreshMotorList();
    restoreListeners();
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    Object source = e.getSource();

    if (source == controllerList) {

      String newController = (String) controllerList.getSelectedItem();

      if (newController != null && newController.length() > 0) {
        refreshMotorList();
      }

    } else if (source == stopButton) {
      power.setValue(0);

    } else if (source == invert){
      myMotor.setInverted(invert.isSelected());
      
    } else if (source == attachButton) {
      if (attachButton.getText().equals(attach)) {
        myService.sendBlocking(boundServiceName, setMotor, motorList.getSelectedItem().toString());
        myService.send(boundServiceName, attach, controllerList.getSelectedItem());
        /*
        myMotor.setLeftPwmPin((int)Integer.decode(leftPwmPinList.getSelectedItem().toString()));
        myMotor.setRightPwmPin((int)Integer.decode(rightPwmPinList.getSelectedItem().toString()));
        try {
          myMotor.attach((String) controllerList.getSelectedItem());
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        */
      } else {
        myService.send(boundServiceName, detach, controllerList.getSelectedItem());
      }
    }

  }

  void refreshMotorList() {
    List<String> mbl = myMotor.motorList;
    for (int i = 0; i < mbl.size(); i++) {
      motorList.addItem(mbl.get(i));
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishChangePos");
    myService.send(boundServiceName, "publishState");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishChangePos", "onChangePos");
  }

  public void onState(MotorHat4Pi motor) {
    
    removeListeners();
    refreshControllers();

    setEnabled(motor.isAttached());
    
    motorList.setSelectedItem(motor.getMotorId());
    
    if (motor.isAttached()) {
      MotorController mc = (MotorController) motor.getController();
      controllerList.setSelectedItem(mc.getName());
      attachButton.setText(detach);
      controllerList.setEnabled(false);
      motorList.setEnabled(false);
      powerValue.setText(String.format("in %3.2f out %3.0f", power.getScaledValue(), myMotor.getPowerLevel()));
    } else {
      attachButton.setText(attach);
      controllerList.setEnabled(true);
      motorList.setEnabled(true);
    }
    invert.setSelected(motor.isInverted());

    restoreListeners();
  }

  public void setEnabled(boolean enable) {
    stopButton.setEnabled(enable);
    clockwiseButton.setEnabled(enable);
    counterclockwiseButton.setEnabled(enable);
    power.setEnabled(enable);
    invert.setEnabled(enable);
    powerValue.setEnabled(enable);

  }

  @Override
  public void stateChanged(ChangeEvent ce) {
    Object source = ce.getSource();
    if (power == source) {
      powerValue.setText(String.format("in %3.2f out %3.0f", power.getScaledValue(), myMotor.getPowerLevel()));
      myService.send(boundServiceName, "move", power.getScaledValue());
      // log.info(String.format("send %s, move, %s", boundServiceName, power.getScaledValue()));
    }
  }

  public void refreshControllers() {
    if (myMotor != null) {
      List<String> v = myMotor.refreshControllers();
      controllerList.removeAllItems();
      for (int i = 0; i < v.size(); ++i) {
        controllerList.addItem(v.get(i));
      }
      if (myMotor.getController() != null) {
        controllerList.setSelectedItem(myMotor.getController().getName());
      }
    }
  }

  public void removeListeners() {
    attachButton.removeActionListener(this);
    controllerList.removeActionListener(this);
    motorList.removeActionListener(this);
    power.removeChangeListener(this);
    invert.removeActionListener(this);
  }

  public void restoreListeners() {
    attachButton.addActionListener(this);
    controllerList.addActionListener(this);
    motorList.addActionListener(this);
    power.addChangeListener(this);
    invert.addActionListener(this);
  }

}
