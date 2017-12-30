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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilterFaceRecognizer;
import org.myrobotlab.service.SwingGui;

public class OpenCVFilterFaceRecognizerGui extends OpenCVFilterGui implements ActionListener {

  private JTextField trainName = new JTextField("trainName", 10);
  private JCheckBox trainMode = new JCheckBox();

  private JButton saveButton = new JButton("Save");
  private JButton loadButton = new JButton("Load");
  
  public OpenCVFilterFaceRecognizerGui(String boundFilterName, String boundServiceName, SwingGui myService) {
    super(boundFilterName, boundServiceName, myService);
    // build the config for this filter.
    TitledBorder title = BorderFactory.createTitledBorder("Face Recognizer");
    JPanel j = new JPanel(new GridBagLayout());
    j.setBorder(title);
    // text box for persons name
    trainName.addActionListener(this);
    trainName.setText("");

    // check box for if you're training or not.
    // by default not checked.
    trainMode.setSelected(false);
    trainMode.addActionListener(this);
    // the person's name that you're going to train for.
    // assemble those elements into the UI.
    JPanel jp = new JPanel(new GridBagLayout());
    jp.add(new JLabel("Train:"));
    jp.add(trainMode);
    jp.add(new JLabel("Name:"));
    jp.add(trainName);
    display.add(jp, gc);
    
    saveButton.addActionListener(this);
    loadButton.addActionListener(this);
    JPanel jp2 = new JPanel(new GridBagLayout());
    jp2.add(saveButton);
    jp2.add(loadButton);
    
    display.add(jp2, gc);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    OpenCVFilterFaceRecognizer bf = (OpenCVFilterFaceRecognizer) boundFilter.filter;
    if (o == trainMode) {

      if (((JCheckBox) o).isSelected()) {
        bf.setMode(OpenCVFilterFaceRecognizer.Mode.TRAIN);
        bf.setTrainName(trainName.getText());
      } else {
        // This means we went from training mode to recognition mode
        // assume that we've created some new training images.
        // so we can invoke the training method
        try {
          bf.train();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        // done training. start recognizing...
        bf.setMode(OpenCVFilterFaceRecognizer.Mode.RECOGNIZE);
      }
      // this is the checkbox
      // when we select this, we should go into train mode
      // when we deselect this, we should train the model and
      // go into recognition mode
    } else if (o == trainName) {
      // the opencv set the current name
      // that the cv filter is accumulating images for.
      bf.setTrainName(((JTextField) o).getText());
    } else if (o == saveButton) {
      try {
        bf.save();
      } catch (IOException e1) {
        log.warn("Error saving face recognition model {}", e1.getLocalizedMessage());
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    } else if (o == loadButton) {
      try {
        bf.load();
      } catch (IOException e1) {
        log.warn("Error loading face recognition model {}", e1.getLocalizedMessage());
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
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
  public void getFilterState(FilterWrapper filterWrapper) {
    // TODO: what the heck are we doing with this method?!
  }

  // @Override
  // public void getFilterState(final FilterWrapper filterWrapper) {
  // SwingUtilities.invokeLater(new Runnable() {
  // @Override
  // public void run() {
  // OpenCVFilterFaceDetect bf = (OpenCVFilterFaceDetect) filterWrapper.filter;
  // // cascadeFile.setSelectedItem(bf.cascadeFile);
  // }
  // });
  // }

}
