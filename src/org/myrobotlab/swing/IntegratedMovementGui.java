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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.IMEngine;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.kinematics.PositionData;
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
  private JPanel armsCoordPane = new JPanel();
  private JPanel itemPane = new JPanel();
  ButtonGroup armRadio = new ButtonGroup();

  private HashMap<String, JPanel> armPanels = new HashMap<String, JPanel>();
  private HashMap<String, JTextField> armText = new HashMap<String, JTextField>();
  private HashMap<String, JButton> buttons = new HashMap<String, JButton>();
  private HashMap<String, Component> components = new HashMap<String, Component>();
  
  public IntegratedMovementGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
    super(boundServiceName, myService, tabs);
    boundService = (IntegratedMovement) Runtime.getService(boundServiceName);

    // Container BACKGROUND = getContentPane();
    //display.setLayout(new BoxLayout());
     
    JPanel north = new JPanel();
    //armsCoordPane.setLayout(new FlowLayout(FlowLayout.LEFT));
    //north.setMaximumSize(new Dimension(500,100));
    //armsCoordPane.setPreferredSize(new Dimension(200,100));
    
    //itemPane.setPreferredSize(new Dimension(200,200));
    addTop(buildControl());
    addTopLine(armsCoordPane);
    itemPane.setLayout(new GridLayout(0,1));
    addTopLine(itemPane);
    //north.add(armsCoordPane);
    //itemPane.setLayout(new GridLayout(0,1));
    //north.add(itemPane);
    
