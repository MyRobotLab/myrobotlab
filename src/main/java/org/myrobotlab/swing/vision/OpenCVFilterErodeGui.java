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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.myrobotlab.service.SwingGui;
import org.myrobotlab.vision.FilterWrapper;
import org.myrobotlab.vision.OpenCVFilterErode;

public class OpenCVFilterErodeGui extends OpenCVFilterGui implements ActionListener {

  JComboBox<Integer> iterations = new JComboBox<Integer>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

  public OpenCVFilterErodeGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    iterations.addActionListener(this);
    display.add(iterations);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterErode bf = (OpenCVFilterErode) boundFilter.filter;

    if (o == iterations) {
      bf.numberOfIterations = (Integer) iterations.getSelectedItem();
    }

    setFilterState(bf);
  }

  // @Override
  public void attachGui() {
    log.debug("attachGui");

  }

  // @Override
  public void detachGui() {
    log.debug("detachGui");

  }

  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        OpenCVFilterErode bf = (OpenCVFilterErode) filterWrapper.filter;
        iterations.setSelectedItem(bf.numberOfIterations);
      }
    });
  }

}
