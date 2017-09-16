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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.myrobotlab.service.Clock;
import org.myrobotlab.service.SwingGui;

// FIXME - add stopwatch capabilities
public class ClockGui extends ServiceGui implements ActionListener {
  static final long serialVersionUID = 1L;
  JButton startClock = new JButton("start clock");

  JLabel clockDisplay = new JLabel("<html><p style=\"font-size:15px;\">00:00:00.</p></html>");
  String displayFormat = "<html><p style=\"font-size:15px\">%s</p></html>";
  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  JTextField interval = new JTextField("1000", 8);
 
  public ClockGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);  
    addTop(3, clockDisplay);
    addTop(startClock, interval, "ms");
    clockDisplay.setText(String.format(displayFormat, dateFormat.format(new Date())));
    startClock.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();

    if (o == startClock) {
      if (startClock.getText().compareTo("start clock") == 0) {
        myService.send(boundServiceName, "setInterval", Integer.parseInt(interval.getText()));
        myService.send(boundServiceName, "startClock");
      } else {
        myService.send(boundServiceName, "stopClock");
      }
    }
    myService.send(boundServiceName, "publishState");
  }

  public void addClockEvent(Date time, String name, String method, Object... data) {
    myService.send(boundServiceName, "addClockEvent", time, name, method, data);
  }

  @Override
  public void subscribeGui() {
    subscribe("countdown");
    subscribe("publishState");
    subscribe("pulse");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("countdown");
    unsubscribe("publishState");
    unsubscribe("pulse");
  }

  public void onState(final Clock c) {
    /*
     * setText IS THREAD SAFE !!!!!
     *
     * SwingUtilities.invokeLater(new Runnable() { public void run() {
     */

    interval.setText((c.interval + ""));

    if (c.isClockRunning) {
      startClock.setText("stop clock");
      interval.setEnabled(false);
    } else {
      startClock.setText("start clock");
      interval.setEnabled(true);
    }
  }

  public void onPulse(Date date) {
    clockDisplay.setText(String.format(displayFormat, dateFormat.format(date)));
    // countdown(System.currentTimeMillis(), date.);
  }

}
