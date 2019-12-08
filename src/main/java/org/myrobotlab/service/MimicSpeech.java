package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * Look at - https://mycroft.ai/documentation/mimic/#mimic
 *
 */
public class MimicSpeech extends AbstractSpeechSynthesis {
  public final static Logger log = LoggerFactory.getLogger(MimicSpeech.class);
  private static final long serialVersionUID = 1L;

  // FIXME: make this cross platform..
  private String mimicFolder = "mimic";
  private String mimicExecutable = mimicFolder + File.separator + "mimic.exe";
  public String mimicOutputFilePath = System.getProperty("user.dir") + File.separator + mimicFolder + File.separator;

  public MimicSpeech(String n, String id) {
    super(n, id);
  }

  static public ServiceType getMetaData() {

    // ServiceType meta = new ServiceType(MimicSpeech.class.getCanonicalName());
    ServiceType meta = AbstractSpeechSynthesis.getMetaData(MimicSpeech.class.getCanonicalName());

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

  public static void main(String[] args) throws Exception {
    Runtime.start("gui", "SwingGui");
    MimicSpeech mimic = (MimicSpeech) Runtime.createAndStart("mimic", "MimicSpeech");
    LoggingFactory.init(Level.INFO);

    mimic.speakBlocking("hello \"world\", it's a  test .. testing 1 2 3 , unicode éléphant");
    mimic.speakBlocking("#THROAT01_F# hi! it works.");
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {
    toSpeak = toSpeak.replace("\"", "\"\"");
    String fileName = getLocalFileName(toSpeak);

    // String fileName = mimicOutputFilePath + UUID.randomUUID().toString() +
    // "." + getAudioCacheExtension();
    String command = System.getProperty("user.dir") + File.separator + mimicExecutable + " -voice " + getVoice() + " -o \"" + fileName + "\" -t \"" + toSpeak + "\"";
    String cmd = "null";

    // FIXME - there are other executables on other OSs - get them and
    // bundle it all together
    if (Platform.getLocalInstance().isWindows()) {

      cmd = Runtime.execute("cmd.exe", "/c", command);

      log.info(cmd);
      // byte[] b = FileIO.toByteArray(f);
      // FileIO.toFile(fileName, b);
      return new AudioData(fileName);

    }
    error("os not supported - currently only windows");
    return null;
  }

  @Override
  protected void loadVoices() {
    // mimic -lv
    // Voices available: kal awb_time kal16 awb rms slt ap

    // mimic -t "Hello" -voice slt
    addVoice("Henry", "male", "en", "slt"); // Japanese
  }

}
