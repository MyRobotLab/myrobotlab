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
 * References :
 * 	http://www.pjrc.com/teensy/td_joystick.html gamepad map
 * 
 * */

package org.myrobotlab.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Joystick;
import org.myrobotlab.service.Joystick.Component;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.swing.widget.JoystickCompassPanel;

public class JoystickGui extends ServiceGui implements ActionListener, ItemListener {

  static final long serialVersionUID = 1L;

  // controller related
  JComboBox<String> controllers = new JComboBox<String>();
  TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();
  JButton refresh = new JButton("refresh");

  // component related
  TreeMap<String, Integer> components = new TreeMap<String, Integer>();
  HashMap<String, JLabel> outputValues = new HashMap<String, JLabel>();

  JoystickGui self = null;
  Joystick myJoy = null;

  /**
   * callback map for component data published from the joystick service
   */
  Map<String, JComponent> componentUi = new TreeMap<String, JComponent>();

  /**
   * panel for all the button components
   */
  JPanel buttonPanel = new JPanel();

  /**
   * panel for all the axis
   */
  JPanel axisPanel = new JPanel();

  JoystickCompassPanel lastJoystickCompassPanel = null;

  public JoystickGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    self = this;
    myJoy = (Joystick)Runtime.getService(boundServiceName);
    addTop(controllers, refresh);
    add(buttonPanel);
    add(axisPanel);
    refresh.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == refresh) {
      send("getControllers");
    } else {
      JButton b = (JButton) o;
      // must be button press from ui ?
      send("publishJoystickInput", new JoystickData(b.getText(), 1.0F));
      send("publishJoystickInput", new JoystickData(b.getText(), 0.0F));
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("getComponents");
    subscribe("getControllers");
    subscribe("publishJoystickInput");

    send("publishState");
    send("getControllers");
  }

  // FIXME - unsubscribes should be handled by Runtime
  // unless its implementation logic specific related
  @Override
  public void unsubscribeGui() {
    unsubscribe("getComponents");
    unsubscribe("getControllers");
    unsubscribe("publishJoystickInput");
  }

  public void onControllers(final Map<String, Integer> contrls) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // controllers.removeActionListener(self);
        controllers.removeItemListener(self);
        controllers.removeAllItems();
        controllerNames.clear();
        controllerNames.putAll(contrls);
        Iterator<String> it = controllerNames.keySet().iterator();
        controllers.addItem("");
        while (it.hasNext()) {
          String name = it.next();
          controllers.addItem(name);
        }
        
        String controller = myJoy.getController();
        if (controller != null){
          controllers.setSelectedItem(controller);
        }
        // controllers.addActionListener(self);
        controllers.addItemListener(self);

      }
    });

  }

  // FIXME - is get/set state interact with Runtime registry ???
  // it probably should
  public void onState(final Joystick joy) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // update reference
        myJoy = joy;

        // FIXME - remove listeners - set JComboBox controllers
        // FIXME - REMOVE ALL LISTENERS !!!
        buttonPanel.removeAll();
        axisPanel.removeAll();
        componentUi.clear();

        Map<String, Component> comp = myJoy.getComponents();
        for (String name : comp.keySet()) {
          Component c = comp.get(name);
          JComponent b = null;
          if (!c.isAnalog) {
            JButton button = new JButton(name);
            button.addActionListener(self);
            b = button;
            b.setBackground(Color.WHITE);
            buttonPanel.add(b);
          } else {
            // we add 2D panels - so we need 2 axis
            // every 2 axis we add a new panel and assign the 2
            // axis
            if (lastJoystickCompassPanel != null) {
              b = lastJoystickCompassPanel;
              lastJoystickCompassPanel.setYid(name);
              lastJoystickCompassPanel = null;
            } else {
              b = new JoystickCompassPanel();
              lastJoystickCompassPanel = (JoystickCompassPanel) b;
              lastJoystickCompassPanel.setXid(name);
              axisPanel.add(b);
            }

          }
          componentUi.put(name, b);
        }
        // myService.pack();
      }
    });

  }

  public void onJoystickInput(final JoystickData input) {
    String id = input.id;
    log.info(String.format("onButton %s", input));
    if (input.value == null) {
      outputValues.get(id).setText("null");
      return;
    }
    if (outputValues.containsKey(id)) {
      outputValues.get(id).setText(input.value.toString());
    }

    if (componentUi.containsKey(id)) {
      JComponent b = componentUi.get(id);
      // TODO - look up type ... to know what to do ..
      if (b.getClass() == JButton.class) {
        if (input.value == 1.0) {
          b.setBackground(Color.GREEN);
          b.setForeground(Color.WHITE);
        } else {
          b.setBackground(Color.WHITE);
          b.setForeground(Color.BLACK);
        }
      } else {
        JoystickCompassPanel jcp = (JoystickCompassPanel) b;
        // jcp.setX(input.value);
        jcp.set(id, input.value);
      }
    }
  }

  @Override
  public void itemStateChanged(final ItemEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = e.getSource();

        if (o == controllers && e.getStateChange() == ItemEvent.SELECTED) {
          String selected = (String) controllers.getSelectedItem();
          if (selected == null || "".equals(selected)) {
            send("stopPolling");
          } else {
            log.info(String.format("changed to %s ", selected));
            send("setController", selected); // setController sets controller
                                             // AND starts polling
          }
        }
      }
    });

  }

}
