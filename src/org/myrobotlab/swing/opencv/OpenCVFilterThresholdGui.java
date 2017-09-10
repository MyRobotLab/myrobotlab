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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterThreshold;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterThresholdGui extends OpenCVFilterGui {

  public class AdjustSlider implements ChangeListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      OpenCVFilterThreshold bf = (OpenCVFilterThreshold) boundFilter.filter;
      JSlider2 slider = (JSlider2) e.getSource();
      if (slider.getName().equals("lowThreshold")) {
        bf.lowThreshold = slider.getValue();
      } else if (slider.getName().equals("lowThreshold")) {
        bf.highThreshold = slider.getValue();
      }
      slider.value.setText("" + slider.getValue());
      setFilterState(bf);
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

  // CV_THRESH_BINARY
  // CV_THRESH_BINARY_INV
  // CV_THRESH_TRUNC
  // CV_THRESH_TOZERO
  // CV_THRESH_TOZERO_INV

  JSlider2 apertureSize = new JSlider2(JSlider.HORIZONTAL, 1, 3, 1);

  JComboBox<String> type = new JComboBox<String>(new String[] { "CV_THRESH_BINARY", "CV_THRESH_BINARY_INV", "CV_THRESH_TRUNC", "CV_THRESH_TOZERO", "CV_THRESH_TOZERO_INV" });

  AdjustSlider change = new AdjustSlider();

  public OpenCVFilterThresholdGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    lowThreshold.setName("lowThreshold");
    highThreshold.setName("highThreshold");

    lowThreshold.addChangeListener(change);
    highThreshold.addChangeListener(change);

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
    title = BorderFactory.createTitledBorder("type");
    j.setBorder(title);
    // j.add(apertureSize);
    // j.add(apertureSize.value);
    j.add(type);
    display.add(j, gc2);

  }

  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        OpenCVFilterThreshold bf = (OpenCVFilterThreshold) filterWrapper.filter;
        lowThreshold.setValueIsAdjusting(true);
        lowThreshold.setValue((int) bf.lowThreshold);
        lowThreshold.setValueIsAdjusting(false);

        highThreshold.setValueIsAdjusting(true);
        highThreshold.setValue((int) bf.highThreshold);
        highThreshold.setValueIsAdjusting(false);
      }
    });

  }

}
