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

package org.myrobotlab.control;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Mpu6050;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class Mpu6050GUI extends ServiceGUI implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mpu6050GUI.class.getCanonicalName());

  String attach = "setController";
	String detach = "unsetController";
	JButton attachButton = new JButton(attach);

	JComboBox<String> controller = new JComboBox<String>();
	JComboBox<String> deviceAddressList = new JComboBox<String>();
	JComboBox<String> deviceBusList = new JComboBox<String>();

	JLabel controllerLabel     = new JLabel("Controller");
	JLabel deviceBusLabel     = new JLabel("Bus");
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
  
  public Mpu6050GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
    super(boundServiceName, myService, tabs);
		boundService = (Mpu6050) Runtime.getService(boundServiceName);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == refresh) {
      myService.send(boundServiceName, "refresh");
    }
		if (o == attachButton) {
			if (attachButton.getText().equals(attach)) {
				int index = controller.getSelectedIndex();
				if (index != -1) {
					myService.send(boundServiceName, attach, 
							controller.getSelectedItem(),
							deviceBusList.getSelectedItem(),
							deviceAddressList.getSelectedItem());
				}
			} else {
				myService.send(boundServiceName, detach);
			}
		}
  }

  @Override
  public void attachGUI() {

    subscribe("publishState", "getState", Mpu6050.class);
    send("publishState");
  }

  @Override
  public void detachGUI() {

    unsubscribe("publishState", "getState", Mpu6050.class);
  }

  public void getState(Mpu6050 mpu6050) {
  	
		refreshControllers();
		controller.setSelectedItem(mpu6050.getControllerName());
		deviceBusList.setSelectedItem(mpu6050.deviceBus);
		deviceAddressList.setSelectedItem(mpu6050.deviceAddress);
		if (mpu6050.isAttached()) {
			attachButton.setText(detach);
			controller.setEnabled(false);
			deviceBusList.setEnabled(false);
			deviceAddressList.setEnabled(false);
			refresh.setEnabled(true);
		} else {
			attachButton.setText(attach);
			controller.setEnabled(true);
			deviceBusList.setEnabled(true);
			deviceAddressList.setEnabled(true);
			refresh.setEnabled(false);
		}
		
    accelX.setText(String.format("%.3f", mpu6050.accelGX));
    accelY.setText(String.format("%.3f", mpu6050.accelGY));
    accelZ.setText(String.format("%.3f", mpu6050.accelGZ));
    temperature.setText(String.format("%.3f", mpu6050.temperatureC));
    gyroX.setText(String.format("%.3f", mpu6050.gyroDegreeX));
    gyroY.setText(String.format("%.3f", mpu6050.gyroDegreeY));
    gyroZ.setText(String.format("%.3f", mpu6050.gyroDegreeZ));

  }

  @Override
  public void init() {

    // Container BACKGROUND = getContentPane();

    // display.setLayout(new BorderLayout());
    // JPanel display
    display.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.ipadx = 0;
    
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 0;
		display.add(controllerLabel);

    c.gridx++;
		display.add(controller);
		
    c.gridx++;
		display.add(deviceBusLabel);

    c.gridx++;
		display.add(deviceBusList);
		
    c.gridx++;
		display.add(deviceAddressLabel);

    c.gridx++;
		display.add(deviceAddressList);
		
    c.gridx++;
		display.add(attachButton);
    attachButton.addActionListener(this);
		
    c.gridx++;
    display.add(refresh, c);
    refresh.addActionListener(this);

    c.gridx = 1;
    c.gridy++;
    c.gridy++;
    display.add(new JLabel("AccelX  :"), c);

    c.gridx++;
    display.add(accelX, c);

    c.gridx++;
    display.add(new JLabel(" G "), c);

    c.gridx = 1;
    c.gridy++;
    display.add(new JLabel("AccelY  :"), c);

    c.gridx++;
    display.add(accelY, c);

    c.gridx++;
    display.add(new JLabel(" G "), c);

    c.gridx = 1;
    c.gridy++;
    display.add(new JLabel("AccelZ  :"), c);

    c.gridx++;
    display.add(accelZ, c);

    c.gridx++;
    display.add(new JLabel(" G "), c);

    c.gridx = 1;
    c.gridy++;
    c.gridy++;
    display.add(new JLabel("Temperature : "), c);

    c.gridx++;
    display.add(temperature, c);

    c.gridx++;
    display.add(new JLabel(" degrees Celcius"), c);

    c.gridx = 1;
    c.gridy++;
    display.add(new JLabel("GyroX  :"), c);

    c.gridx++;
    display.add(gyroX, c);

    c.gridx++;
    display.add(new JLabel(" degrees/s"), c);

    c.gridx = 1;
    c.gridy++;
    display.add(new JLabel("GyroY  :"), c);

    c.gridx++;
    display.add(gyroY, c);

    c.gridx++;
    display.add(new JLabel(" degrees/s"), c);

    c.gridx = 1;
    c.gridy++;
    display.add(new JLabel("GyroZ  :"), c);

    c.gridx++;
    display.add(gyroZ, c);

    c.gridx++;
    display.add(new JLabel(" degrees/s"), c);
    
		refreshControllers();
		getDeviceBusList();
		getDeviceAddressList();
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				List<String> v = boundService.refreshControllers();
				controller.removeAllItems();
				for (int i = 0; i < v.size(); ++i) {
					controller.addItem(v.get(i));
				}
				controller.setSelectedItem(boundService.getControllerName());
			}
		});
	}
}
