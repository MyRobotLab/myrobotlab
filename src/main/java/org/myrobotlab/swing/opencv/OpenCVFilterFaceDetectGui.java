/**
 *                    
 * @author GroG (at) myrobotlab.org
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterFaceDetectGui extends OpenCVFilterGui implements ActionListener {

  JComboBox<String> cascadeFile = new JComboBox<String>(new String[] { "haarcascade_eye.xml", "haarcascade_eye_tree_eyeglasses.xml", "haarcascade_frontalface_alt.xml",
      "haarcascade_frontalface_alt2.xml", "haarcascade_frontalface_alt_tree.xml", "haarcascade_frontalface_default.xml", "haarcascade_fullbody.xml",
      "haarcascade_lefteye_2splits.xml", "haarcascade_lowerbody.xml", "haarcascade_mcs_eyepair_big.xml", "haarcascade_mcs_eyepair_small.xml", "haarcascade_mcs_leftear.xml",
      "haarcascade_mcs_lefteye.xml", "haarcascade_mcs_mouth.xml", "haarcascade_mcs_nose.xml", "haarcascade_mcs_rightear.xml", "haarcascade_mcs_righteye.xml",
      "haarcascade_mcs_upperbody.xml", "haarcascade_profileface.xml", "haarcascade_righteye_2splits.xml", "haarcascade_upperbody.xml" });

  public OpenCVFilterFaceDetectGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);

    cascadeFile.addActionListener(this);
    display.add(new JLabel("haar cascade file  "));
    display.add(cascadeFile);

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
        OpenCVFilterFaceDetect bf = (OpenCVFilterFaceDetect) filterWrapper.filter;
        cascadeFile.setSelectedItem(bf.cascadeFile);
      }
    });
  }

}
