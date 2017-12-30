/**
 *                    
 * @author Mats (at) myrobotlab.org
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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Mpu6050;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class Mpu6050Gui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mpu6050Gui.class);

  String attach = "attach";
  String detach = "detach";
  JButton attachButton = new JButton(attach);

  JComboBox<String> controllerList = new JComboBox<String>();
  JComboBox<String> deviceAddressList = new JComboBox<String>();
  JComboBox<String> deviceBusList = new JComboBox<String>();

  JLabel controllerLabel = new JLabel("Controller");
  JLabel deviceBusLabel = new JLabel("Bus");
  JLabel deviceAddressLabel = new JLabel("Address");

  JButton refresh = new JButton("refresh");

  JLabel accelX = new JLabel();
  JLabel accelY = new JLabel();
  JLabel accelZ = new JLabel();
  JLabel temperature = new JLabel();
  JLabel gyroX = new JLabel();
  JLabel gyroY = new JLabel();
  JLabel gyroZ = new JLabel();

  Mpu6050 boundService = null;

  public Mpu6050Gui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    boundService = (Mpu6050) Runtime.getService(boundServiceName);

    // addTopLine(controllerLabel, controllerList, deviceBusLabel, deviceBusList, deviceAddressLabel, deviceAddressList, attachButton);
    addTopLine(createFlowPanel("input", attachButton, "Controller", controllerList, "Bus", deviceBusList, "Address", deviceAddressList, refresh));

    JPanel measures = new JPanel();
    measures.add(accelX);
    measures.add(accelY);
    measures.add(accelZ);

    measures.add(temperature);

    measures.add(gyroX);
    measures.add(gyroY);
    measures.add(gyroZ);

    center.add(measures);

    refreshControllers();
    getDeviceBusList();
    getDeviceAddressList();
    restoreListeners();

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == attachButton) {
      if (attachButton.getText().equals(attach)) {
        int index = controllerList.getSelectedIndex();
        if (index != -1) {
          myService.send(boundServiceName, attach, controllerList.getSelectedItem(), deviceBusList.getSelectedItem(), deviceAddressList.getSelectedItem());
        }
      } else {
        log.info(String.format("detach %s", controllerList.getSelectedItem()));
        myService.send(boundServiceName, detach, controllerList.getSelectedItem());
      }
    }
    if (o == refresh) {
      myService.send(boundServiceName, "refresh");
    }
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  public void onState(Mpu6050 service) {

    removeListeners();
    refreshControllers();
    if (service.controller != null) {
      controllerList.setSelectedItem(service.controllerName);
      deviceBusList.setSelectedItem(service.deviceBus);
      deviceAddressList.setSelectedItem(service.deviceAddress);
    }
    if (service.isAttached) {
      attachButton.setText(detach);
      controllerList.setEnabled(false);
      deviceBusList.setEnabled(false);
      deviceAddressList.setEnabled(false);
      refresh.setEnabled(true);
    } else {
      attachButton.setText(attach);
      controllerList.setEnabled(true);
      deviceBusList.setEnabled(true);
      deviceAddressList.setEnabled(true);
      refresh.setEnabled(false);
    }
    restoreListeners();

    accelX.setText(String.format("AccelX: %.3f G", service.accelGX));
    accelY.setText(String.format("AccelY: %.3f G", service.accelGY));
    accelZ.setText(String.format("AccelZ: %.3f G", service.accelGZ));
    temperature.setText(String.format("Temperature: %.3f degrees Celicus", service.temperatureC));
    gyroX.setText(String.format("GyroX : %.3f degrees/s", service.gyroDegreeX));
    gyroY.setText(String.format("GyroY : %.3f degrees/s", service.gyroDegreeY));
    gyroZ.setText(String.format("GyroZ : %.3f degrees/s", service.gyroDegreeZ));

  }

  public void getDeviceBusList() {
    List<String> mbl = boundService.deviceBusList;
    for (int i = 0; i < mbl.size(); i++) {
      deviceBusList.addItem(mbl.get(i));
    }
  }

  public void getDeviceAddressList() {

    List<String> mal = boundService.deviceAddressList;
    for (int i = 0; i < mal.size(); i++) {
      deviceAddressList.addItem(mal.get(i));
    }
  }

  public void refreshControllers() {
    List<String> v = boundService.refreshControllers();
    controllerList.removeAllItems();
    for (int i = 0; i < v.size(); ++i) {
      controllerList.addItem(v.get(i));
    }
    if (boundService.controller != null) {
      controllerList.setSelectedItem(boundService.controller.getName());
    }
  }

  public void removeListeners() {
    attachButton.removeActionListener(this);
    refresh.removeActionListener(this);
  }

  public void restoreListeners() {
    attachButton.addActionListener(this);
    refresh.addActionListener(this);
  }

}