//    north.setLayout(new GridLayout(0,6));
//    JPanel line = new JPanel();
//    stop.addActionListener(this);
//    line.add(stop);
//    process.addActionListener(this);
//    line.add(process);
//    north.add(line);
//    line = new JPanel();
//    for (IMEngine dhra: boundService.getArms()) {
//      arm.addItem(dhra.getName());
//    }
//    line.add(arm);
//    north.add(line);
//    line = new JPanel();
//    visualize.addActionListener(this);
//    line.add(visualize);
//    north.add(line);
//    line = new JPanel();
//    north.add(line);
//    line = new JPanel();
//    north.add(line);
//    line = new JPanel();
//    north.add(line);
//    
//    line = new JPanel();
//    line.add(new JLabel("Name"));
//    north.add(line);
//    line = new JPanel();
//    line.add(new JLabel("Origin"));
//    north.add(line);
//    line = new JPanel();
//    line.add(new JLabel("End"));
//    north.add(line);
//    line = new JPanel();
//    line.add(new JLabel("Radius"));
//    north.add(line);
//    line = new JPanel();
//    north.add(line);
//    line = new JPanel();
//    north.add(line);
//    for (int i=0; i<25; i++){
//      JPanel line1 = new JPanel();
//      objectName[i] = new JTextField(15);
//      line1.add(objectName[i]);
//      north.add(line1);
//      line1 = new JPanel();
//      objectOrigin[i] = new JLabel("origin");
//      line1.add(objectOrigin[i]);
//      north.add(line1);
//      line1 = new JPanel();
//      objectEnd[i] = new JLabel("end");
//      line1.add(objectEnd[i]);
//      north.add(line1);
//      line1 = new JPanel();
//      objectRadius[i] = new JLabel("radius");
//      line1.add(objectRadius[i]);
//      north.add(line1);
//      line1 = new JPanel();
//      moveLocation[i] = new JComboBox<ObjectPointLocation>();
//      for (ObjectPointLocation loc : boundService.getEnumLocationValue()) {
//        moveLocation[i].addItem(loc);
//      }
//      moveLocation[i].setSelectedItem(ObjectPointLocation.CLOSEST_POINT);
//      line1.add(moveLocation[i]);
//      north.add(line1);
//      line1 = new JPanel();
//      move[i] = new JButton("Move");
//      move[i].addActionListener(this);
//      line1.add(move[i]);
//      north.add(line1);
//    }
    //display.add(north, BorderLayout.NORTH);
    display.add(north);

  
  }


  private JPanel buildControl() {
    JPanel controlPane = new JPanel();
    stop.addActionListener(this);
    process.addActionListener(this);
    controlPane.add(stop);
    controlPane.add(process);
    return controlPane;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("IntegratedMovementGUI actionPerformed");
    ButtonModel rad = armRadio.getSelection();
    if (e.getActionCommand().contains("move-")){
      String armName = e.getActionCommand().substring(5);
      try {
        Double x = Double.valueOf(armText.get(armName+"-x").getText());
        Double y = Double.valueOf(armText.get(armName+"-y").getText());
        Double z = Double.valueOf(armText.get(armName+"-z").getText());
        myService.send(boundService.getName(), "moveTo", armName, x, y, z);
      }
      catch(NumberFormatException except) {
        log.info("Couln't parse coordinated for {}", armName);
      }
    }
    if (e.getActionCommand().contains("radio-")) {
      String armName = e.getActionCommand().substring(6);
      for (JButton button : buttons.values()) {
        if (button.getActionCommand().substring(0, 4).contains("move")){
          if (button.getActionCommand().contains(armName)) {
            button.setEnabled(true);
          }
          else {
            button.setEnabled(false);
          }
        }
      }
      
      for (JTextField at : armText.values()) {
        if (at.getName().contains(armName)) {
          at.setEnabled(true);
        }
        else {
          at.setEnabled(false);
        }
      }
      if(rad != null) {
        for (Component comp : components.values()) {
          comp.setEnabled(true);
        }
      }
      
    }
    Object o = e.getSource();
    if (e.getActionCommand().equals("moveTo")) {
      JButton but = (JButton)o;
      ObjectPointLocation opl= (ObjectPointLocation) ((JComboBox<?>)components.get("itemLocation-"+but.getName())).getSelectedItem();
      String armName = armRadio.getSelection().getActionCommand().substring(6);
      myService.send(boundServiceName, "moveTo", armName, but.getName(), opl);
    }
    for (int i = 0; i < 25; i++) {
    	if (o == move[i]) {
    		myService.send(boundServiceName, "setCurrentArm", arm.getSelectedItem());
    		myService.send(boundServiceName, "moveTo", objectName[i].getText(),moveLocation[i].getSelectedItem(),0,0,0);
    	}
    }
    if (o == stop) {
      myService.send(boundServiceName, "stopMoving");
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
    subscribe("publishPosition");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishState", "onState", IntegratedMovement.class);
    unsubscribe("publishPosition");
  }

  public void onState(IntegratedMovement im) {
    for (IMEngine engine : im.getArms()) {
      if (!armPanels.containsKey(engine.getName())){
        createArmPanel(engine);
      }
    }
  	objects =  im.getCollisionObject();
  	if (objects == null) return;
  	int i=0;
  	itemPane.removeAll();
  	itemPane.add(createItemPanelHeader());
  	for (CollisionItem ci : objects.values()) {
  	  if (ci.isRender()) {
  	    itemPane.add(createItemPanel(ci));
  	  }
  	}
    ButtonModel rad = armRadio.getSelection();
    if(rad == null) {
      for (Component comp : components.values()) {
        comp.setEnabled(false);
      }
    }
    myService.pack();
//  		if (i < 25){
//  			if(ci == null) continue;
//	  		objectName[i].setVisible(true);
//	  		objectName[i].setText(ci.getName());
//	  		objectOrigin[i].setVisible(true);
//	  		objectOrigin[i].setText(String.format("x: %d y: %d z: %d", (int)ci.getOrigin().getX(),(int)ci.getOrigin().getY(),(int)ci.getOrigin().getZ()));
//	  		objectEnd[i].setVisible(true);
//	  		objectEnd[i].setText(String.format("x: %d y: %d z: %d", (int)ci.getEnd().getX(),(int)ci.getEnd().getY(),(int)ci.getEnd().getZ()));
//	  		objectRadius[i].setVisible(true);
//	  		objectRadius[i].setText(String.format("%d", (int)ci.getRadius()));
//	  		if (ci.isFromKinect()){
//	  			moveLocation[i].setVisible(true);
//	  			move[i].setVisible(true);
//	  		}
//	  		else {
//	  			moveLocation[i].setVisible(false);
//	  			move[i].setVisible(false);
//	  		}
//	  		i++;
//  		}
//  	}
//  	for (int j = i; j < 25; j++) {
//  		objectName[j].setVisible(false);
//  		objectOrigin[j].setVisible(false);
//  		objectEnd[j].setVisible(false);
//  		objectRadius[j].setVisible(false);
//  		moveLocation[j].setVisible(false);
//  		move[j].setVisible(false);
//  	}
//  	arm.removeAllItems();
//  	for (IMEngine a : im.getArms()) {
//  		arm.addItem(a.getName());
//  	}
  }


  private JPanel createItemPanelHeader() {
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(1,6, 10,1));
    String[] labels = new String[]{"Name","Origin","End", "Radius", "Interraction", "Action"};
    for (String s : labels) {
      JLabel label = new JLabel(s);
      panel.add(label);
    }
    return panel;
  }


  private JPanel createItemPanel(CollisionItem ci) {
    JPanel panel = new JPanel(new GridLayout(0,6,10,1));
    JTextField name = new JTextField(15);
    name.setText(ci.getName());
    panel.add(name);
    JLabel origin = new JLabel(String.format("(%d, %d, %d)", (int)ci.getOrigin().getX(), (int)ci.getOrigin().getY(), (int)ci.getOrigin().getZ()));
    panel.add(origin);
    JLabel end = new JLabel(String.format("(%d, %d, %d)", (int)ci.getEnd().getX(), (int)ci.getEnd().getY(), (int)ci.getEnd().getZ()));
    panel.add(end);
    JLabel radius = new JLabel(String.format("%d", (int)ci.getRadius()));
    panel.add(radius);
    JComboBox<ObjectPointLocation> moveLocation = new JComboBox<ObjectPointLocation>();
    for (ObjectPointLocation loc : IntegratedMovement.ObjectPointLocation.values()) {
      moveLocation.addItem(loc);
    }
    moveLocation.setSelectedItem(ObjectPointLocation.CLOSEST_POINT);
    components.put("itemLocation-"+ci.getName(), moveLocation);
    panel.add(moveLocation);
    JButton but = new JButton("Move To");
    but.setName(ci.getName());
    but.setActionCommand("moveTo");
    but.addActionListener(this);
    components.put("butMoveTo-"+ci.getName(), but);
    panel.add(but);
    return panel;
  }


  private void createArmPanel(IMEngine engine) {
    JPanel panel = new JPanel();
    JRadioButton button = new JRadioButton(engine.getName());
    button.setActionCommand("radio-" + engine.getName());
    button.setName(engine.getName());
    panel.add(button);
    button.addActionListener(this);
    JTextField textx = new JTextField(5);
    textx.setName(engine.getName()+"-x");
    textx.setEnabled(false);
    textx.setToolTipText("x");
    panel.add(textx);
    JTextField texty = new JTextField(5);
    texty.setName(engine.getName()+"-y");
    texty.setEnabled(false);
    texty.setToolTipText("y");
    panel.add(texty);
    JTextField textz = new JTextField(5);
    textz.setName(engine.getName()+"-z");
    textz.setEnabled(false);
    textz.setToolTipText("z");
    panel.add(textz);
    armText.put(engine.getName()+"-x", textx);
    armText.put(engine.getName()+"-y", texty);
    armText.put(engine.getName()+"-z", textz);
    Point point = engine.getDHRobotArm().getPalmPosition();
    armText.get(engine.getName()+"-x").setText(String.format("%d", (int)point.getX()));
    armText.get(engine.getName()+"-y").setText(String.valueOf((int)point.getY()));
    armText.get(engine.getName()+"-z").setText(String.valueOf((int)point.getZ()));
    JButton but = new JButton("Move");
    but.setActionCommand("move-"+engine.getName());
    but.addActionListener(this);
    but.setEnabled(false);
    buttons.put("move-" + engine.getName(), but);
    panel.add(but);
    armRadio.add(button);
    armsCoordPane.add(panel);
    armsCoordPane.revalidate();
    armPanels.put(engine.getName(), panel);
    display.validate();
     myService.pack();
  }


  public void onPosition() {
    
  }

  public void onPosition(final PositionData position) {
    if (!armPanels.containsKey(position.armName)){
        createArmPanel((IMEngine) boundService.getEngine(position.armName));
    }
    if (!armText.get(position.armName + "-x").isEnabled()) {
      armText.get(position.armName + "-x").setText(String.format("%d", (int)position.position.getX()));
      armText.get(position.armName + "-y").setText(String.format("%d", (int)position.position.getY()));
      armText.get(position.armName + "-z").setText(String.format("%d", (int)position.position.getZ()));
    }
  }
}
