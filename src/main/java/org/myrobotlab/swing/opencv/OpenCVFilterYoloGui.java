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
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing.opencv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterYoloGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  SliderWithText confidence = new SliderWithText(JSlider.HORIZONTAL, 0, 100, 25);
  JLabel confidenceText = new JLabel("25");

  public OpenCVFilterYoloGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    JPanel north = new JPanel();
    north.add(new JLabel("confidence threshold"));
    north.add(confidence);
    north.add(confidenceText);
    display.setLayout(new BorderLayout());
    display.add(north, BorderLayout.NORTH);
    confidence.addChangeListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterYolo bf = (OpenCVFilterYolo) boundFilter.filter;
    // setFilterState(bf);
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
        OpenCVFilterYolo bf = (OpenCVFilterYolo) filterWrapper.filter;
        confidenceText.setText("" + (bf.getConfidenceThreshold() * 100));
        confidence.setValue((int) (bf.getConfidenceThreshold() * 100));
      }
    });
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    OpenCVFilterYolo bf = (OpenCVFilterYolo) boundFilter.filter;
    Object o = e.getSource();
    if (o == confidence) {
      bf.setConfidenceThreshold(confidence.getValue() / 100);
    }
    // setFilterState(bf);
  }

}
