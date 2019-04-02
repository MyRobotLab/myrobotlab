/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.fsm.api.State;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.FiniteStateMachine;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class FiniteStateMachineGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachineGui.class);

  Map<String, JPasswordField> keyNames = new HashMap<String, JPasswordField>();
  Map<String, JPasswordField> keyValues = new HashMap<String, JPasswordField>();

  JTextField newKeyName = new JTextField();
  JPasswordField newKeyValue = new JPasswordField();
  JButton set = new JButton("set");

  public FiniteStateMachineGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    newKeyName.setPreferredSize(new Dimension(200, 20));
    newKeyValue.setPreferredSize(new Dimension(200, 20));

    setTitle("keys");
    add("key name ", "key value");
    setBottomTitle("add keys");
    addBottom("key name", "key value", " ");
    addBottom(newKeyName, newKeyValue, set);
    set.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == set) {
      send("setKey", newKeyName.getText().trim(), new String(newKeyValue.getPassword()).trim());
      send("save");
      send("broadcastState");
    }
  }

  @Override
  public void subscribeGui() {
    send("broadcastState");
  }

  @Override
  public void unsubscribeGui() {
  }

  /**
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState. This event handler is
   * typically used when data or state information in the service has changed,
   * and the UI should update to reflect this changed state.
   * 
   * @param fsm
   *          the fsm service
   */
  public void onState(FiniteStateMachine fsm) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Set<State> names = fsm.getStates();
        // sort them
        Set<Object> sorted = new TreeSet<Object>(names);
        center.removeAll();
        for (Object name : sorted) {
          add(name.toString(), "*********");
        }
        center.validate();
      }
    });
  }

}
