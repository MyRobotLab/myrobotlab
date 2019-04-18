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

package org.myrobotlab.swing.abstracts;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.swing.ServiceGui;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesisGui extends ServiceGui implements ActionListener, ChangeListener {

  // FIXME - SSML - adding combo box effects adds tags
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesisGui.class);

  JButton speakButton = new JButton(new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "Speech.png"))));

  ImageIcon readyOK = new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "green.png")));
  ImageIcon readyNOK = new ImageIcon(ImageIO.read(new File(Util.getResourceDir() + File.separator + "red.png")));

  JLabel speakingState = new JLabel("not speaking");
  JLabel isReadyIcon = new JLabel(readyNOK, JLabel.CENTER);
  JLabel cacheFile = new JLabel("no file");
  JLabel audioState = new JLabel("not playing");
  JLabel generationTime = new JLabel("generation time : 0 ms");

  JCheckBox useSSML = new JCheckBox();

  JComboBox<String> voices = new JComboBox<String>();
  JTextArea lastUtterance = new JTextArea();
  final JPanel panel = new JPanel();

  // FIXME - if is cloudservice - isReady - hasKeys ?
  // FIXME - make KeyPanel class with label
  Map<String, JPasswordField> keys = new HashMap<String, JPasswordField>();

  // TODO - add volume jlabel for volume value
  JButton save = new JButton("save");
  // FIXME - doesnt work because AudioProcessor won't close file :(
  // JButton purgeFile = new JButton("purge file");
  // JButton purgeCache = new JButton("purge cache");
  JButton pause = new JButton("pause");
  JButton resume = new JButton("resume");
  JButton stop = new JButton("stop");
  JSlider volume = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);

  JComboBox<String> voiceEffectFiles = new JComboBox<String>();

  final AbstractSpeechSynthesisGui self;

  AudioData lastAudioData;

  public AbstractSpeechSynthesisGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);
    self = this;
    speakButton.setSelectedIcon(null);
    lastUtterance.setWrapStyleWord(true);
    lastUtterance.setLineWrap(true);

    Runtime runtime = Runtime.getInstance();

    addTop("Class : " + runtime.getService(boundServiceName).getClass().getSimpleName(), isReadyIcon);
    addTop("speaking state : ", speakingState);
    addTop("generation time : ", generationTime);
    addTop("audio state : ", audioState);
    // generationTime, audioState
    // addTop("cache file : ", purgeFile);
    addTop(2, cacheFile);

    addLeftLine("voices:", voices);
    addLeftLine("effects", voiceEffectFiles);
    addLeftLine("volume:", volume);
    addLeftLine("use SSML:", useSSML);
    save.setPreferredSize(new Dimension(85, 45));
    pause.setPreferredSize(new Dimension(85, 45));
    resume.setPreferredSize(new Dimension(85, 45));
    stop.setPreferredSize(new Dimension(85, 45));
    // purgeCache.setPreferredSize(new Dimension(170, 45));
    addBottom(save, /* purgeCache, */ pause, resume, stop, speakButton);
    addLine(lastUtterance);

    // FIXME - hide status - unless cloud provider
    // FIXME - hide save buttons unless a cloud provider with keys

    voices.addActionListener(this);
    voiceEffectFiles.addActionListener(this);

    speakButton.addActionListener(this);
    pause.addActionListener(this);
    resume.addActionListener(this);
    stop.addActionListener(this);
    save.addActionListener(this);
    useSSML.addActionListener(this);
    // purgeCache.addActionListener(this);
    // purgeFile.addActionListener(this);
  }

  @Override
  public void subscribeGui() {

    subscribe("getVoiceEffectFiles");

    subscribe("publishSpeechRequested");
    subscribe("publishGenerationTime");

    subscribe("publishStartSpeaking");
    subscribe("publishEndSpeaking");

    subscribe("publishAudioStart");
    subscribe("publishAudioEnd");

    send("getVoiceEffectFiles");
  }

  @Override
  public void unsubscribeGui() {

    unsubscribe("getVoiceEffectFiles");

    unsubscribe("publishSpeechRequested");
    unsubscribe("publishGenerationTime");

    unsubscribe("publishStartSpeaking");
    unsubscribe("publishEndSpeaking");

    unsubscribe("publishAudioStart");
    unsubscribe("publishAudioEnd");
  }

  public void onVoiceEffectFiles(List<File> files) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        voiceEffectFiles.removeActionListener(self);
        voiceEffectFiles.removeAllItems();
        voiceEffectFiles.addItem("");

        for (File file : files) {
          log.info("adding voice effect file {}", file.getName());
          voiceEffectFiles.addItem(displayEffectFile(file));
        }

        voiceEffectFiles.addActionListener(self);
      }
    });
  }

  public void onAudioStart(AudioData data) {
    audioState.setText("audio start");
    // File f = new File(data.filename);
    cacheFile.setText(data.getFileName());
    lastAudioData = data;
    log.debug("gui onAudioStart {}", data.toString());
  }

  public void onAudioEnd(AudioData data) {
    audioState.setText(String.format("audio end %d ms", data.getLength()));
    // cacheFile.setText("audio end : " + data.filename);
    log.debug("gui onAudioEnd {} {} ms", data.toString(), data.getLength());
  }

  public void onStartSpeaking(String utterance) {
    log.debug("publishStartSpeaking - {}", utterance);
    String display = utterance;
    if (display.length() > 50) {
      display = String.format("%s ...", display.substring(0, 50));
    }
    speakingState.setText(String.format("started speaking : \"%s\"", display));
  }

  public void onEndSpeaking(String utterance) {
    log.debug("publishEndSpeaking - {}", utterance);
    String display = utterance;
    if (display.length() > 50) {
      display = String.format("%s ...", display.substring(0, 50));
    }
    speakingState.setText(String.format("end speaking     : \"%s\"", display));
  }

  public void onGenerationTime(Long time) {
    generationTime.setText(String.format("generation time %d ms", time));
    // generationTime.setText(String.format("%d", time));
  }

  /**
   * original text requested to speak - not necessarily the same as
   * publishStartSpeaking text since publishStartSpeaking is after a
   * pre-processor/parser
   * 
   * @param toSpeak
   *          - speech requested to speak
   */
  public void onSpeechRequested(String toSpeak) {
    lastUtterance.setText(toSpeak);
  }

  protected String displayEffectFile(File file) {
    String ret = "";
    String filename = file.getName();
    int pos = filename.indexOf(".");
    if (pos > 0) {
      return String.format("#%s#", filename.substring(0, pos));
    }
    return ret;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();
        if (o == voices) {
          String voiceKey = (String) voices.getSelectedItem();
          send("setVoice", voiceKey);
        }

        if (o == voiceEffectFiles) {
          lastUtterance.setText(lastUtterance.getText() + " " + (String) voiceEffectFiles.getSelectedItem());
        }

        if (o == speakButton) {
          send("speak", lastUtterance.getText());
        }

        if (o == pause) {
          send("pause");
        }

        if (o == resume) {
          send("resume");
        }

        if (o == stop) {
          send("stop");
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
          send("getVoices");
          send("broadcastState");
        }
      }
    });
  }

  // TODO - add country flag instead of code - could add language name (it comes
  // from locale)

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
            voices.addItem(voice.toString());
          }
        }

        if (voices.getItemCount() > 0 && speech.getVoice() != null && !speech.getVoice().getName().equals(voices.getSelectedItem())) {
          voices.setSelectedItem(speech.getVoice().toString());
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

        volume.setValue((int) MathUtils.round(speech.getVolume() * 100, 0));
        lastUtterance.setText(speech.getlastUtterance());
        if (speech.isReady()) {
          isReadyIcon.setIcon(readyOK);
        } else {
          isReadyIcon.setIcon(readyNOK);
        }

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
