package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MimicSpeechMeta {
  public final static Logger log = LoggerFactory.getLogger(MimicSpeechMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.MimicSpeech");
    Platform platform = Platform.getLocalInstance();
    

    meta.addDescription("Speech synthesis based on Mimic from the MyCroft AI project.");
    meta.addCategory("speech", "sound");
    meta.addDependency("mycroftai.mimic", "mimic_win64", "1.0", "zip");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");

    meta.setSponsor("Kwatters");
    // meta.addDependency("marytts", "5.2");
    // meta.addDependency("com.sun.speech.freetts", "1.2");
    // meta.addDependency("opennlp", "1.6");
    // TODO: build it for all platforms and add it to the repo as a zip file
    // so each os can download a pre-built version of mimic ...
    return meta;
  }
  
}

