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

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterKinectNavigate;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterKinectNavigateGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  JLabel selectedPoint = new JLabel("here xxxxxxxxxxxxxxxxx");

  public OpenCVFilterKinectNavigateGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    // display.add(input, BorderLayout.NORTH);

    // set the hook
    MRLListener listener = new MRLListener("publishFilterState", myService.getName(), "setFilterState");
    myService.send(boundServiceName, "addListener", listener);
    // thread wait?
    // send the event
    myService.send(boundServiceName, "publishFilterState", boundFilterName);
    display.setLayout(new BorderLayout());
    display.add(selectedPoint, BorderLayout.CENTER);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterKinectNavigate filter = (OpenCVFilterKinectNavigate) boundFilter.filter;

    /*
     * if (o == getFeatures) { filter.needTrackingPoints = true; } else if (o ==
     * clearPoints) { filter.clearPoints = true; }
     */

    // send the updated filter to OpenCV service
    myGui.send(boundServiceName, "setFilterState", boundFilter);
  }

  @Override
  public void getFilterState(FilterWrapper boundFilter) {
    log.info("{}", boundFilter);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (boundFilter != null) {
      myGui.send(boundServiceName, "setFilterState", boundFilter);
    } // else - adjust gui text only
  }

}
