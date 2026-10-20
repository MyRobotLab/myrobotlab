package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VoskSpeechRecognitionMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VoskSpeechRecognitionMeta.class);

  public VoskSpeechRecognitionMeta() {
    addDescription("Offline speech recognition using Vosk (no internet required after model download)");
    addCategory("speech recognition", "speech", "sound");
    setAvailable(true);
    setLicenseApache();

    // Java bindings + platform natives via JNA (bundled in the vosk jar)
    addDependency("com.alphacephei", "vosk", "0.3.45");
  }

}
