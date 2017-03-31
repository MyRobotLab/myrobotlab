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

package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SelectingToggle {
  public static void main(String args[]) {
    String title = (args.length == 0 ? "Selecting Toggle" : args[0]);
    JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JToggleButton toggleButton = new JToggleButton("Selected");
    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        System.out.println("Action - selected=" + selected + "\n");
      }
    };
    ChangeListener changeListener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        AbstractButton abstractButton = (AbstractButton) changeEvent.getSource();
        ButtonModel buttonModel = abstractButton.getModel();
        boolean armed = buttonModel.isArmed();
        boolean pressed = buttonModel.isPressed();
        boolean selected = buttonModel.isSelected();
        System.out.println("Changed: " + armed + "/" + pressed + "/" + selected);
      }
    };
    ItemListener itemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent itemEvent) {
        int state = itemEvent.getStateChange();
        if (state == ItemEvent.SELECTED) {
          System.out.println("Selected");
        } else {
          System.out.println("Deselected");
        }
      }
    };
    // Attach Listeners
    toggleButton.addActionListener(actionListener);
    toggleButton.addChangeListener(changeListener);
    toggleButton.addItemListener(itemListener);
    Container contentPane = frame.getContentPane();
    contentPane.add(toggleButton, BorderLayout.NORTH);

    JToggleButton toggleButton2 = new JToggleButton("Focused");
    contentPane.add(toggleButton2, BorderLayout.CENTER);

    JToggleButton toggleButton3 = new JToggleButton("Not Selected");
    contentPane.add(toggleButton3, BorderLayout.SOUTH);

    frame.setSize(300, 125);
    frame.setVisible(true);
  }
}
