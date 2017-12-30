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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;
import org.myrobotlab.vision.FilterWrapper;
import org.myrobotlab.vision.OpenCVFilterCanny;

public class OpenCVFilterCannyGui extends OpenCVFilterGui implements ChangeListener {

  SliderWithText lowThreshold = new SliderWithText(JSlider.HORIZONTAL, 0, 256, 0);
  SliderWithText highThreshold = new SliderWithText(JSlider.HORIZONTAL, 0, 256, 256);
  SliderWithText apertureSize = new SliderWithText(JSlider.HORIZONTAL, 1, 3, 1); // docs
  // say
  // 1
  // 3
  // 5
  // 7
  // ...
  // but
  // 1
  // craches
  // -
  // will
  // use
  // 3
  // 5
  // or
  // 7

  public OpenCVFilterCannyGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    lowThreshold.addChangeListener(this);
    highThreshold.addChangeListener(this);
    apertureSize.addChangeListener(this);

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

  // FIXME - update components :)
  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // OpenCVFilterCanny bf = (OpenCVFilterCanny) filterWrapper.filter;
        // TODO: not implemented?
      }
    });

  }

  @Override
  public void stateChanged(ChangeEvent e) {

    Object o = e.getSource();
    OpenCVFilterCanny bf = (OpenCVFilterCanny) boundFilter.filter;

    if (o == apertureSize) {
      bf.apertureSize = apertureSize.getValue() * 2 + 1;
      apertureSize.setText(bf.apertureSize);
    } else if (o == highThreshold) {
      bf.highThreshold = highThreshold.getValue();
      highThreshold.setText(highThreshold.getValue());
    } else if (o == lowThreshold) {
      bf.lowThreshold = lowThreshold.getValue();
      lowThreshold.setText(lowThreshold.getValue());
    }

    setFilterState(bf);

  }

}
