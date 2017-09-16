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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.service.SwingGui;

public class TestCatcherGui extends ServiceGui {

  static final long serialVersionUID = 1L;
  JLabel catchInteger = new JLabel("0");
  JButton bindCatchIntegerButton = null;

  public TestCatcherGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    display.add(new JLabel("catchInteger : "));
    display.add(catchInteger);
    display.add(getBindCatchIntegerButton());
  
  }

  @Override
  public void subscribeGui() {
  }

  // TODO - reflect and auto-bind (or pull info from the Service/Method
  // directory
  // autoBind(ServiceName) would send all NotificationEntries to a service
  public void bindCatchInteger() {
    MRLListener MRLListener = new MRLListener("catchInteger", myService.getName(), "catchInteger");
    myService.send(boundServiceName, "addListener", MRLListener);
  }

  public void catchInteger(Integer i) {
    catchInteger.setText(i.toString());
  }

  @Override
  public void unsubscribeGui() {
  }

  // TODO - generalize this and use it in reflection
  public JButton getBindCatchIntegerButton() {
    if (bindCatchIntegerButton == null) {
      bindCatchIntegerButton = new JButton("connect");
      bindCatchIntegerButton.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          myService.send(boundServiceName, "catchNothing");
          /*
           * if (bindCatchIntegerButton.getText().compareTo("connect") == 0) {
           * bindCatchIntegerButton.setText("disconnect");
           * subscribe("catchInteger", "catchInteger", SerializableImage.class);
           * } else { bindCatchIntegerButton.setText("connect");
           * unsubscribe("catchInteger", "catchInteger",
           * SerializableImage.class); }
           */
        }

      });

    }

    return bindCatchIntegerButton;

  }

}
