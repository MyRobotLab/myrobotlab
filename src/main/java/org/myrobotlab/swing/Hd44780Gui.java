/**
 *                    
 * @author moz4r (at) myrobotlab.org
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
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Hd44780;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class Hd44780Gui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AudioFileGui.class);
  ImageIcon readyOK = new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "green.png")));
  ImageIcon readyNOK = new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "red.png")));
  JLabel isReadyIcon = new JLabel(readyNOK, JLabel.CENTER);
  JTextArea screenContent = new JTextArea(4, 20);
  ButtonGroup backLightGroup = new ButtonGroup();
  JRadioButton on = new JRadioButton("On");
  JRadioButton off = new JRadioButton("Off");
  JButton save = new JButton("Push to LCD");

  public Hd44780Gui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);

    JPanel backLightPanel = new JPanel(new GridLayout(0, 2));
    backLightGroup.add(on);
    backLightGroup.add(off);
    backLightPanel.add(on);
    backLightPanel.add(off);
    Color aColor = Color.decode("#c9ed51");
    screenContent.setBackground(aColor);
    Font myFont = new Font("SansSerif", Font.PLAIN, 22);
    screenContent.setFont(myFont);
    addLine("Ready :  ", isReadyIcon);
    addLine("Text :  ", screenContent);
    addLine("BackLight :  ", backLightPanel);
    addLine("Save :  ", save);
    save.addActionListener(this);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        Object o = event.getSource();

        if (o == save) {
          send("clear");
          //parse jtextarea and send lines to lcd
          String[] arrayOfLines = screenContent.getText().split("\n");
          int i = 0;
          for (String line : arrayOfLines) {
            i++;
            send("display", line, i);
          }
          send("setBackLight", on.isSelected());
        }
      }
    });
  }

  public void onState(final Hd44780 lcd) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        if (lcd.isReady()) {
          isReadyIcon.setIcon(readyOK);
        } else {
          isReadyIcon.setIcon(readyNOK);
        }
        StringBuilder str = new StringBuilder();
        for (int i = 1; i < 5; i++) {
          if (lcd.screenContent.get(i) != null) {
            str.append(lcd.screenContent.get(i)).append("\n");
          } else {
            str.append("\n");
          }
        }
        screenContent.setText(str.toString());
        if (lcd.getBackLight() == true) {
          on.setSelected(true);
        } else {
          off.setSelected(true);
        }

      }
    });
  }

}
