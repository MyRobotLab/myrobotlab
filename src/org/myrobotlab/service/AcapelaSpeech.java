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

package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
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

/**
 * AcapelaSpeech - Use the acapela group speech synthesis API. This makes a HTTP
 * request to generate an MP3 that represents the text to be spoken for a given
 * voice. That mp3 is then cached and played back by the AudioFile service.
 * 
 */
@Deprecated // close source, closed service, no fun
public class AcapelaSpeech extends AbstractSpeechSynthesis implements TextListener, AudioListener {

  transient public final static Logger log = LoggerFactory.getLogger(AcapelaSpeech.class);
  private static final long serialVersionUID = 1L;
  // default voice
  public String voice = "Ryan";
  public HashSet<String> voices = new HashSet<String>();
  // this is a peer service.
  transient AudioFile audioFile = null;
  // TODO: fix the volume control
  // private float volume = 1.0f;
  
  transient CloseableHttpClient client;

  public AcapelaSpeech(String n) {
    super(n);

    // TODO: be country/language aware when asking for voices?
    // maybe have a get voices by language/locale
    // Arabic
    voices.add("Leila");
    voices.add("Mehdi");
    voices.add("Nizar");
    voices.add("Salma");
    // Catalan
    voices.add("Laia");
    // Czech
    voices.add("Eliska");
    // Danish
    voices.add("Mette");
    voices.add("Rasmus");
    // Dutch ( Belgium )
    voices.add("Zoe");
    voices.add("Jeroen");
    voices.add("JeroenHappy");
    voices.add("JeroenSad");
    voices.add("Sofie");
    // Dutch ( Netherlands )
    voices.add("Jasmijn");
    voices.add("Daan");
    voices.add("Femke");
    voices.add("Max");
    // English (AU)
    voices.add("Tyler");
    voices.add("Lisa");
    voices.add("Olivia");
    voices.add("Liam");
    // English ( India )
    voices.add("Deepa");
    // English ( Scottish )
    voices.add("Rhona");
    // English (UK)
    voices.add("Rachel");
    voices.add("Graham");
    voices.add("Harry");
    voices.add("Lucy");
    voices.add("Nizareng");
    voices.add("Peter");
    voices.add("PeterHappy");
    voices.add("PeterSad");
    voices.add("QueenElizabeth");
    voices.add("Rosie");
    // English ( USA )
    voices.add("Sharon");
    voices.add("Ella");
    voices.add("EmilioEnglish");
    voices.add("Josh");
    voices.add("Karen");
    voices.add("Kenny");
    voices.add("Laura");
    voices.add("Micah");
    voices.add("Nelly");
    voices.add("Rod");
    voices.add("Ryan");
    voices.add("Saul");
    voices.add("Scott");
    voices.add("Tracy");
    voices.add("ValeriaEnglish");
    voices.add("Will");
    voices.add("WillBadGuy");
    voices.add("WillFromAfar");
    voices.add("WillHappy");
    voices.add("WillLittleCreature");
    // Faroese
    voices.add("Hanna");
    voices.add("Hanus");
    // Finnish
    voices.add("Sanna");
    // French ( Belgium )
    voices.add("Justine");
    // French ( Canada )
    voices.add("Louise");
    // French ( France )
    voices.add("Manon");
    voices.add("Alice");
    voices.add("Antoine");
    voices.add("AntoineFromAfar");
    voices.add("AntoineHappy");
    voices.add("AntoineSad");
    voices.add("Bruno");
    voices.add("Claire");
    voices.add("Manon");
    voices.add("Julie");
    voices.add("Margaux");
    voices.add("MargauxHappy");
    voices.add("MargauxSad");
    // German
    voices.add("Claudia");
    voices.add("Andreas");
    voices.add("Jonas");
    voices.add("Julia");
    voices.add("Klaus");
    voices.add("Lea");
    voices.add("Sarah");
    // Greek
    voices.add("Dimitris");
    voices.add("DimitrisHappy");
    voices.add("DimitrisSad");
    // Italian
    voices.add("Fabiana");
    voices.add("Chiara");
    voices.add("Vittorio");
    // Japanese
    voices.add("Sakura");
    // Korean
    voices.add("Minji");
    // Mandarin
    voices.add("Lulu");
    // Norwegian
    voices.add("Bente");
    voices.add("Kari");
    voices.add("Olav");
    // Polish
    voices.add("Monika");
    voices.add("Ania");
    // Portuguese ( Brazil )
    voices.add("Marcia");
    // Portuguese ( Portugal )
    voices.add("Celia");
    // Russian
    voices.add("Alyona");
    // Sami ( North )
    voices.add("Biera");
    voices.add("Elle");
    // Spanish ( Spain )
    voices.add("Ines");
    voices.add("Antonio");
    voices.add("Maria");
    // Spanish ( US )
    voices.add("Rodrigo");
    voices.add("Emilio");
    voices.add("Rosa");
    voices.add("Valeria");
    // Swedish
    voices.add("Elin");
    voices.add("Emil");
    voices.add("Emma");
    voices.add("Erik");
    // Swedish ( Finland )
    voices.add("Samuel");
    // Swedish ( Gothenburg )
    voices.add("Kal");
    // Swedish ( Scanian )
    voices.add("Mia");
    // Turkish
    voices.add("Ipek");
  }

