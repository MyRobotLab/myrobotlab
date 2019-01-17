/**
 *                    
 * @author GroG (at) myrobotlab.org
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

package org.myrobotlab.swing.opencv;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterTracker;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterTrackerGui extends OpenCVFilterGui implements ActionListener {

  private JComboBox<String> trackerType = new JComboBox<String>(new String[] { "TLD", "Boosting", "CSRT", "GOTURN", "KCF", "MedianFlow", "MIL", "MOSSE" });
  // TODO: add bounding box size control.

  JTextField bbSizeX = new JTextField("20", 8);
  JTextField bbSizeY = new JTextField("20", 8);

  public OpenCVFilterTrackerGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    // build the config for this filter.
    TitledBorder title = BorderFactory.createTitledBorder("Tracker");
    JPanel j = new JPanel(new GridBagLayout());
    j.setBorder(title);
    trackerType.addActionListener(this);

    bbSizeX.addActionListener(this);
    bbSizeY.addActionListener(this);
    j.add(trackerType);
    j.add(new JLabel("Box Width:"));
    j.add(bbSizeX);
    j.add(new JLabel("Height:"));
    j.add(bbSizeY);
    display.add(j);

    // OpenCVFilterTracker bf = (OpenCVFilterTracker) boundFilter.filter;
    // bbSizeX.setText(Integer.toString(bf.boxWidth));
    // bbSizeY.setText(Integer.toString(bf.boxHeight));

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterTracker bf = (OpenCVFilterTracker) boundFilter.filter;
    if (o == trackerType) {
      String type = trackerType.getSelectedItem().toString();
      bf.trackerType = type;
    } else if (o == bbSizeX) {
      bf.boxWidth = Integer.valueOf(bbSizeX.getText());
    } else if (o == bbSizeY) {
      bf.boxHeight = Integer.valueOf(bbSizeY.getText());
    }
  }

  @Override
  public void getFilterState(FilterWrapper filterWrapper) {
    // TODO: what should i implement here?
  }

}
