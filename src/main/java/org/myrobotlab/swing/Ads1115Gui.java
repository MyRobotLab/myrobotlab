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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Ads1115;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class Ads1115Gui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Ads1115Gui.class);

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

  JLabel adc0 = new JLabel();
  JLabel adc1 = new JLabel();
  JLabel adc2 = new JLabel();
  JLabel adc3 = new JLabel();

  Ads1115 boundService = null;

  public Ads1115Gui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    boundService = (Ads1115) Runtime.getService(boundServiceName);

    // addTopLine(controllerLabel, controllerList, deviceBusLabel, deviceBusList, deviceAddressLabel, deviceAddressList, attachButton);
    addTopLine(createFlowPanel("input", attachButton, "Controller", controllerList, "Bus", deviceBusList, "Address", deviceAddressList, refresh));

    JPanel center = new JPanel();
    center.add(new JLabel("Adc0: "));
    center.add(adc0);

    center.add(new JLabel("Adc1: "));
    center.add(adc1);

    center.add(new JLabel("Adc2: "));
    center.add(adc2);

    center.add(new JLabel("Adc3: "));
    center.add(adc3);

    display.add(north, BorderLayout.NORTH);
    display.add(center, BorderLayout.CENTER);

    refreshControllers();
    getDeviceBusList();
    getDeviceAddressList();
    restoreListeners();

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    log.info("Ads1115GUI actionPerformed");
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
      // FIXME !! replace with PortGui
      myService.send(boundServiceName, "refresh");
    }
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  public void onState(Ads1115 service) {

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

    adc0.setText(String.format("%s", service.adc0));
    adc1.setText(String.format("%s", service.adc1));
    adc2.setText(String.format("%s", service.adc2));
    adc3.setText(String.format("%s", service.adc3));
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
