package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class MimicSpeechMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MimicSpeechMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public MimicSpeechMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("Speech synthesis based on Mimic from the MyCroft AI project.");
    addCategory("speech", "sound");
    addDependency("mycroftai.mimic", "mimic_win64", "1.0", "zip");
    addPeer("audioFile", "AudioFile", "audioFile");
    addCategory("speech", "sound");

    setSponsor("Kwatters");
    // addDependency("marytts", "5.2");
    // addDependency("com.sun.speech.freetts", "1.2");
    // addDependency("opennlp", "1.6");
    // TODO: build it for all platforms and add it to the repo as a zip file
    // so each os can download a pre-built version of mimic ...

  }

}
