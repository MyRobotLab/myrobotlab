package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * Local OS speech service
 * 
 * windows & macos compatible
 *
 * @author moz4r
 *
 *         FIXME - use sapi/creatObject if necessary Say -
 *         https://www.lifewire.com/mac-say-command-with-talking-terminal-2260772
 * 
 *         Linux possibilities
 *         https://launchpad.net/ubuntu/precise/+source/svox/
 * 
 */
public class LocalSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);
  private String ttsPath = System.getProperty("user.dir") + File.separator + "tts" + File.separator + "tts.exe";

  public LocalSpeech(String n) {
    super(n);
  }

  static public ServiceType getMetaData() {

    ServiceType meta = AbstractSpeechSynthesis.getMetaData(LocalSpeech.class.getCanonicalName());
    meta.addCategory("speech", "sound");
    meta.addDescription("Local OS text to speech ( tts.exe / say etc ... )");
    meta.setAvailable(true);
    meta.addCategory("speech");
    meta.addDependency("com.microsoft", "tts", "1.1", "zip");
    return meta;
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException, InterruptedException {

    String localFileName = getLocalFileName(toSpeak);

    Platform platform = Runtime.getPlatform();
    String filename = getLocalFileName(toSpeak);
    if (filename == null) {
      return null;
    }
    if (platform.isWindows()) {
      // GAH ! .. tts.exe isn't like a Linux app where -o means output file to
      // "exact" name ...
      // unfortunately it appends .mp3 :P
      // so here we have to trim it off

      filename = filename.substring(0, filename.length() - 5);
      String cmd = "\"" + ttsPath + "\" -f 9 -v " + getVoice().getVoiceProvider().toString() + " -t -o " + "\"" + filename + "\" \"" + toSpeak + "\"";
      Runtime.execute("cmd.exe", "/c", "\"" + cmd + "\"");
    } else if (platform.isMac()) {
      // cmd = Runtime.execute(macOsTtsExecutable, toSpeak, "-o",
      // ttsExeOutputFilePath + uuid + "0.AIFF");
      String cmd = "say \"" + toSpeak + "\"" + "-o " + filename;
      Runtime.execute(cmd);
    } else if (platform.isLinux()) {
      // cmd = getOsTtsApp(); // FIXME IMPLEMENT !!!
      String furtherFiltered = toSpeak.replace("\"", "");// .replace("\'",
      // "").replace("|",
      // "");
      // Runtime.exec("bash", "-c", "echo \"" + furtherFiltered + "\" | festival
      // --tts");
      Process p = Runtime.exec("bash", "-c", "echo \"" + furtherFiltered + "\" | text2wave -o " + localFileName);
      // TODO : use (!p.waitFor(10, TimeUnit.SECONDS)) for security ?
      p.waitFor();
      // audioFile.play(audioData);
    }

    /*
     * String cmd = getTtsCmdLine(toSpeak);
     * 
     * 
     */
    File fileTest = new File(localFileName);
    if (fileTest.exists() && fileTest.length() > 0) {
      return new AudioData(localFileName);
    } else {
      error("%s returned 0 byte file !!! - it may block you", getName());
      return null;
    }
  }

  /**
   * overridden because mac is silly for not being mp3 & ms tts is a mess
   * because it appends 0.mp3 :P
   */
  public String getAudioCacheExtension() {
    if (Platform.getLocalInstance().isMac()) {
      return ".aiff";
    } else if (Platform.getLocalInstance().isWindows()) {
      return "0.mp3"; // ya stoopid no ?
    }
    return ".wav"; // hopefully Linux festival can do this (if not can we ?)
  }

  /**
   * one of the few methods a SpeechSynthesis service must implement if derived
   * from AbstractSpeechSynthesis -
   * 
   * Use protected addVoice(name, gender, lang, voiceProvider) to add voices
   * Voice.voiceProvider allows a serializable key to map MRL's Voice to a
   * implementation of a voice
   */
  @Override
  protected void loadVoices() {
    Platform platform = Platform.getLocalInstance();

    // voices returned from local app
    String voicesText = null;

    if (platform.isWindows()) {
      voicesText = Runtime.execute("cmd.exe", "/c", "\"\"" + ttsPath + "\"" + " -V" + "\"");
      log.info("cmd {}", voicesText);

      String[] lines = voicesText.split(System.getProperty("line.separator"));
      for (String line : lines) {
        // String[] parts = cmd.split(" ");
        String gender = "female"; // unknown
        String lang = "en-US"; // unknown

        if (line.startsWith("Exit")) {
          break;
        }
        // lame-ass parsing ..
        String voiceProvider = line.split(" ", 2)[0];
        // is line start with a number ? yes it's ( maybe ) a voice...
        try {
          int isItaVoice = Integer.parseInt(voiceProvider);
          // voice name cause issues because of spaces or (null), let's just use
          // original number as name...
          addVoice(voiceProvider, gender, lang, voiceProvider);
        } catch (Exception e) {
          break;
        }
      }
    } else if (platform.isMac()) {
      // https://www.lifewire.com/mac-say-command-with-talking-terminal-2260772
      voicesText = Runtime.execute("say -v");

      // FIXME - implement parse -v output
      addVoice("fred", "male", "en-US", "fred"); // in the interim added 1 voice
    } else if (platform.isLinux()) {
      addVoice("Linus", "male", "en-US", "festival");
    }
  }

  /**
   * override default tts.exe path
   * 
   * @param ttsPath
   *          - full path to windows tts.exe executable TODO - override also
   *          other os
   */
  public void setTtsPath(String ttsPath) {
    this.ttsPath = ttsPath;
  }

  public String getTtsPath() {
    return ttsPath;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    Runtime.start("gui", "SwingGui");

    LocalSpeech speech = (LocalSpeech) Runtime.start("speech", "LocalSpeech");
    // speech.parseEffects("#OINK##OINK# hey I thought #DOH# that was funny
    // #LAUGH01_F# very funny");
    // speech.getVoices();
    // speech.setVoice("1");
    /*
     * speech.speak(String.format("hello yes yes yes, my voice name is %s",
     * speech.getVoice().getName()));
     * speech.speakBlocking("I am your R 2 D 2 here me speak #R2D2#");
     * speech.speak("unicode éléphant");
     */

  }

}