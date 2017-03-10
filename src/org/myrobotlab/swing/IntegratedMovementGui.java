/**
 *                    
 * @author Christian Beliveau (at) myrobotlab.org
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.IntegratedMovement.ObjectPointLocation;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class IntegratedMovementGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IntegratedMovementGui.class);


  IntegratedMovement boundService = null;
  
  JTextField[] objectName = new JTextField[25];
  JLabel[] objectOrigin = new JLabel[25];
  JLabel[] objectEnd = new JLabel[25];
  
  ConcurrentHashMap<String, CollisionItem> objects;
	JLabel[] objectRadius = new JLabel[25];
	JComboBox<ObjectPointLocation>[] moveLocation = new JComboBox[25];
	JButton[] move = new JButton[25];
	JButton stop = new JButton("Stop");
	JButton process = new JButton("Process Kinect Data");
	JComboBox<String> arm = new JComboBox<String>();
	JButton visualize = new JButton("Visualize");

  public IntegratedMovementGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
    super(boundServiceName, myService, tabs);
    boundService = (IntegratedMovement) Runtime.getService(boundServiceName);


    // Container BACKGROUND = getContentPane();
    display.setLayout(new BorderLayout());
    JPanel north = new JPanel();
    north.setLayout(new GridLayout(0,6));
    JPanel line = new JPanel();
    stop.addActionListener(this);
    line.add(stop);
    process.addActionListener(this);
    line.add(process);
    north.add(line);
    line = new JPanel();
    for (DHRobotArm dhra: boundService.getArms()) {
      arm.addItem(dhra.name);
    }
    line.add(arm);
    north.add(line);
    line = new JPanel();
    visualize.addActionListener(this);
    line.add(visualize);
    north.add(line);
    line = new JPanel();
    north.add(line);
    line = new JPanel();
    north.add(line);
    line = new JPanel();
    north.add(line);
    line = new JPanel();
    line.add(new JLabel("Name"));
    north.add(line);
    line = new JPanel();
    line.add(new JLabel("Origin"));
    north.add(line);
    line = new JPanel();
    line.add(new JLabel("End"));
    north.add(line);
    line = new JPanel();
    line.add(new JLabel("Radius"));
    north.add(line);
    line = new JPanel();
    north.add(line);
    line = new JPanel();
    north.add(line);
    for (int i=0; i<25; i++){
      JPanel line1 = new JPanel();
      objectName[i] = new JTextField(15);
      line1.add(objectName[i]);
      north.add(line1);
      line1 = new JPanel();
      objectOrigin[i] = new JLabel("origin");
      line1.add(objectOrigin[i]);
      north.add(line1);
      line1 = new JPanel();
      objectEnd[i] = new JLabel("end");
      line1.add(objectEnd[i]);
      north.add(line1);
      line1 = new JPanel();
      objectRadius[i] = new JLabel("radius");
      line1.add(objectRadius[i]);
      north.add(line1);
      line1 = new JPanel();
      moveLocation[i] = new JComboBox<ObjectPointLocation>();
      for (ObjectPointLocation loc : boundService.getEnumLocationValue()) {
        moveLocation[i].addItem(loc);
      }
      moveLocation[i].setSelectedItem(ObjectPointLocation.CLOSEST_POINT);
      line1.add(moveLocation[i]);
      north.add(line1);
      line1 = new JPanel();
      move[i] = new JButton("Move");
      move[i].addActionListener(this);
      line1.add(move[i]);
      north.add(line1);
    }
    display.add(north, BorderLayout.NORTH);

  
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("IntegratedMovementGUI actionPerformed");
    Object o = e.getSource();
    for (int i = 0; i < 25; i++) {
    	if (o == move[i]) {
    		myService.send(boundServiceName, "setCurrentArm", arm.getSelectedItem());
    		myService.send(boundServiceName, "moveTo", objectName[i].getText(),moveLocation[i].getSelectedItem(),0,0,0);
    	}
    }
    if (o == stop) {
    	//myService.send(boundServiceName, "stopMoving");
    	boundService.stopMoving();
    }
    if (o== process) {
    	myService.send(boundServiceName, "processKinectData");
    }
    if (o == arm) {
    	myService.send(boundServiceName, "setCurrentArm", arm.getSelectedItem());
    }
    if (o == visualize) {
    	myService.send(boundServiceName, "visualize");
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishState", "onState", IntegratedMovement.class);
    send("publishState");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishState", "onState", IntegratedMovement.class);
  }

  public void onState(IntegratedMovement im) {
  	objects =  im.getCollisionObject();
  	if (objects == null) return;
  	int i=0;
  	for (CollisionItem ci : objects.values()) {
  		if (i < 25){
  			if(ci == null) continue;
	  		objectName[i].setVisible(true);
	  		objectName[i].setText(ci.getName());
	  		objectOrigin[i].setVisible(true);
	  		objectOrigin[i].setText(String.format("x: %d y: %d z: %d", (int)ci.getOrigin().getX(),(int)ci.getOrigin().getY(),(int)ci.getOrigin().getZ()));
	  		objectEnd[i].setVisible(true);
	  		objectEnd[i].setText(String.format("x: %d y: %d z: %d", (int)ci.getEnd().getX(),(int)ci.getEnd().getY(),(int)ci.getEnd().getZ()));
	  		objectRadius[i].setVisible(true);
	  		objectRadius[i].setText(String.format("%d", (int)ci.getRadius()));
	  		if (ci.isFromKinect()){
	  			moveLocation[i].setVisible(true);
	  			move[i].setVisible(true);
	  		}
	  		else {
	  			moveLocation[i].setVisible(false);
	  			move[i].setVisible(false);
	  		}
	  		i++;
  		}
  	}
  	for (int j = i; j < 25; j++) {
  		objectName[j].setVisible(false);
  		objectOrigin[j].setVisible(false);
  		objectEnd[j].setVisible(false);
  		objectRadius[j].setVisible(false);
  		moveLocation[j].setVisible(false);
  		move[j].setVisible(false);
  	}
  	arm.removeAllItems();
  	for (DHRobotArm a : im.getArms()) {
  		arm.addItem(a.name);
  	}
  	if (im.getCurrentArm()!=null) {
  		arm.setSelectedItem(im.getCurrentArm().name);
  	}
  }

  
}
