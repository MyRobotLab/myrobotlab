/**
 *                    
 * @author steve (at) myrobotlab.org
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
 * http://www.voicerss.org/api/documentation.aspx
 * 
 * */
package org.myrobotlab.service;

import java.net.URLEncoder;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.VoiceRssConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import com.voicerss.tts.AudioCodec;
import com.voicerss.tts.AudioFormat;
import com.voicerss.tts.VoiceParameters;
import com.voicerss.tts.VoiceProvider;

public class VoiceRss extends AbstractSpeechSynthesis {

  transient public final static Logger log = LoggerFactory.getLogger(VoiceRss.class);
  private static final long serialVersionUID = 1L;

  public final static String VOICERSS_API_KEY = "voicerss.api.key";
  public Integer rate = 0;

  public VoiceRss(String n, String id) {
    super(n, id);
  }

  public Integer getRate() {
    return rate;
  }

  // FIXME - this needs to be universal e.g. -1.0 <=> 0 <=> 1.0
  public void setRate(Integer rate) {
    if (rate < -10) {
      rate = -10;
    }
    if (rate > 10) {
      rate = 10;
    }
    this.rate = rate;
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws Exception {

    try {
      VoiceProvider tts = new VoiceProvider(getKey(VOICERSS_API_KEY));
      String fileName = getLocalFileName(toSpeak);
      VoiceParameters params = new VoiceParameters(URLEncoder.encode(toSpeak, "UTF-8"), getVoice().getVoiceProvider().toString()); // Languages.English_UnitedStates
      params.setCodec(AudioCodec.MP3);
      params.setFormat(AudioFormat.Format_44KHZ.AF_44khz_16bit_stereo);
      params.setBase64(false);
      params.setSSML(false); // FIXME - make true
      params.setRate(rate);
      byte[] b = tts.speech(params);
      FileIO.toFile(fileName, b);
      return new AudioData(fileName);
    } catch (Exception e) {
      error("could not retrieve audio file %s", e.getMessage());
      setReady(false);
    }
    return null;
  }
  
  @Override
  public void startService() {
    super.startService();
    setReady(getKey(VOICERSS_API_KEY) != null);
  }
  
  @Override
  public void setKey(String name, String value) {
    super.setKey(name, value);
    VoiceRssConfig c = (VoiceRssConfig)config;
    c.key = value;
    setReady(true);
  }
  
  @Override
  public String getKey(String name) {
//    VoiceRssConfig c = (VoiceRssConfig)config;
//    if (c != null && c.key != null) {
//      super.setKey(VOICERSS_API_KEY, c.key);
//    }
    return super.getKey(name);
  }

  @Override
  public String[] getKeyNames() {
    return new String[] { VOICERSS_API_KEY };
  }

  @Override
  public void loadVoices() {
    // derived from
    // http://www.voicerss.org/api/documentation.aspx
    addVoice("Lei", "female", "ca-es", "ca-es"); // Catalan
    addVoice("Hui", "female", "zh-cn", "zh-cn"); // Chinese (China)
    addVoice("Jiao", "female", "zh-hk", "zh-hk"); // Chinese (Hong Kong)
    addVoice("Ju", "female", "zh-tw", "zh-tw"); // Chinese (Taiwan)
    addVoice("Ella", "female", "da-dk", "da-dk"); // Danish
    addVoice("Eva", "female", "nl-nl", "nl-nl"); // Dutch
    addVoice("Agnes", "female", "en-au", "en-au"); // English (Australia)
    addVoice("Chloe", "female", "en-ca", "en-ca"); // English (Canada)
    addVoice("Mary", "female", "en-gb", "en-gb"); // English (Great Britain)
    addVoice("Aditi", "female", "en-in", "en-in"); // English (India)
    addVoice("Sally", "female", "en-us", "en-us"); // English (United States)
    addVoice("Ansa", "female", "fi-fi", "fi-fi"); // Finnish
    addVoice("Adelle", "female", "fr-ca", "fr-ca"); // French (Canada)
    addVoice("Adyelya", "female", "fr-fr", "fr-fr"); // French (France)
    addVoice("Anna", "female", "de-de", "de-de"); // German
    addVoice("Alessandra", "female", "it-it", "it-it"); // Italian
    addVoice("Akari", "female", "ja-jp", "ja-jp"); // Japanese
    addVoice("Su", "female", "ko-kr", "ko-kr"); // Korean
    addVoice("Anette", "female", "nb-no", "nb-no"); // Norwegian
    addVoice("Agata", "female", "pl-pl", "pl-pl"); // Polish
    addVoice("Analia", "female", "pt-br", "pt-br"); // Portuguese (Brazil)
    addVoice("Balei", "female", "pt-pt", "pt-pt"); // Portuguese (Portugal)
    addVoice("Anastasia", "female", "ru-ru", "ru-ru"); // Russian
    addVoice("Isabella", "female", "es-mx", "es-mx"); // Spanish (Mexico)
    addVoice("Camila", "female", "es-es", "es-es"); // Spanish (Spain)
    addVoice("Elsa", "female", "sv-se", "sv-se"); // Swedish (Sweden)
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    // Runtime.start("gui", "SwingGui");
    VoiceRss voicerss = (VoiceRss) Runtime.start("voicerss", "VoiceRss");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();

    // add your api key
    // use gui to do this, or force it here only ONCE :
    // voicerss.setKey(VOICERSS_API_KEY, "xxxx");
    voicerss.setVoice("en-gb");
    voicerss.setRate(0);

    // TODO: fix the volume control
    // speech.setVolume(0);
    voicerss.speak("it works, yes I believe it does");
    voicerss.speak("yes yes. oh good. excellent!");
    voicerss.speak("to be or not to be that is the question, weather tis nobler in the mind to suffer the slings and arrows of ");
    voicerss.speak("I'm afraid I can't do that.");
    voicerss.speak("I am your R 2 D 2 #R2D2#");
  }

}
