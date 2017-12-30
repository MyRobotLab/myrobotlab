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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.NeoPixel;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class NeoPixelGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(NeoPixelGui.class);

  String attach = "attach";
  String detach = "detach";
  JButton attachButton = new JButton(attach);

  JComboBox<String> controller = new JComboBox<String>();
  JComboBox<String> pinList = new JComboBox<String>();
  JComboBox<String> pixelList = new JComboBox<String>();

  JLabel controllerLabel     = new JLabel("Controller");
  JLabel pinLabel     = new JLabel("Pin");
  JLabel pixelLabel = new JLabel("Num. Pixel");
  
  JButton refresh = new JButton("refresh");

  JLabel[] pixelAddress = new JLabel[25];
  JTextField[] pixelRed = new JTextField[25]; 
  JTextField[] pixelGreen = new JTextField[25]; 
  JTextField[] pixelBlue = new JTextField[25]; 
  
  JButton[] setPixel = new JButton[25];
  JButton[] sendPixel = new JButton[25];

  NeoPixel boundService = null;
  
  JButton sendPixelMatrix = new JButton("Send Pixel Matrix");
  JButton turnOnOff = new JButton("Turn Off");
  
  JLabel animation = new JLabel("No animation");
  JComboBox<String> animationList = new JComboBox<String>();
  JLabel labelRed = new JLabel("Red");
  JLabel labelGreen = new JLabel("Green");
  JLabel labelBlue = new JLabel("Blue");
  JLabel labelSpeed = new JLabel("Speed");
  JPanel animColor = new JPanel();
  JTextField animRed = new JTextField(3);
  JTextField animGreen = new JTextField(3);
  JTextField animBlue = new JTextField(3);
  JTextField animSpeed = new JTextField(4);
  JButton animStart = new JButton("Start Animation");

  public NeoPixelGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    boundService = (NeoPixel) Runtime.getService(boundServiceName);


    // Container BACKGROUND = getContentPane();

    display.setLayout(new BorderLayout());
    JPanel north = new JPanel();
    north.add(controllerLabel);
    north.add(controller);
    north.add(pinLabel);    
    north.add(pinList);
    north.add(pixelLabel);
    north.add(pixelList);
    north.add(attachButton);
    north.add(refresh);
    attachButton.addActionListener(this); 
    refresh.addActionListener(this);

    display.add(north, BorderLayout.NORTH);
    JPanel anim = new JPanel();
    anim.add(new JLabel("Current Animation:"));
    anim.add(animation);
    anim.add(animationList);
    animColor.add(labelRed);
    animColor.add(animRed);
    animRed.setText("0");
    animColor.add(labelGreen);
    animColor.add(animGreen);
    animGreen.setText("0");
    animColor.add(labelBlue);
    animColor.add(animBlue);
    animBlue.setText("0");
    anim.add(animColor);
    anim.add(labelSpeed);
    anim.add(animSpeed);
    animSpeed.setText("1");
    anim.add(animStart);
    animStart.addActionListener(this);
    animationList.addActionListener(this);

    
    JPanel center = new JPanel();
    center.setLayout(new GridLayout(0,2));
    JPanel lineEnd=new JPanel();
    sendPixelMatrix.addActionListener(this);
    lineEnd.add(sendPixelMatrix);
    turnOnOff.addActionListener(this);
    lineEnd.add(turnOnOff);
    center.add(lineEnd);
    JPanel line = new JPanel();
    line.add(new JLabel("Address"));
    line.add(new JLabel("Red"));
    line.add(new JLabel("Green"));
    line.add(new JLabel("Blue"));
    center.add(line);
    for (int i=0; i<25; i++){
      JPanel line1 = new JPanel();
      pixelAddress[i] = new JLabel("test");
      line1.add(pixelAddress[i]);
      pixelRed[i] = new JTextField(3);
      line1.add(pixelRed[i]);
      pixelGreen[i] = new JTextField(3);
      line1.add(pixelGreen[i]);
      pixelBlue[i] = new JTextField(3);
      line1.add(pixelBlue[i]);
      setPixel[i] = new JButton("Set Pixel");
      setPixel[i].addActionListener(this);
      sendPixel[i] = new JButton("Send Pixel");
      sendPixel[i].addActionListener(this);
      line1.add(setPixel[i]);
      line1.add(sendPixel[i]);
      center.add(line1);
    }
    display.add(center, BorderLayout.CENTER);
    display.add(anim, BorderLayout.SOUTH);
    getPinList();
    getPixelList();
    getAnimationList();
  
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("NeoPixelGUI actionPerformed");
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
              pinList.getSelectedItem(),
              pixelList.getSelectedItem());
        }
      } else {
        myService.send(boundServiceName, detach);
      }
    }
    if (o == sendPixelMatrix){
      myService.send(boundServiceName, "writeMatrix");
    }
    if (o == turnOnOff){
      if (turnOnOff.getText().equals("Turn On")){
        myService.send(boundServiceName, "turnOn");
        turnOnOff.setText("Turn Off");
      }
      else{
        myService.send(boundServiceName, "turnOff");
        turnOnOff.setText("Turn On");
      }
    }
    for (int i = 0; i < 25; i++) {
      if(o == setPixel[i]){
        myService.send(boundServiceName, "setPixel", pixelAddress[i].getText(), pixelRed[i].getText(), pixelGreen[i].getText(), pixelBlue[i].getText());
      }
      if(o == sendPixel[i]) {
        myService.send(boundServiceName, "sendPixel", pixelAddress[i].getText(), pixelRed[i].getText(), pixelGreen[i].getText(), pixelBlue[i].getText());
      }
    }
    if(o == animationList) {
      myService.send(boundServiceName, "setAnimationSetting", animationList.getSelectedItem());
    }
    if(o == animStart) {
      myService.send(boundServiceName, "setAnimation", animationList.getSelectedItem(), animRed.getText(), animGreen.getText(), animBlue.getText(), animSpeed.getText());
    }
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  public void onState(NeoPixel neopixel) {

    refreshControllers();
    controller.setSelectedItem(neopixel.getControllerName());
    pinList.setSelectedItem(neopixel.pin);
    pixelList.setSelectedItem(neopixel.numPixel.toString());
    animStart.setEnabled(neopixel.isAttached());
    if (neopixel.isAttached()) {
      animColor.setVisible(neopixel.animationSettingColor);
      labelSpeed.setVisible(neopixel.animationSettingSpeed);
      animSpeed.setVisible(neopixel.animationSettingSpeed);
      attachButton.setText(detach);
      controller.setEnabled(false);
      pinList.setEnabled(false);
      pixelList.setEnabled(false);
      refresh.setEnabled(true);
      animation.setText(neopixel.animation);
      animationList.setEnabled(true);
      for (int i = 0; i < neopixel.savedPixelMatrix.size() && neopixel.savedPixelMatrix.size()>0; i++ ) {
        pixelAddress[i].setVisible(true);
        pixelRed[i].setVisible(true);
        pixelGreen[i].setVisible(true);
        pixelBlue[i].setVisible(true);
        setPixel[i].setVisible(true);
        sendPixel[i].setVisible(true);
      try {
        pixelAddress[i].setText(neopixel.savedPixelMatrix.get(i).address+"");
        pixelRed[i].setText(neopixel.savedPixelMatrix.get(i).red+"");
        pixelGreen[i].setText(neopixel.savedPixelMatrix.get(i).green+"");
        pixelBlue[i].setText(neopixel.savedPixelMatrix.get(i).blue+"");
 
      } catch (Exception e) {
        log.warn("neopixel {} savedPixelMatrix InvocationTargetException", neopixel.getName());
        log.debug("neopixel {} savedPixelMatrix InvocationTargetException : "+e, neopixel.getName());
      }
    
        setPixel[i].setEnabled(true);
        sendPixel[i].setEnabled(true);
      }
      for (int i = neopixel.savedPixelMatrix.size(); i<25 && neopixel.savedPixelMatrix.size()>0; i++ ) {
        pixelAddress[i].setVisible(false);
        pixelRed[i].setVisible(false);
        pixelGreen[i].setVisible(false);
        pixelBlue[i].setVisible(false);
        setPixel[i].setVisible(false);
        sendPixel[i].setVisible(false);
      }
      sendPixelMatrix.setEnabled(true);
      turnOnOff.setEnabled(true);
    } else {
      animColor.setVisible(false);
      labelSpeed.setVisible(neopixel.animationSettingSpeed);
      animSpeed.setVisible(neopixel.animationSettingSpeed);
      attachButton.setText(attach);
      controller.setEnabled(true);
      pinList.setEnabled(true);
      pixelList.setEnabled(true);
      refresh.setEnabled(false);
      animationList.setEnabled(false);
      for (int i = 0; i < 25 ; i++){
        setPixel[i].setEnabled(false);
        sendPixel[i].setEnabled(false);
      }
      sendPixelMatrix.setEnabled(false);
      turnOnOff.setEnabled(false);
    }
    if (neopixel.off){
      turnOnOff.setText("Turn On");
    }
    else{
      turnOnOff.setText("Turn Off");
    }
  }
  
  private void getAnimationList() {
    List<String> anim = boundService.animations;
    for (int i = 0; i < anim.size(); i++){
      animationList.addItem(anim.get(i));
    }
  }

  public void getPinList() {
    for (int i = 0; i < 70; i++) {
      pinList.addItem(String.format("%d",i));
    }
  }
  
  public void getPixelList() {
    for (int i = 0; i < 25; i++) {
      pixelList.addItem(String.format("%d",i));
    }
  }
  
  public void refreshControllers() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        boundService.refreshControllers();
        controller.removeAllItems();
        List<String> c = boundService.controllers; 
        for (int i = 0; i < c.size(); ++i) {
          controller.addItem(c.get(i));
        }
        controller.setSelectedItem(boundService.getControllerName());
      }
    });
  }
}
