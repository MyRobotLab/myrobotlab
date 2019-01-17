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
 * http://docs.opencv.org/modules/video/doc/motion_analysis_and_object_tracking.html#void calcOpticalFlowPyrLK(InputArray prevImg, InputArray nextImg, InputArray prevPts, InputOutputArray nextPts, OutputArray status, OutputArray err, Size winSize, int maxLevel, TermCriteria criteria, int flags, double minEigThreshold)
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing.opencv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterKinectDepth;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterKinectDepthGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  JCheckBox useDepth = new JCheckBox("Use depth ");
  JCheckBox useColor = new JCheckBox("Use color ");
  JButton clearSamplePoints = new JButton("Clear Points");

  public OpenCVFilterKinectDepthGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    // updateState((OpenCVFilterKinectDepth) boundFilter.filter); noWorky -
    // would be nice to have a good reference
    useDepth.setSelected(true);
    useColor.setSelected(true);
    enableListeners();
    display.setLayout(new BorderLayout());
    JPanel f = new JPanel();
    f.add(useDepth);
    f.add(useColor);
    display.add(f, BorderLayout.CENTER);
    display.add(clearSamplePoints, BorderLayout.SOUTH);
    clearSamplePoints.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterKinectDepth filter = (OpenCVFilterKinectDepth) boundFilter.filter;
    if (o == useDepth) {
      if (useDepth.isSelected()) {
        filter.useDepth(true);
      } else {
        filter.useDepth(false);
      }
    }

    if (o == useColor) {
      if (useColor.isSelected()) {
        filter.useColor(true);
      } else {
        filter.useColor(false);
      }
    }

    if (o == clearSamplePoints) {
      filter.clearSamplePoints();
    }

    // send the updated filter to OpenCV service
    myGui.send(boundServiceName, "setFilterState", boundFilter);
  }

  // FIXME - rename to onFilterState - its a callback available from the filter
  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    updateState((OpenCVFilterKinectDepth) filterWrapper.filter);
  }

  /**
   * updates the ui based on filter data
   * 
   * @param filter
   */
  private void updateState(OpenCVFilterKinectDepth filter) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        disableListeners();
        OpenCVFilterKinectDepth myfilter = (OpenCVFilterKinectDepth) boundFilter.filter;
        if (myfilter.isDepth()) {
          useDepth.setSelected(true);
        } else {
          useDepth.setSelected(true);
        }
        if (myfilter.isColor()) {
          useColor.setSelected(true);
        } else {
          useColor.setSelected(true);
        }
        enableListeners();
      }

    });
  }

  private void enableListeners() {
    useDepth.addActionListener(this);
    useColor.addActionListener(this);
  }

  private void disableListeners() {
    useDepth.removeActionListener(this);
    useColor.removeActionListener(this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (boundFilter != null) {
      myGui.send(boundServiceName, "setFilterState", boundFilter);
    } // else - adjust gui text only
  }

}
