/**
 *                    
 * @author moz4r  (at) myrobotlab.org
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

import java.awt.event.ActionListener;
import java.io.IOException;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.abstracts.AbstractSpeechSynthesisGui;
import org.slf4j.Logger;

public class IndianTtsGui extends AbstractSpeechSynthesisGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IndianTtsGui.class);

  public IndianTtsGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);
  }

}
