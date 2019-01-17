/**
 *                    
 * @author GroG (at) myrobotlab.org
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

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterFaceDetectGui extends OpenCVFilterGui {

  JComboBox<String> cascadeFile = new JComboBox<String>(new String[] { "haarcascade_eye.xml", "haarcascade_eye_tree_eyeglasses.xml", "haarcascade_frontalface_alt.xml",
      "haarcascade_frontalface_alt2.xml", "haarcascade_frontalface_alt_tree.xml", "haarcascade_frontalface_default.xml", "haarcascade_fullbody.xml",
      "haarcascade_lefteye_2splits.xml", "haarcascade_lowerbody.xml", "haarcascade_mcs_eyepair_big.xml", "haarcascade_mcs_eyepair_small.xml", "haarcascade_mcs_leftear.xml",
      "haarcascade_mcs_lefteye.xml", "haarcascade_mcs_mouth.xml", "haarcascade_mcs_nose.xml", "haarcascade_mcs_rightear.xml", "haarcascade_mcs_righteye.xml",
      "haarcascade_mcs_upperbody.xml", "haarcascade_profileface.xml", "haarcascade_righteye_2splits.xml", "haarcascade_upperbody.xml" });

  JRadioButton doCannyPruning = new JRadioButton("Do Canny Pruning ");
  JRadioButton doRoughSearch = new JRadioButton("Do Rough Search ");
  JRadioButton featureMax = new JRadioButton("Feature Max ");
  JRadioButton biggestObject = new JRadioButton("Biggest Object ");
  JRadioButton magicValue = new JRadioButton("Magic Value ");
  JRadioButton scaleObject = new JRadioButton("Scale Object ");

  public OpenCVFilterFaceDetectGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    cascadeFile.addActionListener(this);

    display.setLayout(new BorderLayout());

    JPanel north = new JPanel();
    north.add(new JLabel("haar cascade file  "));
    north.add(cascadeFile);
    display.add(north, BorderLayout.NORTH);

    JPanel center = new JPanel();
    Box b = Box.createVerticalBox();
    b.add(doCannyPruning);
    b.add(doRoughSearch);
    b.add(featureMax);
    b.add(biggestObject);
    b.add(magicValue);
    b.add(scaleObject);

    /*
     * center.add(doCannyPruning); center.add(doRoughSearch);
     * center.add(featureMax); center.add(biggestObject);
     * center.add(magicValue); center.add(scaleObject);
     */
    center.add(b);

    display.add(center, BorderLayout.CENTER);
  }

  public void enableListeners(boolean b) {
    if (b) {
      doCannyPruning.addActionListener(this);
      doRoughSearch.addActionListener(this);
      featureMax.addActionListener(this);
      biggestObject.addActionListener(this);
      magicValue.addActionListener(this);
      scaleObject.addActionListener(this);
      scaleObject.addActionListener(this);
      cascadeFile.addActionListener(this);
    } else {
      doCannyPruning.removeActionListener(this);
      doRoughSearch.removeActionListener(this);
      featureMax.removeActionListener(this);
      biggestObject.removeActionListener(this);
      magicValue.removeActionListener(this);
      scaleObject.removeActionListener(this);
      scaleObject.removeActionListener(this);
      cascadeFile.removeActionListener(this);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterFaceDetect bf = (OpenCVFilterFaceDetect) boundFilter.filter;

    if (o == cascadeFile) {
      bf.cascadeFile = (String) cascadeFile.getSelectedItem();
      bf.cascade = null;
    }
    setFilterState(bf);
  }

  @Override
  public void getFilterState(final FilterWrapper filterWrapper) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        enableListeners(false);
        OpenCVFilterFaceDetect bf = (OpenCVFilterFaceDetect) filterWrapper.filter;
        cascadeFile.setSelectedItem(bf.cascadeFile);
        enableListeners(true);
      }
    });
  }

}
