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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
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

  private class SliderListener implements ChangeListener,MouseListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

      boundPos.setText(String.format("%d", slider.getValue()));

      if (myService != null) {
        myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
      } else {
        log.error("can not send message myService is null");
      }
    }


    @Override
    public void mousePressed(MouseEvent e) {
     send("setOverrideAutoDisable",true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
     send("setOverrideAutoDisable",false);
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

  public final static Logger log = LoggerFactory.getLogger(ServoGui.class);
  private String lastControllerUsed;
  static final long serialVersionUID = 1L;

  JLabel boundPos = new JLabel("90");
  JButton attachButton = new JButton("attach");
  JButton updateMinMaxButton = new JButton("set");
  JTextField velocity = new JTextField("-1");
  JButton setVelocity = new JButton("Velocity");
  
  JButton updateMapButton = new JButton("set");
  JButton enableButton = new JButton("enable");
  JCheckBox autoDisable = new JCheckBox("autoDisable");
  JSlider slider = new JSlider(0, 180, 90);
  BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
  BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

  JComboBox<String> controller = new JComboBox<String>();
  JComboBox<Integer> pinList = new JComboBox<Integer>();

  JTextField posMin = new JTextField("0");
  JTextField posMax = new JTextField("180");
  JTextField minInput = new JTextField("0");
  JTextField maxInput = new JTextField("180");
  JTextField minOutput = new JTextField("0");
  JTextField maxOutput = new JTextField("180");
  
  JButton sweepButton = new JButton("sweep");
  JButton eventsButton = new JButton("events");
  
  JLabel imageenabled = new JLabel();
  ImageIcon enabled = Util.getImageIcon("enabled.png");

  // Servo myServox = null;

  SliderListener sliderListener = new SliderListener();

  boolean eventsEnabled;
  
  public ServoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    // myServo = (Servo) Runtime.getService(boundServiceName);

   
    
    for (int i = 0; i < 54; i++) {
      pinList.addItem(i);
    }
    setVelocity.addActionListener(this);
    posMin.setPreferredSize(new Dimension( 50, 24 ));
    posMax.setPreferredSize(new Dimension( 50, 24 ));
    minInput.setPreferredSize(new Dimension( 50, 24 ));
    maxInput.setPreferredSize(new Dimension( 50, 24 ));
    minOutput.setPreferredSize(new Dimension( 50, 24 ));
    maxOutput.setPreferredSize(new Dimension( 50, 24 ));
    velocity.setPreferredSize(new Dimension( 50, 24 ));
    updateMinMaxButton.addActionListener(this);
    updateMapButton.addActionListener(this);
    left.addActionListener(this);
    right.addActionListener(this);
    controller.addActionListener(this);
    attachButton.addActionListener(this);
    enableButton.addActionListener(this);
    autoDisable.addActionListener(this);
    sweepButton.addActionListener(this);
    eventsButton.addActionListener(this);
    pinList.addActionListener(this);
    boundPos.setFont(boundPos.getFont().deriveFont(32.0f));

    JPanel s = new JPanel();
    s.add(left);
    s.add(slider);
    s.add(right);
    addTopLeft(2, boundPos, 3, s,velocity,setVelocity );
    // addLine(s);
   
    
    JPanel controllerP = new JPanel();
    Border borderController = BorderFactory.createTitledBorder("Controller");
    controllerP.setBorder(borderController);
    JLabel controllerlabel = new JLabel("controller : ");
    JLabel pinlabel = new JLabel("pin : ");
    controllerP.add(controllerlabel);
    controllerP.add(controller);
    controllerP.add(pinlabel);
    controllerP.add(pinList);
    controllerP.add(attachButton);
    
    JPanel minMax = new JPanel();
    Border borderminMax = BorderFactory.createTitledBorder("minMax(min, max)");
    minMax.setBorder(borderminMax);
    minMax.add(posMin);
    minMax.add(posMax);
    minMax.add(updateMinMaxButton);
    
    JPanel map = new JPanel();
    Border bordermap = BorderFactory.createTitledBorder("map(minInput, maxInput, minOutput, maxOutput)");
    map.setBorder(bordermap);
    map.add(minInput);
    map.add(maxInput);
    map.add(minOutput);
    map.add(maxOutput);
    map.add(updateMapButton);
    
    JPanel power = new JPanel();
    Border extraborder = BorderFactory.createTitledBorder("Power");
    power.setBorder(extraborder);
    imageenabled.setIcon(enabled);
    power.add(enableButton);
    power.add(imageenabled);
    autoDisable.setSelected(false);
    power.add(autoDisable);
    
    JPanel sweep = new JPanel();
    Border sweepborder = BorderFactory.createTitledBorder("Sweep and Events");
    sweep.setBorder(sweepborder);
    sweep.add(sweepButton);
    sweep.add(eventsButton);
    
    addTopLeft(" ");
    addTopLeft(controllerP);
    addTopLeft(power);
    addTopLeft(" ");
    addTopLeft(map);
    addTopLeft(minMax);
    addTopLeft(sweep);
  
    refreshControllers();
  }

  // SwingGui's action processing section - data from user
  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        //log.error(o.toString());
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
        if (o == setVelocity) {
            send("setVelocity", Double.parseDouble(velocity.getText()));
            return;
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
        
        if (o == enableButton) {
            if (enableButton.getText().equals("enable")) {
            	if (!attachButton.getText().equals("attach")) {
              send("enable");
              imageenabled.setVisible(true);
            	}
            	else
            	{
            	log.error("Servo is not attached");	
            	}
            } else {
              send("disable");
              imageenabled.setVisible(false);
            }
            return;
          }
        
        if (o == autoDisable) {
            if (autoDisable.isSelected()) {
              send("enableAutoDisable",true);
            } else {
            	send("enableAutoDisable",false);
            }
            return;
          }

        if (o == updateMinMaxButton) {
          send("setMinMax", Double.parseDouble(posMin.getText()), Double.parseDouble(posMax.getText()));
          return;
        }

        if (o == updateMapButton) {
            send("map", Double.parseDouble(minInput.getText()), Double.parseDouble(maxInput.getText()),Double.parseDouble(minOutput.getText()), Double.parseDouble(maxOutput.getText()));
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
          }
          else {
            send("stop");        
          }
          return;
        }
  
        if (o == eventsButton) {
            send("eventsEnabled",!eventsEnabled);
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
        velocity.setText(servo.getVelocity() + "");
        
        minInput.setText(servo.getMinInput() + "");
        maxInput.setText(servo.getMaxInput() + "");
        minOutput.setText(servo.getMinOutput() + "");
        maxOutput.setText(servo.getMaxOutput() + "");
        
        if (servo.isSweeping()){
          sweepButton.setText("stop");        
        }
        else {
          sweepButton.setText("sweep");              
        }
        
        eventsEnabled = servo.isEventsEnabled();
        
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
    slider.removeMouseListener(sliderListener);
  }

  public void restoreListeners() {
    controller.addActionListener(this);
    pinList.addActionListener(this);
    slider.addChangeListener(sliderListener);
    slider.addMouseListener(sliderListener);
  }
}