  public void startService() {
    super.startService();
    if (client == null) {
        // new MultiThreadedHttpConnectionManager()
        client = HttpClients.createDefault();
    }
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
    this.voice = voice;
    return voices.contains(voice);
  }

  @Override
  public void setLanguage(String l) {
    // FIXME ! "MyLanguages", "sonid8" ???
    // FIXME - implement !!!
  }

  public String getMp3Url(String toSpeak) {
    HttpPost post = null;
    try {
      
      // request form & send text
      String url = "http://www.acapela-group.com/demo-tts/DemoHTML5Form_V2.php?langdemo=Powered+by+%3Ca+href%3D%22http%3A%2F%2Fwww.acapela-vaas.com"
          + "%22%3EAcapela+Voice+as+a+Service%3C%2Fa%3E.+For+demo+and+evaluation+purpose+only%2C+for+commercial+use+of+generated+sound+files+please+go+to+"
          + "%3Ca+href%3D%22http%3A%2F%2Fwww.acapela-box.com%22%3Ewww.acapela-box.com%3C%2Fa%3E";
      post = new HttpPost(url);
      List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      nvps.add(new BasicNameValuePair("MyLanguages", "sonid10"));
      nvps.add(new BasicNameValuePair("MySelectedVoice", voice));
      nvps.add(new BasicNameValuePair("MyTextForTTS", toSpeak));
      nvps.add(new BasicNameValuePair("t", "1"));
      nvps.add(new BasicNameValuePair("SendToVaaS", ""));
      UrlEncodedFormEntity formData = new UrlEncodedFormEntity(nvps, "UTF-8");
      post.setEntity(formData);
      HttpResponse response = client.execute(post);
      log.info(response.getStatusLine().toString());
      HttpEntity entity = response.getEntity();
      byte[] b = FileIO.toByteArray(entity.getContent());
      // parse out mp3 file url
      String mp3Url = null;
      String data = new String(b);
      String startTag = "var myPhpVar = '";
      int startPos = data.indexOf(startTag);
      if (startPos != -1) {
        int endPos = data.indexOf("';", startPos);
        if (endPos != -1) {
          mp3Url = data.substring(startPos + startTag.length(), endPos);
        }
      }
      if (mp3Url == null) {
        error("could not get mp3 back from Acapela server !");
      }
      return mp3Url;
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      if (post != null) {
        post.releaseConnection();
      }
    }

    return null;

  }

  public byte[] getRemoteFile(String toSpeak) {

    String mp3Url = getMp3Url(toSpeak);
    HttpGet get = null;
    byte[] b = null;
    try {
      HttpResponse response = null;
      // fetch file
      get = new HttpGet(mp3Url);
      log.info("mp3Url {}", mp3Url);
      // get mp3 file & save to cache
      response = client.execute(get);
      log.info("got {}", response.getStatusLine());
      HttpEntity entity = response.getEntity();
      // cache the mp3 content
      b = FileIO.toByteArray(entity.getContent());
      EntityUtils.consume(entity);
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      if (get != null) {
        get.releaseConnection();
      }
    }

    return b;
  }

  @Override
  public boolean speakBlocking(String toSpeak) throws IOException {
    log.info("speak blocking {}", toSpeak);

    if (voice == null) {
      log.warn("voice is null! setting to default: Ryan");
      voice = "Ryan";
    }
    String localFileName = getLocalFileName(this, toSpeak, "mp3");
    String filename = AudioFile.globalFileCacheDir + File.separator + localFileName;
    if (!audioFile.cacheContains(localFileName)) {
      byte[] b = getRemoteFile(toSpeak);
      audioFile.cache(localFileName, b, toSpeak);
    }
    invoke("publishStartSpeaking", toSpeak);
    audioFile.playBlocking(filename);
    invoke("publishEndSpeaking", toSpeak);
    log.info("Finished waiting for completion.");
    return false;
  }

