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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
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

public class VoiceRss extends AbstractSpeechSynthesis {

  transient public final static Logger log = LoggerFactory.getLogger(VoiceRss.class);
  private static final long serialVersionUID = 1L;
  // stored inside json, this must be UNIQUE identifiers
  HashMap<String, String> voiceInJsonConfig;
  // end

  public Integer rate = 0;

  public VoiceRss(String n) {
    super(n);

  }

  public void startService() {
    super.startService();
    security = (Security) startPeer("security");

    // TODO: be country/language aware when asking for voiceList?
    // maybe have a get voiceList by language/locale
    // Arabic

    getVoiceList().add("ca-es");// Catalan
    getVoiceList().add("zh-cn");// Chinese (China)
    getVoiceList().add("cn");// Chinese (China)
    getVoiceList().add("zh-hk");// Chinese (Hong Kong)
    getVoiceList().add("zh-tw");// Chinese (Taiwan)
    getVoiceList().add("da-dk");// Danish
    getVoiceList().add("nl-nl");// Dutch
    getVoiceList().add("en-au");// English (Australia)
    getVoiceList().add("en-ca");// English (Canada)
    getVoiceList().add("en-gb");// English (Great Britain)
    getVoiceList().add("en-in");// English (India)
    getVoiceList().add("en-us");// English (United States)
    getVoiceList().add("en");// English (United States)
    getVoiceList().add("fi-fi");// Finnish
    getVoiceList().add("fr-ca");// French (Canada)
    getVoiceList().add("fr-fr");// French (France)
    getVoiceList().add("fr");// French (France)
    getVoiceList().add("de-de");// German
    getVoiceList().add("de");// German
    getVoiceList().add("it-it");// Italian
    getVoiceList().add("ja-jp");// Japanese
    getVoiceList().add("ja");// Japanese
    getVoiceList().add("jp");// Japanese
    getVoiceList().add("ko-kr");// Korean
    getVoiceList().add("nb-no");// Norwegian
    getVoiceList().add("pl-pl");// Polish
    getVoiceList().add("pt-br");// Portuguese (Brazil)
    getVoiceList().add("pt-pt");// Portuguese (Portugal)
    getVoiceList().add("ru-ru");// Russian
    getVoiceList().add("es-mx");// Spanish (Mexico)
    getVoiceList().add("es-es");// Spanish (Spain)
    getVoiceList().add("es");// Spanish (Spain)
    getVoiceList().add("sv-se");// Swedish (Sweden)
    setEngineError("Online");
    setEngineStatus(true);
    subSpeechStartService();
  }

  @Override
  public List<String> getVoices() {
    return getVoiceList();
  }

  public Integer getRate() {
    return rate;
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

  public String getLocalDirectory(SpeechSynthesis provider) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.

    return "audioFile" + File.separator + provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8");

  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    Runtime.start("gui", "SwingGui");
    VoiceRss speech = (VoiceRss) Runtime.start("speech", "VoiceRss");
    // add your api key
    // use gui to do this, or force it here only ONCE :
    // speech.setKeys("xxx");
    // speech.setVoice("en-gb");
    speech.setRate(0);

    // TODO: fix the volume control
    // speech.setVolume(0);
    speech.speakBlocking("it works, yes I believe it does");
    speech.speakBlocking("yes yes. oh good. excellent!");
    speech.speakBlocking("to be or not to be that is the question, weather tis nobler in the mind to suffer the slings and arrows of ");
    speech.speakBlocking("I'm afraid I can't do that.");
    speech.speak("I am your R 2 D 2 #R2D2#");
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
    subGetMetaData(meta);
    meta.addPeer("security", "Security", "security");
    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.setCloudService(true);
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    meta.addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
    meta.addDependency("com.voicerss", "tts", "1.0");
    return meta;
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) throws UnsupportedEncodingException {
    VoiceProvider tts = new VoiceProvider(getKeys()[0]);

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
      setEngineError("API error ?");
      setEngineStatus(false);
      return null;
    }

  }

  public void setKeys(String keyId) {
    setKeys(keyId, null);

  }

  @Override
  public void setKeys(String keyId, String keyIdSecret) {
    security.addSecret("voicerss.user.api", keyId);
    security.saveStore();
    getVoices();
    setVoice(getVoice());
    broadcastState();

  }

  @Override
  public String[] getKeys() {
    String[] Keys = new String[2];
    security.loadStore();
    Keys[0] = security.getSecret("voicerss.user.api");
    return Keys;
  }

  @Override
  public String getVoiceInJsonConfig() {
    if (voiceInJsonConfig == null) {
      voiceInJsonConfig = new HashMap<String, String>();
    }

    return voiceInJsonConfig.get(this.getClass().getSimpleName());
  }

  @Override
  public void setVoiceInJsonConfig(String voice) {
    voiceInJsonConfig.put(this.getClass().getSimpleName(), voice);

  }
}
