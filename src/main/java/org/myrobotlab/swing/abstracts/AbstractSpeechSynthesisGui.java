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
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
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
  JComboBox<String> comboVoiceEmotions = new JComboBox<String>();
  JTextArea lastUtterance = new JTextArea();
  final JPanel panel = new JPanel();

  // used for effects like in mary
  protected final JPanel EffectpanelTLeft = new JPanel();
  protected final JPanel EffectpanelTRight = new JPanel();
  protected final JPanel EffectpanelBLeft = new JPanel();
  protected final JPanel EffectpanelBRight = new JPanel();

  protected JTextField effetsParameters = new JTextField();
  protected JLabel ComboEffectLabel = new JLabel("Effect :");
  protected JLabel ComboEffectLabel2 = new JLabel("Parameters :");
  protected JComboBox<String> comboEffects = new JComboBox<String>();
  protected JTextArea selectedEffects = new JTextArea();
  protected JButton addEffect = new JButton("Add");
  protected JButton updateEffect = new JButton("Update");
  // used for api if needed by service
  protected JLabel keyIdLabel = new JLabel("key Id :");
  protected JLabel keyIdSecretLabel = new JLabel("Secret :");
  protected JPanel apiKeylPanel = new JPanel();
  JPasswordField keyId = new JPasswordField();
  protected JPasswordField keyIdSecret = new JPasswordField();
  JButton save = new JButton("Save");
  protected JLabel apiKeyLabel = new JLabel("API Keys :");
  protected JLabel volumeLabel = new JLabel("Volume :");
  JSlider volume = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);

  private class SliderListener implements ChangeListener {

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {

      if (swingGui != null) {

        swingGui.send(boundServiceName, "setVolume", (double) Double.valueOf(volume.getValue()) / 100);
      } else {
        log.error("can not send message myService is null");

      }
    }
  }

  SliderListener volumeListener = new SliderListener();

  public AbstractSpeechSynthesisGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);

    speechGuiPanel.setLayout(new GridLayout(7, 2, 0, 0));
    JLabel status = new JLabel("Status :");
    speechGuiPanel.add(status);
    speechGuiPanel.add(statusIcon);

    JLabel voice = new JLabel("Voice :");
    speechGuiPanel.add(voice);

    speechGuiPanel.add(comboVoice);

    JLabel voiceEffects = new JLabel("Voice emotions :");
    speechGuiPanel.add(voiceEffects);
    speechGuiPanel.add(comboVoiceEmotions);
    speechGuiPanel.add(volumeLabel);
    speechGuiPanel.add(volume);
    speechGuiPanel.add(panel);
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    JLabel test = new JLabel("Test :");
    panel.add(test);
    speakButton.setSelectedIcon(null);
    panel.add(speakButton);

    add(speechGuiPanel);
    lastUtterance.setWrapStyleWord(true);
    lastUtterance.setLineWrap(true);
    selectedEffects.setWrapStyleWord(true);
    selectedEffects.setLineWrap(true);

    speechGuiPanel.add(lastUtterance);

    // used for api if needed by service
    apiKeylPanel.setLayout(new GridLayout(4, 2, 0, 0));

    apiKeylPanel.add(keyIdLabel);
    apiKeylPanel.add(keyIdSecretLabel);
    apiKeylPanel.add(keyId);
    apiKeylPanel.add(keyIdSecret);
    apiKeylPanel.add(save);

    // listeners
    comboVoice.addActionListener(this);
    comboVoiceEmotions.addActionListener(this);
    speakButton.addActionListener(this);
    save.addActionListener(this);
    comboEffects.addActionListener(this);
    updateEffect.addActionListener(this);
    addEffect.addActionListener(this);

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
        if (o == comboVoiceEmotions) {
          send("speak", comboVoiceEmotions.getSelectedItem());
        }
        if (o == comboEffects) {
          send("setSelectedEffect", comboEffects.getSelectedItem());
        }
        if (o == updateEffect) {
          send("setAudioEffects", selectedEffects.getText());
        }
        if (o == addEffect) {
          String output = (String) comboEffects.getSelectedItem();
          if (!effetsParameters.getText().isEmpty()) {
            output += "(" + effetsParameters.getText() + ")";
          }
          if (!selectedEffects.getText().isEmpty()) {
            output = "+" + output;
          }

          send("setAudioEffects", selectedEffects.getText() + output);
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

        if (comboVoiceEmotions.getItemCount() == 0 && !(speech.getVoiceEffects() == null)) {
          comboVoiceEmotions.removeAll();
          speech.getVoiceEffects().forEach((v) -> comboVoiceEmotions.addItem(v));
          comboVoiceEmotions.setEnabled(true);
        }
        if ((speech.getVoiceEffects() == null)) {

          comboVoiceEmotions.addItem("Offline");
          comboVoiceEmotions.setEnabled(false);
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
        if (comboEffects.getItemCount() > 0 && speech.getSelectedEffect() != null) {
          comboVoice.setSelectedItem((speech.getSelectedEffect()));
          effetsParameters.setText(speech.getEffectsList().get(speech.getSelectedEffect()));
        }

        volume.setValue((int) MathUtils.round(speech.getVolume() * 100, 0));
        lastUtterance.setText(speech.getlastUtterance());
        selectedEffects.setText(speech.getAudioEffects());
        restoreListeners();

      }
    });

  }

  public void removeListeners() {
    comboVoice.removeActionListener(this);
    speakButton.removeActionListener(this);
    save.removeActionListener(this);
    comboVoiceEmotions.removeActionListener(this);
    volume.removeChangeListener(volumeListener);
    comboEffects.removeActionListener(this);

  }

  public void restoreListeners() {
    comboVoice.addActionListener(this);
    speakButton.addActionListener(this);
    save.addActionListener(this);
    comboVoiceEmotions.addActionListener(this);
    volume.addChangeListener(volumeListener);
    comboEffects.addActionListener(this);

  }
}
