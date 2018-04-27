/**
 *                    
 * @author steve (at) myrobotlab.org
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
package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

import com.voicerss.tts.AudioCodec;
import com.voicerss.tts.AudioFormat;
//import com.voicerss.tts.Languages;
//import com.voicerss.tts.SpeechDataEvent;
//import com.voicerss.tts.SpeechDataEventListener;
//import com.voicerss.tts.SpeechErrorEvent;
//import com.voicerss.tts.SpeechErrorEventListener;
import com.voicerss.tts.VoiceParameters;
import com.voicerss.tts.VoiceProvider;

public class VoiceRss extends AbstractSpeechSynthesis implements TextListener, AudioListener {

  transient public final static Logger log = LoggerFactory.getLogger(VoiceRss.class);
  private static final long serialVersionUID = 1L;
  // default voice
  public String voice = "fr-fr";
  public HashSet<String> voices = new HashSet<String>();
  public String key = "0000";
  public Integer rate = 0;
  public boolean credentialsError = false;

  public VoiceRss(String n) {
    super(n);

    // TODO: be country/language aware when asking for voices?
    // maybe have a get voices by language/locale
    // Arabic

    voices.add("ca-es");// Catalan
    voices.add("zh-cn");// Chinese (China)
    voices.add("cn");// Chinese (China)
    voices.add("zh-hk");// Chinese (Hong Kong)
    voices.add("zh-tw");// Chinese (Taiwan)
    voices.add("da-dk");// Danish
    voices.add("nl-nl");// Dutch
    voices.add("en-au");// English (Australia)
    voices.add("en-ca");// English (Canada)
    voices.add("en-gb");// English (Great Britain)
    voices.add("en-in");// English (India)
    voices.add("en-us");// English (United States)
    voices.add("en");// English (United States)
    voices.add("fi-fi");// Finnish
    voices.add("fr-ca");// French (Canada)
    voices.add("fr-fr");// French (France)
    voices.add("fr");// French (France)
    voices.add("de-de");// German
    voices.add("de");// German
    voices.add("it-it");// Italian
    voices.add("ja-jp");// Japanese
    voices.add("ja");// Japanese
    voices.add("jp");// Japanese
    voices.add("ko-kr");// Korean
    voices.add("nb-no");// Norwegian
    voices.add("pl-pl");// Polish
    voices.add("pt-br");// Portuguese (Brazil)
    voices.add("pt-pt");// Portuguese (Portugal)
    voices.add("ru-ru");// Russian
    voices.add("es-mx");// Spanish (Mexico)
    voices.add("es-es");// Spanish (Spain)
    voices.add("es");// Spanish (Spain)
    voices.add("sv-se");// Swedish (Sweden)
  }

  public void startService() {
    super.startService();
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");
  }

  public AudioFile getAudioFile() {
    return audioFile;
  }

  @Override
  public ArrayList<String> getVoices() {
    return new ArrayList<String>(voices);
  }

  @Override
  public String getVoice() {
    return voice;
  }

  @Override
  public boolean setVoice(String voice) {
    // backward compatibility because voicerss doesnt support setvoice
    // return true;
    return voices.contains(voice);
  }

  public String getKey() {
    return key;
  }

  public Integer getRate() {
    return rate;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setRate(Integer rate) {
    if (rate < -10) {
      rate = -10;
    }
    if (rate > 10) {
      rate = 10;
    }
    this.rate = rate;
  }

  public List<String> getLanguages() {
    log.warn("not yet implemented");
    return null;
  }

  @Override
  public void setLanguage(String l) {
    // todo : implement generic method & language code

    log.warn("not yet implemented");
  }

  // HashSet<String> audioFiles = new HashSet<String>();
  Stack<String> audioFiles = new Stack<String>();

  public String getLocalDirectory(SpeechSynthesis provider) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.

    return "audioFile" + File.separator + provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8");

  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    VoiceRss speech = (VoiceRss) Runtime.start("speech", "VoiceRss");
    speech.setKey("your-api");
    speech.setLanguage("en-gb");
    speech.setRate(0);

    // TODO: fix the volume control
    // speech.setVolume(0);
    speech.speakBlocking("it works, yes I believe it does");
    speech.speakBlocking("yes yes. oh good. excellent!");
    speech.speakBlocking("to be or not to be that is the question, weather tis nobler in the mind to suffer the slings and arrows of ");
    speech.speak("I'm afraid I can't do that.");

  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(VoiceRss.class.getCanonicalName());
    meta.addDescription("VoiceRss speech synthesis service.");
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.setCloudService(true);
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    meta.addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
    meta.addDependency("com.voicerss", "tts", "1.0");
    return meta;
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) throws UnsupportedEncodingException {
    VoiceProvider tts = new VoiceProvider(getKey());

    VoiceParameters params = new VoiceParameters(URLEncoder.encode(toSpeak, "UTF-8"), getVoice()); // Languages.English_UnitedStates
    params.setCodec(AudioCodec.MP3);
    params.setFormat(AudioFormat.Format_44KHZ.AF_44khz_16bit_stereo);
    params.setBase64(false);
    params.setSSML(false);
    params.setRate(rate);

    try {
      return tts.speech(params);
    } catch (Exception e) {
      error("VoiceRSS crashed : %s : ", e);
      return null;
    }

  }

}
