package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;

/**
 * Local OS speech service
 * 
 * windows and macos compatible
 *
 * @author moz4r
 *
 *         FIXME - use sapi/creatObject if necessary Say -
 *         https://www.lifewire.com/mac-say-command-with-talking-terminal-2260772
 * 
 *         Linux possibilities
 *         https://launchpad.net/ubuntu/precise/+source/svox/
 *         
 *         More Voices can be found for Windows at
 *         https://www.microsoft.com/en-us/download/details.aspx?id=3971
 *         
 *         ESPEAK - 
 *            list voices :
 *                espeak --voices  
 *            use voice :
 *                espeak  -v en "Hello world, how are you doing today?"
 *                espeak  -v en-sc "Hello world, how are you doing today?"
 *                
 *            using mbrola voice
 *            install voice :
 *                sudo apt-get install mbrola mbrola-us3 mbrola-en1 ...
 *            use voice :
 *                espeak -v mb-en1 "Hello world"
 *                espeak -v mb-en1 "Hello world" -w out.wav
 *                espeak -f speak.txt -v mb-en1 -w out.wav
 *                espeak  -v mb-us1 "Hello world, how are you doing today?"
 *                
 *                
 *         MBROLA voices - https://github.com/espeak-ng/espeak-ng/blob/master/docs/mbrola.md#linux-installation
 * 
 */
public class LocalSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);
  private String ttsPath = System.getProperty("user.dir") + File.separator + "tts" + File.separator + "tts.exe";

  public LocalSpeech(String n, String id) {
    super(n, id);
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
      // ProcessBuilder pb = new ProcessBuilder()
      // cmd = getOsTtsApp(); // FIXME IMPLEMENT !!!
      String furtherFiltered = toSpeak.replace("\"", "");// .replace("\'",
      // "").replace("|",
      // "");
      // Runtime.exec("bash", "-c", "echo \"" + furtherFiltered + "\" | festival
      // --tts");
      
      // apt install espeak 
      // sudo apt-get install mbrola mbrola-en1
      // espeak -f speak.txt -w out.wav 
      // espeak -ven-sc -f speak.txt -w out.wav
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
      if (platform.isLinux()) {
        error("0 byte file - is festival installed?  apt install festival");
      } else {
        error("%s returned 0 byte file !!! - it may block you", getName());
      }
      return null;
    }
  }

  /**
   * overridden because mac is silly for not being mp3 and ms tts is a mess
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

    if (voices.size() > 0) {
      log.info("already loaded voices");
      return;
    }

    Platform platform = Platform.getLocalInstance();

    // voices returned from local app
    String voicesText = null;

    if (platform.isWindows()) {
      voicesText = Runtime.execute("cmd.exe", "/c", "\"\"" + ttsPath + "\"" + " -V" + "\"");

      log.info("cmd {}", voicesText);

      String[] lines = voicesText.split(System.getProperty("line.separator"));
      for (String line : lines) {
        // String[] parts = cmd.split(" ");
        // String gender = "female"; // unknown
        String lang = "en-US"; // unknown

        if (line.startsWith("Exit")) {
          break;
        }
        String[] parts = line.split(" ");
        if (parts.length < 2) { // some voices are not based on a standard pattern
          continue;
        }
        // lame-ass parsing ..
        // standard sapi pattern is 5 parameters :
        // INDEX PROVIDER VOICE_NAME PLATEFORM - LANG
        // we need INDEX, VOICE_NAME, LANG
        // but .. some voices dont use it, we will try to detect pattern and adapt if no respect about it :

        // INDEX :
        String voiceProvider = parts[0];

        // VOICE_NAME
        String voiceName = "Unknown" + voiceProvider; //default name if there is an issue
        // it is standard, cool
        if (parts.length >= 6) {
          voiceName = parts[2];//line.trim();
        }
        // almost standard, we have INDEX PROVIDER VOICE_NAME
        else if (parts.length > 2) {
          voiceName = line.split(" ")[2];
        }
        // non standard at all ... but we catch it !
        else {
          voiceName = line.split(" ")[1];
        }

        // LANG ( we just detect for a keyword inside the whole string, because position is random sometime )
        // TODO: locale converter from keyword somewhere ?

        if (line.toLowerCase().contains("french") || line.toLowerCase().contains("français")) {
          lang = "fr-FR";
        }

        try {
          // verify integer
          Integer.parseInt(voiceProvider);
          // voice name cause issues because of spaces or (null), let's just use
          // original number as name...
          addVoice(voiceName, null, lang, voiceProvider);
        } catch (Exception e) {
          continue;
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
  

  @Override
  public Map<String, Locale> getLocales() {
    return Locale.getLocaleMap("en-US");
  }


  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    Runtime.start("gui", "SwingGui");

    LocalSpeech speech = (LocalSpeech) Runtime.start("speech", "LocalSpeech");
    speech.speakBlocking("hello my name is sam, sam i am");
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