  @Override
  public void setVolume(float volume) {
    // TODO: fix the volume control
    log.warn("Volume control not implemented in AcapelaSpeech yet.");
  }

  @Override
  public float getVolume() {
    return 0;
  }

  @Override
  public void interrupt() {
    // TODO: Implement me!
  }

  @Override
  public void onText(String text) {
    log.info("ON Text Called: {}", text);
    try {
      speak(text);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public String getLanguage() {
    return null;
  }

  // HashSet<String> audioFiles = new HashSet<String>();
  Stack<String> audioFiles = new Stack<String>();

  public AudioData speak(String toSpeak) throws IOException {
    // this will flip to true on the audio file end playing.
    AudioData ret = null;
    log.info("speak {}", toSpeak);
    if (voice == null) {
      log.warn("voice is null! setting to default: Ryan");
      voice = "Ryan";
    }
    String filename = this.getLocalFileName(this, toSpeak, "mp3");
    if (audioFile.cacheContains(filename)) {
      ret = audioFile.playCachedFile(filename);
      utterances.put(ret, toSpeak);
      return ret;
    }
    audioFiles.push(filename);
    byte[] b = getRemoteFile(toSpeak);
    audioFile.cache(filename, b, toSpeak);
    ret = audioFile.playCachedFile(filename);
    utterances.put(ret, toSpeak);
    return ret;

  }

  @Override
  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.
    return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator + DigestUtils.md5Hex(toSpeak) + "."
        + audioFileType;
  }

  @Override
  public void addEar(SpeechRecognizer ear) {
    // TODO: move this to a base class. it's basically the same for all
    // mouths/ speech synth stuff.
    // when we add the ear, we need to listen for request confirmation
    addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
    addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
  }

  public void onRequestConfirmation(String text) {
    try {
      speakBlocking(String.format("did you say. %s", text));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public List<String> getLanguages() {
    // TODO Auto-generated method stub
    ArrayList<String> ret = new ArrayList<String>();
    // FIXME - add iso language codes currently supported e.g. en en_gb de
    // etc..
    return ret;
  }

  // audioData to utterance map TODO: revisit the design of this
  HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();
private Object language;

  @Override
  public String publishStartSpeaking(String utterance) {
    log.info("publishStartSpeaking {}", utterance);
    return utterance;
  }

  @Override
  public String publishEndSpeaking(String utterance) {
    log.info("publishEndSpeaking {}", utterance);
    return utterance;
  }

  @Override
  public void onAudioStart(AudioData data) {
    log.info("onAudioStart {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishStartSpeaking", utterance);
    }
  }

  @Override
  public void onAudioEnd(AudioData data) {
    log.info("onAudioEnd {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishEndSpeaking", utterance);
      utterances.remove(data);
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      // Runtime.start("webgui", "WebGui");
      AcapelaSpeech speech = (AcapelaSpeech) Runtime.start("speech", "AcapelaSpeech");
      speech.setVoice("Rod");
      // TODO: fix the volume control
      // speech.setVolume(0);
      speech.speakBlocking("this is another test");
      speech.speakBlocking("does this work");
      speech.speakBlocking("uh oh");
      speech.speakBlocking("to be or not to be that is the question, weather tis nobler in the mind to suffer the slings and arrows of ");
      speech.speakBlocking("I'm afraid I can't do that.");

      // speech.speak("this is a test");
      //
      // speech.speak("i am saying something new once again again");
      // speech.speak("one");
      // speech.speak("two");
      // speech.speak("three");
      // speech.speak("four");
      /*
       * speech.speak("what is going on"); //speech.speakBlocking(
       * "Répète après moi"); speech.speak( "hello there my name is ryan");
       * speech.speak("hello world"); speech.speak("one two three four");
       */
      // arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
      // arduino.connect(port);
      // arduino.broadcastState();
    } catch (Exception e) {
      Logging.logError(e);
    }
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
    ServiceType meta = new ServiceType(AcapelaSpeech.class.getCanonicalName());
    
    meta.addDescription("is a proprietary cloud service, currently returns speech and background music");
    meta.addCategory("speech");
    
    meta.setLicenseProprietary();
    meta.setCloudService(true);
    
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");

    //End of support
    meta.setAvailable(false);
    return meta;
  }

}
