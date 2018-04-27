package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.myrobotlab.arduino.ArduinoUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

public class MimicSpeech extends AbstractSpeechSynthesis implements TextListener {
  public final static Logger log = LoggerFactory.getLogger(MimicSpeech.class);
  private static final long serialVersionUID = 1L;
  private String currentVoice = "slt";
  // TODO: make this cross platform..
  private String mimicExecutable = "mimic\\mimic.exe";
  private HashSet<String> voices = new HashSet<String>();

  public MimicSpeech(String reservedKey) {
    super(reservedKey);
    // bootstrap the voices
    getVoices();
  }

  @Override
  public List<String> getVoices() {
    ArrayList<String> args = new ArrayList<String>();
    args.add("-lv");
    // args.add(getVoice());
    String res = ArduinoUtils.RunAndCatch(mimicExecutable, args);
    System.out.println("Stdout: " + res);
    for (String p : res.split(" ")) {
      voices.add(p);
    }
    ArrayList<String> vs = new ArrayList<String>(voices);
    return vs;
  }

  @Override
  public boolean setVoice(String voice) {
    if (voices.contains(voice)) {
      currentVoice = voice;
      return true;
    } else {
      return false;
    }
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


  @Override
  public AudioData speak(String toSpeak) {
    // TODO Auto-generated method stub
    // TODO: what's up with the audio data return value?!
    return null;
  }

  @Override
  public boolean speakBlocking(String toSpeak) {
    // TODO Auto-generated method stub
    invoke("publishStartSpeaking");
    // TODO: play the audio..
    ArrayList<String> args = new ArrayList<String>();
    args.add("-voice");
    args.add(getVoice());
    args.add(toSpeak);
    String res = ArduinoUtils.RunAndCatch(mimicExecutable, args);
    invoke("publishEndSpeaking");
    return false;
  }

  @Override
  public void setVolume(float volume) {
    // TODO Auto-generated method stub
  }

  @Override
  public float getVolume() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getVoice() {
    // TODO Auto-generated method stub
    return currentVoice;
  }


  public String getMimicExecutable() {
    return mimicExecutable;
  }

  public void setMimicExecutable(String mimicExecutable) {
    this.mimicExecutable = mimicExecutable;
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MimicSpeech.class.getCanonicalName());
    meta.addDescription("Speech synthesis based on Mimic from the MyCroft AI project.");
    meta.addCategory("speech", "sound");
    meta.addDependency("mycroftai.mimic", "mimic_win64", "1.0", "zip");
    // meta.addDependency("marytts", "5.2");
    // meta.addDependency("com.sun.speech.freetts", "1.2");
    // meta.addDependency("opennlp", "1.6");
    // TODO: build it for all platforms and add it to the repo as a zip file
    // so each os can download a pre-built version of mimic ...
    return meta;
  }

  public static void main(String[] args) throws Exception {
    MimicSpeech mimic = (MimicSpeech) Runtime.createAndStart("mimic", "MimicSpeech");
    List<String> voices = mimic.getVoices();
    for (int i = 0; i < voices.size(); ++i) {
      log.info("voice " + voices.get(i));
    }
    mimic.speakBlocking("hello hello, this is a test .. testing 1 2 3");
    mimic.speakBlocking("test me");
    mimic.speakBlocking("Hello world");
    mimic.speakBlocking("i am mimic");
    mimic.speakBlocking(
        "to be or not to be that is the question weather tis nobler in the mind to suffer the slings and arrows of outrageous fortune or to take arms against a sea of troubles");
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) {
    // TODO Auto-generated method stub
    return null;
  }

}
