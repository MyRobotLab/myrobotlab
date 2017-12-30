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
 * This service uses jnativehook so that keyboard events will be listened to even
 * if the GUI doesn't have focus
 * 
 * References:
 * 
 * https://github.com/kwhat/jnativehook/
 * https://github.com/kwhat/jnativehook/wiki/SwingGui
 * */

package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Keyboard;
import org.myrobotlab.service.Keyboard.MouseEvent;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class KeyboardGui extends ServiceGui implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(KeyboardGui.class);
  static final long serialVersionUID = 1L;
  JButton listen = new JButton("start listening");
  JLabel lastKey = new JLabel("");
  JLabel lastKeyCode = new JLabel("");
  JLabel mouseX = new JLabel("");
  JLabel mouseY = new JLabel("");

  public KeyboardGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    addTop("keyboard");
    addTop("key: ", lastKey, " code: ", lastKeyCode);
    addTop(" ");
    addTop("mouse");
    addTop("x: ", mouseX, " y: ", mouseY);
  }

  @Override
  public void subscribeGui() {
    subscribe("publishKey");
    subscribe("publishKeyCode");
    subscribe("publishMouseMoved");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishKey");
    unsubscribe("publishKeyCode");
    unsubscribe("publishMouseMoved");
  }
  
  public void onKey(final String key){
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        lastKey.setText(key);
      }
    });
  }
  
  public void onKeyCode(final Integer code){
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        lastKeyCode.setText(String.format("%d", code));
      }
    });
  }
  
  public void onMouseMoved(final MouseEvent me ){
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mouseX.setText(String.format("%d", (int) me.pos.x));
        mouseY.setText(String.format("%d", (int) me.pos.y));
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == listen) {
      String text = listen.getText();
      if ("start listening".equals(text)){
        myService.send(boundServiceName, "startListening");
        listen.setText("stop listening");
      } else {
        myService.send(boundServiceName, "stopListening");
        listen.setText("start listening");
      }
    }
  }

  /*
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState.  This event handler is typically
   * used when data or state information in the service has changed, and the UI should
   * update to reflect this changed state.
   */
  public void onState(Keyboard keyboard) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
      }
    });
  }
}