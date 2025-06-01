package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.MarySpeechConfig;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class MarySpeechMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MarySpeechMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public MarySpeechMeta() {

    addCategory("speech", "sound");
    addDescription("Speech synthesis based on MaryTTS");

    addDependency("de.dfki.mary", "marytts", "5.2", "pom");
    // FIXME - use the following config file to generate the needed data for
    // loadVoice()
    // main config for voices
    // https://github.com/marytts/marytts-installer/blob/master/components.json

    String[] voices = new String[] { "voice-bits1-hsmm", "voice-bits3-hsmm", "voice-cmu-bdl-hsmm", "voice-cmu-nk-hsmm", "voice-cmu-rms-hsmm", "voice-cmu-slt-hsmm",
        "voice-dfki-obadiah-hsmm", "voice-dfki-ot-hsmm", "voice-dfki-pavoque-neutral-hsmm", "voice-dfki-poppy-hsmm", "voice-dfki-prudence-hsmm", "voice-dfki-spike-hsmm",
        "voice-enst-camille-hsmm", "voice-enst-dennys-hsmm", "voice-istc-lucia-hsmm", "voice-upmc-jessica-hsmm", "voice-upmc-pierre-hsmm" };

    for (String voice : voices) {
      addDependency("de.dfki.mary", voice, "5.2");
      // force using Runtimes httpclient version - exclude here
      exclude("org.apache.httpcomponents", "httpcore");
      exclude("org.apache.httpcomponents", "httpclient");

      if ("voice-bits1-hsmm".equals(voice) || "voice-cmu-slt-hsmm".equals(voice)) {
        exclude("org.slf4j", "slf4j-log4j12");
        exclude("log4j", "log4j");
      }
    }
    exclude("org.slf4j", "slf4j-api");
    exclude("commons-io", "commons-io");
    exclude("log4j", "log4j");
    exclude("commons-lang", "commons-lang");
    exclude("com.google.guava", "guava");
    exclude("org.apache.opennlp", "opennlp-tools");
    exclude("org.apache.opennlp", "opennlp-maxent");
    exclude("org.slf4j", "slf4j-log4j12");

    addDependency("org.apache.logging.log4j", "log4j-1.2-api", "2.12.1");
    addDependency("org.apache.logging.log4j", "log4j-api", "2.12.1");
   
  }


}
