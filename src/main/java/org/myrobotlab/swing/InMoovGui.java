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
package org.myrobotlab.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class InMoovGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoovGui.class);
  Runtime myRuntime = (Runtime) Runtime.getInstance();
  InMoov i01 = (InMoov) myRuntime.getService(boundServiceName);
  private final JTabbedPane inmoovPane = new JTabbedPane(JTabbedPane.TOP);
  JComboBox comboLanguage = new JComboBox();
  JCheckBox muteCheckBox = new JCheckBox("");
  JButton startVision = new JButton("startOpenCV()");
  JButton configureVision = new JButton("Configure OpenCV");
  private final JCheckBox enableOpenCV = new JCheckBox("");
  private final JLabel lblFlipCamera = new JLabel("Always ON filters ( you can add more via scrip ) :");
  private final JPanel panel = new JPanel();
  private final JCheckBox Flip = new JCheckBox("Flip Camera");
  private final JCheckBox PyramidDown = new JCheckBox("PyramidDown");
  private final JLabel notReadyLabel1 = new JLabel("InMoov SwingGui is not yet ready... ");
  private final JLabel notReadyLabel2 = new JLabel(".");

  public InMoovGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    Font f = new Font("SansSerif", Font.BOLD, 20);
    // Create TABS and content

    // general tab
    JPanel generalPanel = new JPanel();
    add(inmoovPane);
    generalPanel.setBackground(Color.WHITE);
    ImageIcon generalIcon = Util.getImageIcon("InMoov.png");
    inmoovPane.addTab("General", generalIcon, generalPanel);
    generalPanel.setLayout(new GridLayout(3, 2, 0, 0));
    notReadyLabel1.setForeground(Color.RED);

    generalPanel.add(notReadyLabel1);

    generalPanel.add(notReadyLabel2);

    JLabel lblNewLabel = new JLabel(" Language : ");
    generalPanel.add(lblNewLabel);

    for (Entry<String, String> e : InMoov.languages.entrySet()) {
      comboLanguage.addItem(e.getValue());
    }
    generalPanel.add(comboLanguage);

    JLabel MuteLabel = new JLabel("Mute startup :");
    generalPanel.add(MuteLabel);
    muteCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
    generalPanel.add(muteCheckBox);

    // vision tab
    JPanel visionPanel = new JPanel();
    visionPanel.setBackground(Color.WHITE);
    ImageIcon visionIcon = Util.getImageIcon("OpenCV.png");
    inmoovPane.addTab("Vision", visionIcon, visionPanel);
    visionPanel.setLayout(new GridLayout(3, 2, 0, 0));
    visionPanel.add(startVision);
    visionPanel.add(configureVision);
    JLabel visionLabel = new JLabel("Enable OpenCV :");
    visionPanel.add(visionLabel);
    visionPanel.add(enableOpenCV);

    visionPanel.add(lblFlipCamera);

    visionPanel.add(panel);

    panel.add(Flip);

    panel.add(PyramidDown);

    // listeners
    restoreListeners();
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        if (o == comboLanguage) {
          send("setLanguage", (String) InMoov.languagesIndex.get(comboLanguage.getSelectedIndex()));
        }
        if (o == muteCheckBox) {
          send("setMute", muteCheckBox.isSelected());
        }
        if (o == configureVision) {
          swingGui.setActiveTab(boundServiceName + ".opencv");
        }
        if (o == startVision) {
          send("startOpenCV");
        }
        if (o == enableOpenCV) {
          i01.vision.setOpenCVenabled(enableOpenCV.isSelected());
        }
        if (o == Flip) {
          if (Flip.isSelected()) {
            i01.vision.addPreFilter("Flip");
          } else {
            i01.vision.removePreFilter("Flip");
          }
        }
        if (o == PyramidDown) {
          if (PyramidDown.isSelected()) {
            i01.vision.addPreFilter("PyramidDown");
          } else {
            i01.vision.removePreFilter("PyramidDown");
          }
        }
      }
    });
  }

  public void onState(final InMoov i01) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeListeners();
        comboLanguage.setSelectedItem(i01.languages.get(i01.getLanguage()));
        muteCheckBox.setSelected(i01.getMute());
        if (i01.vision != null) {
          enableOpenCV.setSelected(i01.vision.openCVenabled);
          Flip.setSelected(i01.vision.preFilters.contains("Flip"));
          PyramidDown.setSelected(i01.vision.preFilters.contains("PyramidDown"));
        }
        if (i01.opencv == null) {
          startVision.setText("startOpenCV()");
          startVision.setEnabled(true);
          configureVision.setEnabled(false);
        } else {
          startVision.setText("OpenCV started...");
          startVision.setEnabled(false);
          configureVision.setEnabled(true);
        }
        restoreListeners();
      }
    });
  }

  public void removeListeners() {
    comboLanguage.removeActionListener(this);
    muteCheckBox.removeActionListener(this);
    startVision.removeActionListener(this);
    configureVision.removeActionListener(this);
    Flip.removeActionListener(this);
    enableOpenCV.removeActionListener(this);
    PyramidDown.removeActionListener(this);
  }

  public void restoreListeners() {
    comboLanguage.addActionListener(this);
    muteCheckBox.addActionListener(this);
    startVision.addActionListener(this);
    configureVision.addActionListener(this);
    enableOpenCV.addActionListener(this);
    Flip.addActionListener(this);
    PyramidDown.addActionListener(this);
  }

}