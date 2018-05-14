/**
 *                    
 * @author moz4r (at) myrobotlab.org
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

package org.myrobotlab.swing.abstracts;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.swing.ServiceGui;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesisGui extends ServiceGui implements ActionListener {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesisGui.class);
  BufferedImage speakButtonPic = ImageIO.read(FileIO.class.getResource("/resource/Shoutbox.png"));

  JButton speakButton = new JButton(new ImageIcon(speakButtonPic));
  ImageIcon statusImageOK = new ImageIcon((BufferedImage) ImageIO.read(FileIO.class.getResource("/resource/green.png")));
  ImageIcon statusImageNOK = new ImageIcon((BufferedImage) ImageIO.read(FileIO.class.getResource("/resource/red.png")));

  JLabel statusIcon = new JLabel("Not initialized", statusImageNOK, JLabel.CENTER);
  protected JPanel speechGuiPanel = new JPanel();
  JComboBox<String> comboVoice = new JComboBox<String>();
  JComboBox<String> comboVoiceEffects = new JComboBox<String>();
  JTextArea lastUtterance = new JTextArea();
  final JPanel panel = new JPanel();

  // used for api if needed by service
  protected JLabel keyIdLabel = new JLabel("key Id :");
  protected JLabel keyIdSecretLabel = new JLabel("Secret :");
  protected JPanel apiKeylPanel = new JPanel();
  JPasswordField keyId = new JPasswordField();
  protected JPasswordField keyIdSecret = new JPasswordField();
  JButton save = new JButton("Save");
  protected JLabel apiKeyLabel = new JLabel("API Keys :");

  public AbstractSpeechSynthesisGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);

    speechGuiPanel.setLayout(new GridLayout(5, 2, 0, 0));
    JLabel status = new JLabel("Status :");
    speechGuiPanel.add(status);
    speechGuiPanel.add(statusIcon);

    JLabel voice = new JLabel("Voice :");
    speechGuiPanel.add(voice);

    speechGuiPanel.add(comboVoice);

    JLabel voiceEffects = new JLabel("Voice emotions :");
    speechGuiPanel.add(voiceEffects);
    speechGuiPanel.add(comboVoiceEffects);
    speechGuiPanel.add(panel);
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    JLabel test = new JLabel("Test :");
    panel.add(test);
    speakButton.setSelectedIcon(null);
    panel.add(speakButton);

    add(speechGuiPanel);
    lastUtterance.setWrapStyleWord(true);
    lastUtterance.setLineWrap(true);

    speechGuiPanel.add(lastUtterance);

    // used for api if needed by service
    apiKeylPanel.setLayout(new GridLayout(3, 2, 0, 0));

    apiKeylPanel.add(keyIdLabel);
    apiKeylPanel.add(keyIdSecretLabel);
    apiKeylPanel.add(keyId);
    apiKeylPanel.add(keyIdSecret);
    apiKeylPanel.add(save);

    // listeners
    comboVoice.addActionListener(this);
    comboVoiceEffects.addActionListener(this);
    speakButton.addActionListener(this);
    save.addActionListener(this);
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

        if (o == save) {
          send("setKeys", keyId.getText(), keyIdSecret.getText());
        }
        if (o == comboVoiceEffects) {
          send("speak", comboVoiceEffects.getSelectedItem());
        }

      }
    });
  }

  public void onState(SpeechSynthesis speech) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeListeners();
        if (speech.getEngineStatus()) {
          statusIcon.setIcon(statusImageOK);

        } else {
          statusIcon.setIcon(statusImageNOK);
        }
        statusIcon.setText(speech.getEngineError());

        if (comboVoiceEffects.getItemCount() == 0 && !(speech.getVoiceEffects() == null)) {
          comboVoiceEffects.removeAll();
          speech.getVoiceEffects().forEach((v) -> comboVoiceEffects.addItem(v));
          comboVoiceEffects.setEnabled(true);
        }
        if ((speech.getVoiceEffects() == null)) {

          comboVoiceEffects.addItem("Offline");
          comboVoiceEffects.setEnabled(false);
        }
        if (comboVoice.getItemCount() == 0 && !(speech.getVoices() == null)) {
          speech.getVoices().forEach((v) -> comboVoice.addItem(v));
        }
        if (comboVoice.getItemCount() > 0) {
          comboVoice.setSelectedItem((speech.getVoice()));
        }
        if (speech.getKeys() == null || speech.getKeys()[0] == null || speech.getKeys()[0].isEmpty()) {
          keyId.setBackground(Color.red);

        } else {
          keyId.setBackground(Color.green);
          keyId.setText(speech.getKeys()[0]);
        }
        if (speech.getKeys() == null || speech.getKeys()[1] == null || speech.getKeys()[1].isEmpty()) {
          keyIdSecret.setBackground(Color.red);

        } else {
          keyIdSecret.setBackground(Color.green);
          keyIdSecret.setText(speech.getKeys()[1]);
        }
        lastUtterance.setText(speech.getlastUtterance());
        restoreListeners();

      }
    });

  }

  public void removeListeners() {
    comboVoice.removeActionListener(this);
    speakButton.removeActionListener(this);
    save.removeActionListener(this);
    comboVoiceEffects.removeActionListener(this);
  }

  public void restoreListeners() {
    comboVoice.addActionListener(this);
    speakButton.addActionListener(this);
    save.addActionListener(this);
    comboVoiceEffects.addActionListener(this);
  }
}
