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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.myrobotlab.service.SwingGui;

public class TestThrowerGui extends ServiceGui {

  static final long serialVersionUID = 1L;
  static final String type = "TestThrowerGUI";
  Integer integer = new Integer(33);
  JTextField lowPitchInteger = new JTextField("353");
  JTextField throwInteger = new JTextField("17");
  JTextField setNumberOfPitchers = new JTextField("0");

  JLabel throwIntegerLabel = new JLabel("");
  JLabel throwVideoLabel = new JLabel("");
  JLabel throwSerialLabel = new JLabel("");

  public TestThrowerGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    display.setSize(400, 200);
    display.setLayout(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.EAST;
    gc.fill = GridBagConstraints.HORIZONTAL;

    // ---------------------------------------------------
    JButton t = new JButton("throw integer");
    t.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myService.send(boundServiceName, "throwInteger", Integer.parseInt(throwInteger.getText()));
      }

    });

    gc.gridy = 0;
    gc.gridx = 0;
    display.add(t, gc);
    ++gc.gridx;
    display.add(throwInteger, gc);

    // ---------------------------------------------------
    JButton t2 = new JButton("lowPitch integer");
    t2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myService.send(boundServiceName, "lowPitchInteger", Integer.parseInt(lowPitchInteger.getText()));
      }

    });

    gc.gridx = 0;
    ++gc.gridy;
    display.add(t2, gc);
    ++gc.gridx;
    display.add(lowPitchInteger, gc);

    // ---------------------------------------------------
    JButton t3 = new JButton("set number of pitchers");
    t3.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myService.send(boundServiceName, "setNumberOfPitchers", Integer.parseInt(setNumberOfPitchers.getText()));
      }

    });

    gc.gridx = 0;
    ++gc.gridy;
    display.add(t3, gc);
    ++gc.gridx;
    display.add(setNumberOfPitchers, gc);

    // ---------------------------------------------------

    gc.gridx = 0;
    ++gc.gridy;
    display.add(new JLabel("throwInteger : "), gc);
    ++gc.gridx;
    display.add(throwIntegerLabel, gc);

    ++gc.gridx;

    JButton throwIntegerButton = new JButton("connect throwInteger");
    throwIntegerButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        subscribe("throwInteger", "throwInteger");
      }

    });
    display.add(throwIntegerButton, gc);

    gc.gridx = 0;
    ++gc.gridy;
    display.add(new JLabel("throwVideo : "), gc);
    ++gc.gridx;
    display.add(throwVideoLabel, gc);

    gc.gridx = 0;
    ++gc.gridy;
    display.add(new JLabel("throwSerial : "), gc);
    ++gc.gridx;
    display.add(throwSerialLabel, gc);

    // ---------------------------------------------------

  
  }

  @Override
  public void subscribeGui() {
    // TODO Auto-generated method stub

  }

  @Override
  public void unsubscribeGui() {
    // TODO Auto-generated method stub

  }

  public void throwInteger(Integer count) {
    throwIntegerLabel.setText(count.toString());
  }

  public void throwSerial(Integer count) {
    throwSerialLabel.setText(count.toString());
  }

  public void throwVideo(Integer count) {
    throwVideoLabel.setText(count.toString());
  }

}
