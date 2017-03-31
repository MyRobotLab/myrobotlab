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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterInRange;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterInRangeGui extends OpenCVFilterGui implements ChangeListener {

  JCheckBox useHue = new JCheckBox();
  SliderWithText hueMin = new SliderWithText(JSlider.VERTICAL, 0, 256, 0);
  SliderWithText hueMax = new SliderWithText(JSlider.VERTICAL, 0, 256, 256);

  JCheckBox useSaturation = new JCheckBox();
  SliderWithText saturationMin = new SliderWithText(JSlider.VERTICAL, 0, 256, 0);
  SliderWithText saturationMax = new SliderWithText(JSlider.VERTICAL, 0, 256, 256);

  JCheckBox useValue = new JCheckBox();
  SliderWithText valueMin = new SliderWithText(JSlider.VERTICAL, 0, 256, 0);
  SliderWithText valueMax = new SliderWithText(JSlider.VERTICAL, 0, 256, 256);

  OpenCVFilterInRange myFilter = null;

  public OpenCVFilterInRangeGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    // myFilter = (OpenCVFilterInRange) myOpenCVFilter;
    // myFilter.useHue = true;

    hueMin.addChangeListener(this);
    hueMax.addChangeListener(this);
    valueMin.addChangeListener(this);
    valueMax.addChangeListener(this);
    saturationMin.addChangeListener(this);
    saturationMax.addChangeListener(this);

    useHue.addChangeListener(this);
    useSaturation.addChangeListener(this);
    useValue.addChangeListener(this);

    display.setLayout(new BorderLayout());
    JPanel p = new JPanel();
    // JPanel display = new JPanel();

    TitledBorder title;
    JPanel j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("hue");
    j.setBorder(title);
    gc.gridx = 0;
    gc.gridy = 0;
    j.add(new JLabel("enable"), gc);
    ++gc.gridx;
    j.add(useHue, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(new JLabel("  min max"), gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(hueMin, gc);
    ++gc.gridx;
    j.add(hueMax, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(hueMin.value, gc);
    ++gc.gridx;
    j.add(hueMax.value, gc);
    p.add(j);

    j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("saturation");
    j.setBorder(title);
    gc.gridx = 0;
    gc.gridy = 0;
    j.add(new JLabel("enable"), gc);
    ++gc.gridx;
    j.add(useSaturation, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(new JLabel("  min max"), gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(saturationMin, gc);
    ++gc.gridx;
    j.add(saturationMax, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(saturationMin.value, gc);
    ++gc.gridx;
    j.add(saturationMax.value, gc);
    p.add(j);

    j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("value");
    j.setBorder(title);
    gc.gridx = 0;
    gc.gridy = 0;
    j.add(new JLabel("enable"), gc);
    ++gc.gridx;
    j.add(useValue, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(new JLabel(" min max"), gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(valueMin, gc);
    ++gc.gridx;
    j.add(valueMax, gc);
    ++gc.gridy;
    gc.gridx = 0;
    j.add(valueMin.value, gc);
    ++gc.gridx;
    j.add(valueMax.value, gc);
    p.add(j);

    display.add(p, BorderLayout.CENTER);

  }

  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // OpenCVFilterInRange bf = (OpenCVFilterInRange) filterWrapper.filter;
        // TODO: not implemented ?
      }
    });
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    Object o = e.getSource();
    OpenCVFilterInRange bf = (OpenCVFilterInRange) boundFilter.filter;

    if (o == useHue) {
      bf.useHue = useHue.getModel().isSelected();
    } else if (o == hueMin) {
      bf.hueMinValue = hueMin.getValue();
      hueMin.setText(hueMin.getValue());
    } else if (o == hueMax) {
      bf.hueMaxValue = hueMax.getValue();
      hueMax.setText(hueMax.getValue());
    }

    if (o == useValue) {
      bf.useValue = useValue.getModel().isSelected();
    } else if (o == valueMin) {
      bf.valueMinValue = valueMin.getValue();
      valueMin.setText(valueMin.getValue());
    } else if (o == valueMax) {
      bf.valueMaxValue = valueMax.getValue();
      valueMax.setText(valueMax.getValue());
    }

    if (o == useSaturation) {
      bf.useSaturation = useSaturation.getModel().isSelected();
    } else if (o == saturationMin) {
      bf.saturationMinValue = saturationMin.getValue();
      saturationMin.setText(saturationMin.getValue());
    } else if (o == saturationMax) {
      bf.saturationMaxValue = saturationMax.getValue();
      saturationMax.setText(saturationMax.getValue());
    }

    setFilterState(bf);
  }

}
