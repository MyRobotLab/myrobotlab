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
 * http://docs.opencv.org/modules/video/doc/motion_analysis_and_object_tracking.html#void calcOpticalFlowPyrLK(InputArray prevImg, InputArray nextImg, InputArray prevPts, InputOutputArray nextPts, OutputArray status, OutputArray err, Size winSize, int maxLevel, TermCriteria criteria, int flags, double minEigThreshold)
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing.opencv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterLKOpticalTrack;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterLKOpticalTrackGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  SliderWithText maxPointCount = new SliderWithText(JSlider.HORIZONTAL, 0, 256, 30);
  SliderWithText minDistance = new SliderWithText(JSlider.HORIZONTAL, 0, 256, 10);
  SliderWithText qualityLevel = new SliderWithText(JSlider.HORIZONTAL, 0, 100, 0.05f);
  SliderWithText blockSize = new SliderWithText(JSlider.HORIZONTAL, 1, 10, 3);
  JButton getFeatures = new JButton("get features");
  JButton clearPoints = new JButton("clear points");

  public OpenCVFilterLKOpticalTrackGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    maxPointCount.setName("maxCount");
    minDistance.setName("minDistance");
    qualityLevel.setName("qualityLevel");
    blockSize.setName("blockSize");

    maxPointCount.addChangeListener(this);
    minDistance.addChangeListener(this);
    qualityLevel.addChangeListener(this);
    blockSize.addChangeListener(this);
    getFeatures.addActionListener(this);
    clearPoints.addActionListener(this);

    gc.gridy = 0;
    gc.gridx = 0;

    ++gc.gridy;
    gc.gridx = 0;
    display.add(new JLabel("max points  "), gc);
    ++gc.gridy;
    gc.gridwidth = 2;
    display.add(maxPointCount, gc);
    gc.gridwidth = 1;
    gc.gridx += 2;
    display.add(maxPointCount.value, gc);

    ++gc.gridy;
    gc.gridx = 0;
    display.add(new JLabel("min distance"), gc);
    ++gc.gridy;
    gc.gridwidth = 2;
    display.add(minDistance, gc);
    gc.gridwidth = 1;
    gc.gridx += 2;
    display.add(minDistance.value, gc);

    ++gc.gridy;
    gc.gridx = 0;
    display.add(new JLabel("quality     "), gc);
    ++gc.gridy;
    gc.gridwidth = 2;
    display.add(qualityLevel, gc);
    gc.gridwidth = 1;
    gc.gridx += 2;
    display.add(qualityLevel.value, gc);

    ++gc.gridy;
    gc.gridx = 0;
    display.add(new JLabel("block size  "), gc);
    ++gc.gridy;
    gc.gridwidth = 2;
    display.add(blockSize, gc);
    gc.gridwidth = 1;
    gc.gridx += 2;
    display.add(blockSize.value, gc);

    ++gc.gridy;
    gc.gridx = 0;
    display.add(getFeatures, gc);

    ++gc.gridx;
    display.add(clearPoints, gc);

    // set the hook
    MRLListener listener = new MRLListener("publishFilterState", myService.getName(), "setFilterState");
    myService.send(boundServiceName, "addListener", listener);
    // thread wait?
    // send the event
    myService.send(boundServiceName, "publishFilterState", boundFilterName);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterLKOpticalTrack filter = (OpenCVFilterLKOpticalTrack) boundFilter.filter;

    if (o == getFeatures) {
      filter.needTrackingPoints = true;
    } else if (o == clearPoints) {
      filter.clearPoints = true;
    }

    // send the updated filter to OpenCV service
    myGui.send(boundServiceName, "setFilterState", boundFilter);
  }

  @Override
  public void getFilterState(FilterWrapper boundFilter) {
    /*
     * if (this.boundFilter == null) { this.boundFilter = boundFilter; }
     */

    // OpenCVFilterLKOpticalTrack bf = (OpenCVFilterLKOpticalTrack)
    // boundFilter.filter;
    maxPointCount.setValueIsAdjusting(true);
    minDistance.setValueIsAdjusting(true);
    qualityLevel.setValueIsAdjusting(true);
    blockSize.setValueIsAdjusting(true);

    // maxPointCount.setValue(bf.maxPointCount);

    // minDistance.setValue((int)bf.minDistance);
    //
    // qualityLevel.setValue((int)bf.qualityLevel * 100);
    // qualityLevel.value.setText("" + bf.qualityLevel);

    // blockSize.setValue((int)bf.blockSize);

    blockSize.setValueIsAdjusting(false);
    qualityLevel.setValueIsAdjusting(false);
    minDistance.setValueIsAdjusting(false);
    maxPointCount.setValueIsAdjusting(false);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (boundFilter != null) {
      SliderWithText slider = (SliderWithText) e.getSource();
      Object[] params = new Object[3];
      params[0] = name;
      params[1] = slider.getName();
      params[2] = slider.getValue();

      if (!slider.getValueIsAdjusting()) {
        // OpenCVFilterLKOpticalTrack filter = (OpenCVFilterLKOpticalTrack)
        // boundFilter.filter;
        if (slider.getName().equals("qualityLevel")) {
          // params[2] = slider.getValue() * 2 + 1;
          // qualityLevel.value.setText("" +
          // (float)qualityLevel.getValue()/100);
          // filter.qualityLevel = (float)qualityLevel.getValue()/100;
        } else if (slider.getName().equals("maxCount")) {
          maxPointCount.value.setText("" + maxPointCount.getValue());
          // filter.maxPointCount = maxPointCount.getValue();
        } else if (slider.getName().equals("minDistance")) {
          minDistance.value.setText("" + minDistance.getValue());
          // filter.minDistance = minDistance.getValue();
        } else if (slider.getName().equals("blockSize")) {
          blockSize.value.setText("" + blockSize.getValue());
          // filter.blockSize = blockSize.getValue();
        }

        myGui.send(boundServiceName, "setFilterState", boundFilter);
      } // else - adjust gui text only
    }
  }

}
