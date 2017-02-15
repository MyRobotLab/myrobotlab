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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterAffine;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.SliderWithText;

public class OpenCVFilterAffineGui extends OpenCVFilterGui implements ChangeListener, ActionListener {

  SliderWithText angle = new SliderWithText(JSlider.HORIZONTAL, 0, 360, 0);
  JTextField dX = new JTextField("dX", 10);
  JTextField dY = new JTextField("dY", 10);

  public OpenCVFilterAffineGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    angle.addChangeListener(this);
    dX.addActionListener(this);
    dY.addActionListener(this);

    dX.setText("0.0");
    dY.setText("0.0");
    TitledBorder title;
    JPanel j = new JPanel(new GridBagLayout());
    title = BorderFactory.createTitledBorder("Affine Config");
    j.setBorder(title);

    gc.gridx = 0;
    gc.gridy = 0;
    j.add(new JLabel("Angle"));
    // ++gc.gridx;
    j.add(angle);
    // ++gc.gridx;
    j.add(angle.value);
    display.add(j, gc);

    JPanel j2 = new JPanel(new GridBagLayout());
    j2.add(new JLabel("Delta X"));
    j2.add(dX);

    j2.add(new JLabel("Delta Y"));
    j2.add(dY);

    GridBagConstraints gc2 = new GridBagConstraints();
    gc2.gridx = 0;
    gc2.gridy = 1;
    display.add(j2, gc2);

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    // TODO Auto-generated method stub
    Object o = event.getSource();
    OpenCVFilterAffine af = (OpenCVFilterAffine) boundFilter.filter;
    if (o == dX) {
      String val = ((JTextField) o).getText();
      af.setDx(Double.valueOf(val));
    } else if (o == dY) {
      String val = ((JTextField) o).getText();
      af.setDy(Double.valueOf(val));
    }
  }

  // FIXME - update components :)
  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    boundFilter = filterWrapper;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // OpenCVFilterAffine af = (OpenCVFilterAffine) filterWrapper.filter;
        // TODO: not implemented?
      }
    });

  }

  @Override
  public void stateChanged(ChangeEvent e) {
    Object o = e.getSource();
    OpenCVFilterAffine af = (OpenCVFilterAffine) boundFilter.filter;
    if (o == angle) {
      af.setAngle(angle.getValue());
      angle.setText(angle.getValue());
    } else {
      log.info("Unknown object in state change {}", o);
    }
    setFilterState(af);

  }
}
