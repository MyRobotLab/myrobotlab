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
import org.myrobotlab.opencv.OpenCVFilterMiniXception;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterMiniXceptionGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  SliderWithText confidence = new SliderWithText(JSlider.HORIZONTAL, 0, 100, 25);
  JLabel confidenceText = new JLabel("25");

  SliderWithText slopSize = new SliderWithText(JSlider.HORIZONTAL, 0, 100, 10);
  JLabel slopSizeText = new JLabel("10");

  
  public OpenCVFilterMiniXceptionGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    JPanel north = new JPanel();
    north.add(new JLabel("confidence threshold"));
    north.add(confidence);
    north.add(confidenceText);
    
    JPanel north2 = new JPanel();
    north2.add(new JLabel("box slop"));
    north2.add(slopSize);
    north2.add(slopSizeText);
    
    display.setLayout(new BorderLayout());
    display.add(north, BorderLayout.NORTH);
    display.add(north2, BorderLayout.SOUTH);

    confidence.addChangeListener(this);
    slopSize.addChangeListener(this);
    
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterMiniXception bf = (OpenCVFilterMiniXception) boundFilter.filter;
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
        OpenCVFilterMiniXception bf = (OpenCVFilterMiniXception) filterWrapper.filter;
        confidenceText.setText(Double.toString((bf.getConfidence() * 100 )));
        confidence.setValue((int) (bf.getConfidence() * 100 ));
        
        slopSizeText.setText(Integer.toString(bf.getBoxSlop()));
        slopSize.setValue((int) (bf.getBoxSlop() ));
      }
    });
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    OpenCVFilterMiniXception bf = (OpenCVFilterMiniXception) boundFilter.filter;
    Object o = e.getSource();
    if (o == confidence) {
      bf.setConfidence(confidence.getValue() / 100.0);
      confidenceText.setText(Integer.toString(confidence.getValue()));
    } else if (o == slopSize) {
      bf.setBoxSlop(slopSize.getValue());
      slopSizeText.setText(Integer.toString(slopSize.getValue()));
    }

    // setFilterState(bf);
  }

}
