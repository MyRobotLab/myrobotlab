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

import javax.swing.JLabel;

import org.myrobotlab.image.Util;
import org.myrobotlab.service.SwingGui;

public class Welcome extends ServiceGui {

  static final long serialVersionUID = 1L;

  public Welcome(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    JLabel image = new JLabel();
    image.setIcon(Util.getResourceIcon("mrl_logo.gif"));
    addTop(image);
    addTop("<html><b><i>I for one, welcome our new robot overlords ...</i></b></html>");   
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  public String setRemoteConnectionStatus(String state) {
    return state;
  }

}
