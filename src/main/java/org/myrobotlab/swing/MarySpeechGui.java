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

package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.MarySpeech;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.image.BufferedImage;
import org.myrobotlab.io.FileIO;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.imageio.ImageIO;
import java.io.IOException;

public class MarySpeechGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MarySpeechGui.class);

  Runtime myRuntime = (Runtime) Runtime.getInstance();
  JPanel marylPanel = new JPanel();
  JComboBox comboVoice = new JComboBox();
  JTextArea lastUtterance = new JTextArea();
  private final JPanel panel = new JPanel();
  BufferedImage speakButtonPic = ImageIO.read(FileIO.class.getResource("/resource/Shoutbox.png"));

  JButton speakButton = new JButton(new ImageIcon(speakButtonPic));

  public MarySpeechGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);
    marylPanel.setLayout(new GridLayout(2, 2, 0, 0));

    JLabel voice = new JLabel("Voice :");
    marylPanel.add(voice);

    marylPanel.add(comboVoice);

    marylPanel.add(panel);
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    JLabel test = new JLabel("Test :");
    panel.add(test);
    speakButton.setSelectedIcon(null);
    panel.add(speakButton);

    add(marylPanel);
    lastUtterance.setWrapStyleWord(true);
    lastUtterance.setLineWrap(true);

    marylPanel.add(lastUtterance);

    // listeners
    comboVoice.addActionListener(this);
    speakButton.addActionListener(this);

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        if (o == comboVoice) {
          send("setVoice", comboVoice.getSelectedItem());
        }
        if (o == speakButton) {
          send("speak", lastUtterance.getText());
        }

      }
    });
  }

  @Override
  public void subscribeGui() {

  }

  @Override
  public void unsubscribeGui() {

  }

  public void onState(MarySpeech mary) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeListeners();
        if (comboVoice.getItemCount() == 0 && !(mary.voices==null)) {
          mary.voices.forEach((v) -> comboVoice.addItem(v));
        }
        comboVoice.setSelectedItem((mary.getVoice()));
        lastUtterance.setText(mary.lastUtterance);
        restoreListeners();

      }
    });
  }

  public void removeListeners() {
    comboVoice.removeActionListener(this);
    speakButton.removeActionListener(this);
  }

  public void restoreListeners() {
    comboVoice.addActionListener(this);
    speakButton.addActionListener(this);

  }

}
