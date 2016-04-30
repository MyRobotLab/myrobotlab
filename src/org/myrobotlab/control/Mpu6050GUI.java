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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Mpu6050;
import org.slf4j.Logger;

public class Mpu6050GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Mpu6050GUI.class.getCanonicalName());

	JButton refresh = new JButton("refresh");
	
	JLabel accelX = new JLabel();
	JLabel accelY = new JLabel();
	JLabel accelZ = new JLabel();
	JLabel temperature = new JLabel();
	JLabel gyroX = new JLabel();
	JLabel gyroY = new JLabel();
	JLabel gyroZ = new JLabel();
	
	public Mpu6050GUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    log.info("Mpu6050 actionPerformed");
		Object o = e.getSource();
		if (o == refresh) {
			myService.send(boundServiceName, "readRaw");
		}
	}

	@Override
	public void attachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's
		subscribe("publishState", "getState", Mpu6050.class);
		send("publishState");
	}

	@Override
	public void detachGUI() {
		// commented out subscription due to this class being used for
		// un-defined gui's

		unsubscribe("publishState", "getState", Mpu6050.class);
	}

	public void getState(Mpu6050 mpu6050) {
		accelX.setText(String.format("%s",mpu6050.accelX));
		accelY.setText(String.format("%s",mpu6050.accelY));
		accelZ.setText(String.format("%s",mpu6050.accelZ));
		temperature.setText(String.format("%s",mpu6050.temperature));
		gyroX.setText(String.format("%s",mpu6050.gyroX));
		gyroY.setText(String.format("%s",mpu6050.gyroY));
		gyroZ.setText(String.format("%s",mpu6050.gyroZ));
		
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
		c.gridx = 2;
		c.gridy = 0;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		display.add(refresh,c);
		refresh.addActionListener(this);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		display.add(new JLabel("AccelX :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 1;
		display.add(accelX,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 1;
		display.add(new JLabel(" uom? "),c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 2;
		display.add(new JLabel("AccelY :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 2;
		display.add(accelY,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 2;
		display.add(new JLabel(" uom? "),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 3;
		display.add(new JLabel("AccelZ :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 3;
		display.add(accelZ,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 3;
		display.add(new JLabel(" uom ? "),c);	
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 4;
		display.add(new JLabel("Temperature :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 4;
		display.add(temperature,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 4;
		display.add(new JLabel(" degrees Celcius"),c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 5;
		display.add(new JLabel("GyroX :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 5;
		display.add(gyroX,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 5;
		display.add(new JLabel(" degrees"),c);
		

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 6;
		display.add(new JLabel("GyroY :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 6;
		display.add(gyroY,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 6;
		display.add(new JLabel(" degrees"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 7;
		display.add(new JLabel("GyroZ :"),c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 7;
		display.add(gyroZ,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 7;
		display.add(new JLabel(" degrees"),c);
		
		display.repaint();
	}

}
