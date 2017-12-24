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

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service._TemplateService;
import org.slf4j.Logger;

public class _TemplateServiceGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGui.class);
  
  JLabel var1 = new JLabel("0.0");
  JLabel var2 = new JLabel("1.0");

  public _TemplateServiceGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    
    setTitle("status");
    addLine("name ", boundServiceName);
    addLine("var1 ", var1);
    addLine("var2 ", var2);
    
    setTitle("input");
    addLine("service ", boundServiceName);
    
    setTitle("output");
    
    /*
    JPanel panel = createPanel("panel1");
    panel.add(createLine("service ", boundServiceName));
    panel.add(createLine("gui service ", myService.getName()));
    panel.add(createLine(boundServiceName, " begining service"));
    
    display.add(panel, BorderLayout.EAST);
    */
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

  }

  @Override
  public void subscribeGui() {
    // un-defined gui's

    // subscribe("someMethod");
    // send("someMethod");
  }

  @Override
  public void unsubscribeGui() {
    // commented out subscription due to this class being used for
    // un-defined gui's

    // unsubscribe("someMethod");
  }

  /**
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState.  This event handler is typically
   * used when data or state information in the service has changed, and the UI should
   * update to reflect this changed state.
   * @param template the template service
   */
  public void onState(_TemplateService template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

      }
    });
  }

}
