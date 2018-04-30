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
  // stored inside json, this must be UNIQUE identifiers
  String voirssVoice;
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

    voiceList.add("ca-es");// Catalan
    voiceList.add("zh-cn");// Chinese (China)
    voiceList.add("cn");// Chinese (China)
    voiceList.add("zh-hk");// Chinese (Hong Kong)
    voiceList.add("zh-tw");// Chinese (Taiwan)
    voiceList.add("da-dk");// Danish
    voiceList.add("nl-nl");// Dutch
    voiceList.add("en-au");// English (Australia)
    voiceList.add("en-ca");// English (Canada)
    voiceList.add("en-gb");// English (Great Britain)
    voiceList.add("en-in");// English (India)
    voiceList.add("en-us");// English (United States)
    voiceList.add("en");// English (United States)
    voiceList.add("fi-fi");// Finnish
    voiceList.add("fr-ca");// French (Canada)
    voiceList.add("fr-fr");// French (France)
    voiceList.add("fr");// French (France)
    voiceList.add("de-de");// German
    voiceList.add("de");// German
    voiceList.add("it-it");// Italian
    voiceList.add("ja-jp");// Japanese
    voiceList.add("ja");// Japanese
    voiceList.add("jp");// Japanese
    voiceList.add("ko-kr");// Korean
    voiceList.add("nb-no");// Norwegian
    voiceList.add("pl-pl");// Polish
    voiceList.add("pt-br");// Portuguese (Brazil)
    voiceList.add("pt-pt");// Portuguese (Portugal)
    voiceList.add("ru-ru");// Russian
    voiceList.add("es-mx");// Spanish (Mexico)
    voiceList.add("es-es");// Spanish (Spain)
    voiceList.add("es");// Spanish (Spain)
    voiceList.add("sv-se");// Swedish (Sweden)
    setEngineError("Online");
    setEngineStatus(true);
    subSpeechStartService();
  }

  @Override
  public List<String> getVoices() {
    return voiceList;
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
    setVoice(this.voirssVoice);
    broadcastState();

  }

  @Override
  public String[] getKeys() {
    String[] Keys = new String[2];
    security.loadStore();
    Keys[0] = security.getSecret("voicerss.user.api");
    return Keys;
  }
}
