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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.WolframAlpha;
import org.slf4j.Logger;

public class WolframAlphaGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WolframAlphaGui.class);
  private JEditorPane result = new JEditorPane();
  private JTextField query = new JTextField();

  public WolframAlphaGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    display.setLayout(new BorderLayout());
    display.setPreferredSize(new Dimension(800, 600));
    JScrollPane js = new JScrollPane(result);
    js.setAutoscrolls(true);
    display.add(js, "Center");
    display.add(query, "South");
    // query.setText("query");
    result.setContentType("text/html");
    result.setText("<html><body>Wolfram Alpha Knowledge Engine<br>Type a query and press enter in the box below.</body></html>");
    query.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        String text = query.getText();
        query.setText("querying...");
        query.validate();
        query.update(query.getGraphics());
        String answer = (String) myService.sendBlocking(boundServiceName, 30000, "wolframAlpha", text, Boolean.TRUE);
        // System.out.println(answer);
        result.setText(answer);
        result.setCaretPosition(0);
        query.setText("");
      }
    });
  
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void subscribeGui() {
    subscribe("publishState", "onState");
    myService.send(boundServiceName, "publishState");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishState", "onState");
  }

  public void onState(WolframAlpha template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
      }
    });
  }

}
