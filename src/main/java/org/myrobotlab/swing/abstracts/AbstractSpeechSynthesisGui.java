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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.swing.ServiceGui;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesisGui extends ServiceGui implements ActionListener, ChangeListener {

  // FIXME - SSML - adding combo box effects adds tags
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesisGui.class);

  JButton speakButton = new JButton(new ImageIcon(ImageIO.read(FileIO.class.getResource("/resource/Speech.png"))));
  ImageIcon statusImageOK = new ImageIcon((BufferedImage) ImageIO.read(FileIO.class.getResource("/resource/green.png")));
  ImageIcon statusImageNOK = new ImageIcon((BufferedImage) ImageIO.read(FileIO.class.getResource("/resource/red.png")));
  
  JLabel speakingState = new JLabel("                  ");
  JLabel cacheFile = new JLabel("                         ");

  // FIXME - if cloud provider with keys - "not initialized" statusImage not oke
  JLabel statusIcon = new JLabel("Not initialized", statusImageNOK, JLabel.CENTER);

  JComboBox<String> voices = new JComboBox<String>();
  JTextArea lastUtterance = new JTextArea();
  final JPanel panel = new JPanel();

  // FIXME - if is cloudservice - isReady - hasKeys ?
  // FIXME - make KeyPanel class with label
  Map<String, JPasswordField> keys = new HashMap<String, JPasswordField>();

  // TODO - add volume jlabel for volume value
  JButton save = new JButton("save");
  JSlider volume = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);

  public AbstractSpeechSynthesisGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);
    speakButton.setSelectedIcon(null);
    lastUtterance.setWrapStyleWord(true);
    lastUtterance.setLineWrap(true);

    // addTop(statusIcon, statusIcon);
    addTop(" ");
    addTop(speakingState);
    addTop(cacheFile);
    addTop(" ");
    addLeftLine("voices:", voices);
    addLeftLine("volume:", volume);
    save.setPreferredSize(new Dimension(85, 45));
    addBottom(save, speakButton);
    addLine(lastUtterance);

    // FIXME - hide status - unless cloud provider
    // FIXME - hide save buttons unless a cloud provider with keys
    speakButton.addActionListener(this);
    save.addActionListener(this);
    voices.addActionListener(this);
  }
  
  @Override
  public void subscribeGui() {
    
    subscribe("publishStartSpeaking");
    subscribe("publishEndSpeaking");
    
    subscribe("publishAudioStart");
    subscribe("publishAudioEnd");
  }

  @Override
  public void unsubscribeGui() {
    
    unsubscribe("publishStartSpeaking");
    unsubscribe("publishEndSpeaking");
    
    unsubscribe("publishAudioStart");
    unsubscribe("publishAudioEnd");
  }
  
  public void onAudioStart(AudioData data) {
    cacheFile.setText("playing : " + data.filename);
    log.debug("gui onAudioStart {}", data.toString());
  }
  
  public void onAudioEnd(AudioData data) {
    cacheFile.setText("finished : " + data.filename);
    log.debug("gui onAudioEnd {}", data.toString());
  }
  
  public String onStartSpeaking(String utterance) {
    log.debug("publishStartSpeaking - {}", utterance);
    speakingState.setText("speaking started " + utterance);
    lastUtterance.setText(utterance);
    return utterance;
  }

  public String onEndSpeaking(String utterance) {
    log.debug("publishEndSpeaking - {}", utterance);
    speakingState.setText("speaking finished");
    return utterance;
  }
  
  @Override
  public void actionPerformed(ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        if (o == voices) {
          String v = (String) voices.getSelectedItem();
          if (v.length() > 0) {
            String[] vparts = v.split(" ");
            send("setVoice", vparts[0]);
          }
        }
        if (o == speakButton) {
          send("speak", lastUtterance.getText());
        }

        if (o == save) {
          // save keys if any
          for (String keyName : keys.keySet()) {
            if (keys.get(keyName).getPassword() != null) {
              send("setKey", keyName, new String(keys.get(keyName).getPassword()));
            }
          }
          // save json
          send("save");
        }
      }
    });
  }

  // TODO - add country flag instead of code - could add language name (it comes
  // from locale)
  String display(Voice voice) {
    StringBuilder display = new StringBuilder();
    display.append(voice.getName());
    display.append((voice.getLanguageCode() == null) ? "" : " " + voice.getLanguageCode());
    display.append((voice.getGender() == null) ? "" : " " + voice.getGender());
    return display.toString();
  }

  // FIXME - CERTAINLY SHOULDN"T BE PROCESSED EVERY TIME IT SPEAKS !!!
  public void onState(AbstractSpeechSynthesis speech) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        removeListeners();

        // voice list
        List<Voice> vs = speech.getVoices();

        if (vs.size() != voices.getItemCount()) {
          voices.removeAllItems();
          for (Voice voice : vs) {
            // formatting display
            // TODO add flags for language
            voices.addItem(display(voice));
            if (speech.getVoice() != null) {
              if (voice.getName().equals(speech.getVoice().getName())) {
                voices.setSelectedItem(display(voice));
              }
            }
          }
        }

        // keys
        if (speech.getKeyNames().length != keys.size()) {
          String[] keyNames = speech.getKeyNames();
          for (int i = 0; i < keyNames.length; ++i) {
            String keyName = keyNames[i];
            JPasswordField p = new JPasswordField();
            if (speech.getKey(keyName) != null) {
              p.setText(speech.getKey(keyName));
            } else {
              error("%s required", keyName);
            }
            keys.put(keyName, p);
            addLeftLine(keyName, p);
          }

        }

        // FIXME - is it necessary ???
        if (speech.isReady()) {
          statusIcon.setIcon(statusImageOK);
        } else {
          statusIcon.setIcon(statusImageNOK);
        }

        volume.setValue((int) MathUtils.round(speech.getVolume() * 100, 0));
        lastUtterance.setText(speech.getlastUtterance());
        restoreListeners();
      }
    });

  }

  @Override
  public void stateChanged(javax.swing.event.ChangeEvent e) {
    if (swingGui != null) {
      swingGui.send(boundServiceName, "setVolume", (double) Double.valueOf(volume.getValue()) / 100);
    }
  }

  public void removeListeners() {
    voices.removeActionListener(this);
    speakButton.removeActionListener(this);
    save.removeActionListener(this);
    volume.removeChangeListener(this);
  }

  public void restoreListeners() {
    voices.addActionListener(this);
    speakButton.addActionListener(this);
    save.addActionListener(this);
    volume.addChangeListener(this);
  }
}
