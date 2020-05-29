package org.myrobotlab.service.meta;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

public class MarySpeechMeta {
  public final static Logger log = LoggerFactory.getLogger(MarySpeechMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.MarySpeech");

    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");
    meta.addDescription("Speech synthesis based on MaryTTS");

    meta.addDependency("de.dfki.mary", "marytts", "5.2", "pom");
    // FIXME - use the following config file to generate the needed data for
    // loadVoice()
    // main config for voices
    // https://github.com/marytts/marytts-installer/blob/master/components.json

  
    String[] voices = new String[] { "voice-bits1-hsmm", "voice-bits3-hsmm", "voice-cmu-bdl-hsmm", "voice-cmu-nk-hsmm", "voice-cmu-rms-hsmm", "voice-cmu-slt-hsmm",
        "voice-dfki-obadiah-hsmm", "voice-dfki-ot-hsmm", "voice-dfki-pavoque-neutral-hsmm", "voice-dfki-poppy-hsmm", "voice-dfki-prudence-hsmm", "voice-dfki-spike-hsmm",
        "voice-enst-camille-hsmm", "voice-enst-dennys-hsmm", "voice-istc-lucia-hsmm", "voice-upmc-jessica-hsmm", "voice-upmc-pierre-hsmm" };

    for (String voice : voices) {
      meta.addDependency("de.dfki.mary", voice, "5.2");
      meta.exclude("org.apache.httpcomponents", "httpcore");
      meta.exclude("org.apache.httpcomponents", "httpclient");

      if ("voice-bits1-hsmm".equals(voice) || "voice-cmu-slt-hsmm".equals(voice)) {
        meta.exclude("org.slf4j", "slf4j-log4j12");
      }
    }
    meta.exclude("org.slf4j", "slf4j-api");
    meta.exclude("commons-io", "commons-io");
    meta.exclude("log4j", "log4j");
    meta.exclude("commons-lang", "commons-lang");
    meta.exclude("com.google.guava", "guava");
    meta.exclude("org.apache.opennlp", "opennlp-tools");
    meta.exclude("org.slf4j", "slf4j-log4j12");

    return meta;
  }
  
}

