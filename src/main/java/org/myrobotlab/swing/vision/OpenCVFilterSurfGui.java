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

package org.myrobotlab.swing.vision;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.myrobotlab.service.SwingGui;
import org.myrobotlab.vision.FilterWrapper;
import org.myrobotlab.vision.OpenCVFilterSURF;

public class OpenCVFilterSurfGui extends OpenCVFilterGui implements ActionListener {

  JTextField objectFilename = new JTextField("objectFilename", 200);

  public OpenCVFilterSurfGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    objectFilename.addActionListener(this);
    TitledBorder title;
    JPanel j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("SURF Config");
    j.setBorder(title);
    j.add(new JLabel("Filename"));
    j.add(objectFilename);
    display.add(j, gc);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    // TODO Auto-generated method stub
    Object o = event.getSource();
    OpenCVFilterSURF sf = (OpenCVFilterSURF) boundFilter.filter;
    if (o == objectFilename) {
      String val = ((JTextField) o).getText();
      sf.loadObjectImageFilename(val);
    } else {
      log.warn("Inknown object invoked in surf filter ui");
    }
  }

  // FIXME - update components :)
  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // OpenCVFilterSURF af = (OpenCVFilterSURF) filterWrapper.filter;
        // TODO: doesn't do anything yet ? not implemented?
      }
    });

  }
}
