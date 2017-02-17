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

package org.myrobotlab.swing.opencv;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.service.SwingGui;

public class OpenCVFloorFinderGui extends OpenCVFilterGui {

  public class AdjustSlider implements ChangeListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      JSlider2 slider = (JSlider2) e.getSource();
      Object[] params = new Object[3];
      params[0] = name;
      params[1] = slider.getName();
      params[2] = slider.getValue();
      if (slider.getName().compareTo("apertureSize") == 0) {
        params[2] = slider.getValue() * 2 + 1;
      }
      myGui.send(boundServiceName, "setFilterCFG", params);
      slider.value.setText("" + slider.getValue());
    }
  }

  public class JSlider2 extends JSlider {
    private static final long serialVersionUID = 1L;
    JLabel value = new JLabel();

    public JSlider2(int vertical, int i, int j, int k) {
      super(vertical, i, j, k);
      value.setText("" + k);
    }

  }

  JSlider2 lowThreshold = new JSlider2(JSlider.HORIZONTAL, 0, 256, 0);

  JSlider2 highThreshold = new JSlider2(JSlider.HORIZONTAL, 0, 256, 256);

  JSlider2 apertureSize = new JSlider2(JSlider.HORIZONTAL, 1, 3, 1);

  AdjustSlider change = new AdjustSlider();

  public OpenCVFloorFinderGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    lowThreshold.setName("lowThreshold");
    highThreshold.setName("highThreshold");
    apertureSize.setName("apertureSize");

    lowThreshold.addChangeListener(change);
    highThreshold.addChangeListener(change);
    apertureSize.addChangeListener(change);

    GridBagConstraints gc2 = new GridBagConstraints();

    TitledBorder title;
    JPanel j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("threshold");
    j.setBorder(title);

    gc.gridx = 0;
    gc.gridy = 0;
    j.add(new JLabel("low"), gc);
    ++gc.gridx;
    j.add(lowThreshold, gc);
    ++gc.gridx;
    j.add(lowThreshold.value, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(new JLabel("high"), gc);
    ++gc.gridx;
    j.add(highThreshold, gc);
    ++gc.gridx;
    j.add(highThreshold.value, gc);

    display.add(j, gc2);
    gc2.gridy = 1;
    gc2.gridx = 0;

    j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("apertureSize");
    j.setBorder(title);
    j.add(apertureSize);
    j.add(apertureSize.value);
    display.add(j, gc2);

  }

  @Override
  public void getFilterState(FilterWrapper filterWrapper) {
    // TODO Auto-generated method stub

  }

}
