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
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterKinectPointCloud;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterKinectPointCloudGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  JButton clearPoints = new JButton("clear points");

  public OpenCVFilterKinectPointCloudGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    display.setLayout(new BorderLayout());
    clearPoints.addActionListener(this);
    JPanel flow = new JPanel();
    flow.add(clearPoints);
    display.add(flow, BorderLayout.CENTER);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // OpenCVFilterKinectPointCloud filter = (OpenCVFilterKinectPointCloud)
    // boundFilter.filter;
    OpenCVFilterKinectPointCloud filter = (OpenCVFilterKinectPointCloud) boundFilter.filter;
    Object o = e.getSource();
    if (o == clearPoints) {
      // send( WTH no send-to-filter ?
      filter.clearSamplePoints(); // lame
    }

    // CALL FUNCTIONS or UPDATE THE SKELETAL DATA FILTER ???
    // send the updated filter to OpenCV service
    // myGui.send(boundServiceName, "setFilterState", boundFilter); // FIXME
    // should just be broadcastFilterstate
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (boundFilter != null) {
      SliderWithText slider = (SliderWithText) e.getSource();

      if (!slider.getValueIsAdjusting()) {
        // OpenCVFilterKinectPointCloud filter = (OpenCVFilterKinectPointCloud)
        // boundFilter.filter;
        // if (slider == level) {
        // params[2] = slider.getValue() * 2 + 1;
        // qualityLevel.value.setText("" +
        // (float)qualityLevel.getValue()/100);
        // filter.qualityLevel = (float)qualityLevel.getValue()/100;
        // }

        myGui.send(boundServiceName, "setFilterState", boundFilter);
      } // else - adjust gui text only
    }
  }

  @Override // FIXME - should be onFilterState
  public void getFilterState(FilterWrapper filterWrapper) {

  }

}
