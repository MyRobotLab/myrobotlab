package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;

/**
 * Local OS speech service
 * 
 * Linux, Windows and OSx compatible
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
 *         ESPEAK - list voices : espeak --voices use voice : espeak -v en
 *         "Hello world, how are you doing today?" espeak -v en-sc "Hello world,
 *         how are you doing today?"
 * 
 *         using mbrola voice install voice : sudo apt-get install mbrola
 *         mbrola-us3 mbrola-en1 ... use voice : espeak -v mb-en1 "Hello world"
 *         espeak -v mb-en1 "Hello world" -w out.wav espeak -f speak.txt -v
 *         mb-en1 -w out.wav espeak -v mb-us1 "Hello world, how are you doing
 *         today?"
 * 
 *         MBROLA voices -
 *         https://github.com/espeak-ng/espeak-ng/blob/master/docs/mbrola.md#linux-installation
 * 
 */
public class LocalSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);
  protected String ttsPath = getResourceDir() + fs + "tts" + fs + "tts.exe";
  protected String mimicPath = getResourceDir() + fs + "mimic" + fs + "mimic.exe";
  protected String ttsCommand = null;
  protected String filterChars = "\"\'";
  protected boolean removeExt = false;
  protected boolean ttsHack = false;

  public LocalSpeech(String n, String id) {
    super(n, id);
    // setup the default tts per os
    Platform platform = Runtime.getPlatform();
    if (platform.isWindows()) {
      setTts();
    } else if (platform.isMac()) {
      setSay();
    } else if (platform.isLinux()) {
      setFestival();
    } else {
      error("%s unknown platform %s", getName(), platform.getOS());
    }
  }

  public void removeExt(boolean b) {
    removeExt = b;
  }

  public void setTtsHack(boolean b) {
    ttsHack = b;
  }

  /**
   * set the tts command template
   * 
   * @param ttsCommand
   */
  public void setTtsCommand(String ttsCommand) {
    info("LocalSpeech template is now: %s", ttsCommand);
    this.ttsCommand = ttsCommand;
  }

  /**
   * get the tts command template
   * 
   * @return
   */
  public String getTtsCommand() {
    return ttsCommand;
  }

  /**
   * setFestival sets the Windows tts template
   * 
   * @return
   */
  public boolean setTts() {
    removeExt(false);
    setTtsHack(true);
    setTtsCommand("\"" + ttsPath + "\" -f 9 -v {voice} -o {filename} -t \"{text}\"");
    if (!Runtime.getPlatform().isWindows()) {
      error("tts only supported on Windows");
      return false;
    }
    return true;
  }

  /**
   * setMimic sets the Windows mimic template
   * 
   * @return
   */
  public boolean setMimic() {
    removeExt(false);
    setTtsHack(false);
    setTtsCommand(mimicPath + " -voice " + getVoice() + " -o {filename} -t \"{text}\"");
    if (!Runtime.getPlatform().isWindows()) {
      error("mimic only supported on Windows");
      return false;
    }
    return true;
  }

  /**
   * setSay sets the Mac say template
   * 
   * @return
   */
  public boolean setSay() {
    removeExt(false);
    setTtsHack(false);
    setTtsCommand("say \"{text}\"" + "-o {filename}");
    if (!Runtime.getPlatform().isMac()) {
      error("say only supported on Mac");
      return false;
    }
    return true;
  }

  /**
   * setFestival sets the Linux tts to festival template
   * 
   * @return
   */
  public boolean setFestival() {
    removeExt(false);
    setTtsHack(false);
    setTtsCommand("echo \"{text}\" | text2wave -o {filename}");
    if (!Runtime.getPlatform().isLinux()) {
      error("festival only supported on Linux");
      return false;
    }
    return true;
  }

  /**
   * setEspeak sets the Linux tts to espeak template
   * 
   * @return
   */
  public boolean setEspeak() {
    removeExt(false);
    setTtsHack(false);
    setTtsCommand("espeak \"{text}\" -w {filename}");
    if (!Runtime.getPlatform().isLinux()) {
      error("festival only supported on Linux");
      return false;
    }
    return true;
  }

  /**
   * String of characters to filter out of text to create the tts command.
   * Typically double quotes should be filtered out of the command as creating
   * the text to speech process command can be broken by double quotes
   * 
   * @param filter
   */
  public void setFilter(String filter) {
    filterChars = filter;
  }

  public String getFilter() {
    return filterChars;
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException, InterruptedException {

    // the actual filename on the file system
    String localFileName = getLocalFileName(toSpeak);

    // the cmd filename - in some cases cmd templates don't want the extension
    String filename = localFileName;
    if (removeExt) {
      // some cmd line require the filename without ext be supplied
      filename = localFileName.substring(0, localFileName.lastIndexOf("."));
    }

    if (ttsHack) {
      // lame tts.exe on windows appends "0.mp3" to whatever filename was
      // supplied wtf?
      filename = filename.substring(0, filename.length() - 5);
    }

    // filter out breaking chars
    if (filterChars != null) {
      for (int i = 0; i < filterChars.length(); ++i) {
        toSpeak = toSpeak.replace(filterChars.charAt(i), ' ');
      }
    }

    Platform platform = Runtime.getPlatform();
    String cmd = ttsCommand.replace("{text}", toSpeak);

    cmd = cmd.replace("{filename}", filename);

    if (getVoice() != null) {
      cmd = cmd.replace("{voice}", getVoice().getVoiceProvider().toString());
    }

    if (platform.isWindows()) {
      Runtime.execute("cmd.exe", "/c", "\"" + cmd + "\"");
    } else if (platform.isMac()) {
      Runtime.execute(cmd);
    } else if (platform.isLinux()) {
      Runtime.execute("bash", "-c", cmd);
    }

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
    } else if (ttsHack) {
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
        if (parts.length < 2) { // some voices are not based on a standard
                                // pattern
          continue;
        }
        // lame-ass parsing ..
        // standard sapi pattern is 5 parameters :
        // INDEX PROVIDER VOICE_NAME PLATEFORM - LANG
        // we need INDEX, VOICE_NAME, LANG
        // but .. some voices dont use it, we will try to detect pattern and
        // adapt if no respect about it :

        // INDEX :
        String voiceProvider = parts[0];

        // VOICE_NAME
        String voiceName = "Unknown" + voiceProvider; // default name if there
                                                      // is an issue
        // it is standard, cool
        if (parts.length >= 6) {
          voiceName = parts[2];// line.trim();
        }
        // almost standard, we have INDEX PROVIDER VOICE_NAME
        else if (parts.length > 2) {
          voiceName = line.split(" ")[2];
        }
        // non standard at all ... but we catch it !
        else {
          voiceName = line.split(" ")[1];
        }

        // LANG ( we just detect for a keyword inside the whole string, because
        // position is random sometime )
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

  public static void main(String[] args) {
    try {

      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
      LoggingFactory.init("WARN");

      // Runtime.start("gui", "SwingGui");

      LocalSpeech mouth = (LocalSpeech) Runtime.start("mouth", "LocalSpeech");

      boolean done = true;
      if (done) {
        return;
      }
      
      mouth.speakBlocking("hello my name is sam, sam i am yet again, how \"are you? do you 'live in a zoo too? ");
      mouth.setMimic();
      mouth.speakBlocking("bork bork bork, hello my name is sam, sam i am yet again, how \"are you? do you 'live in a zoo too? ");
      // speech.setTtsCommand("espeak \"{text}\" -w {filename}");
      mouth.setEspeak();
      log.info("tts command template is {}", mouth.getTtsCommand());
      mouth.speakBlocking("i can speak some more");
      mouth.speakBlocking("my name is bob");
      mouth.speakBlocking("i have a job");
      mouth.speakBlocking("and i can dance in a mob");
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
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

}