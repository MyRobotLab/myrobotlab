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

import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class NoGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(NoGui.class);

  public NoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    ServiceInterface si = Runtime.getService(boundServiceName);
    addTopLine(String.format( "%s does not have a user interface", si.getSimpleName()));
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  /*
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState.  This event handler is typically
   * used when data or state information in the service has changed, and the UI should
   * update to reflect this changed state.
   */
  public void onState(Service template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

      }
    });
  }

}
