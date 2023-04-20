package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.LocalSpeechConfig;
import org.myrobotlab.service.config.ServiceConfig;
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

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);

  private static final long serialVersionUID = 1L;

  protected String mimicPath = getResourceDir() + fs + "mimic" + fs + "mimic.exe";

  protected boolean removeExt = false;

  protected String ttsCommand = null;

  protected boolean ttsHack = false;

  protected Set<String> types = new HashSet<>(Arrays.asList("Espeak", "Festival", "Mimic", "MsSpeech", "Say", "Tts", "Pico2Wav"));

  protected String ttsPath = getResourceDir() + fs + "tts" + fs + "tts.exe";

  public LocalSpeech(String n, String id) {
    super(n, id);
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException, InterruptedException {

    LocalSpeechConfig c = (LocalSpeechConfig) config;

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
    if (c.replaceChars == null) {
      // if not user defined - escape double quotes to not affect templates
      c.replaceChars = new HashMap<>();
      c.replaceChars.put("\'", "\'\'");
    }
    for (String target : c.replaceChars.keySet()) {
      toSpeak = toSpeak.replace(target, c.replaceChars.get(target));
    }

    Platform platform = Runtime.getPlatform();
    if (ttsCommand == null) {
      log.error("tts command is invalid");
      return null;
    }
    String cmd = ttsCommand.replace("{text}", toSpeak);

    cmd = cmd.replace("{filename}", filename);

    if (getVoice() != null) {
      cmd = cmd.replace("{voice}", getVoice().getVoiceProvider().toString());
      cmd = cmd.replace("{voice_name}", getVoice().getName());
    }

    if (platform.isWindows()) {
      // Runtime.execute("cmd.exe", "/c", "\"" + cmd + "\"");
      List<String> args = new ArrayList<>();

      // https://thinkpowershell.com/create-cortana-audio-files-from-text-using-powershell/
      // https://mcpmag.com/articles/2018/03/07/talking-through-powershell.aspx

      // windows 10 minimum - power shell interface - output in json
      args.add("Add-Type -AssemblyName System.Speech;");
      args.add("$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;");
      args.add("$speak.SelectVoice('" + getVoice().getVoiceProvider().toString() + "');");
      args.add("$speak.SetOutputToWaveFile('" + localFileName + "');");
      args.add("$speak.speak('" + toSpeak + "')");
      String ret = Runtime.execute("powershell.exe", args, null, null, null);

      log.info("powershell returned : {}", ret);

    } else {
      Runtime.execute("bash", "-c", cmd);
    }

    File fileTest = new File(localFileName);
    if (fileTest.exists() && fileTest.length() > 0) {
      return new AudioData(localFileName);
    } else {
      if (platform.isLinux()) {
        error("0 byte file - please install a speech program: sudo apt-get install -y festival espeak speech-dispatcher gnustep-gui-runtime");
      } else {
        error("%s returned 0 byte file !!! - error with speech generation");
      }
      return null;
    }
  }

  /**
   * overridden because mac is silly for not being mp3 and ms tts is a mess
   * because it appends 0.mp3 :P
   */
  @Override
  public String getAudioCacheExtension() {
    return ".wav"; // hopefully Linux festival can do this (if not can we ?)
  }

  public Map<String, String> getFilter() {
    return ((LocalSpeechConfig) config).replaceChars;
  }

  @Override
  public Map<String, Locale> getLocales() {
    return Locale.getLocaleMap("en-US");
  }

  /**
   * @return get the tts command template
   */
  public String getTtsCommand() {
    return ttsCommand;
  }

  public String getTtsPath() {
    return ttsPath;
  }

  /**
   * one of the few methods a SpeechSynthesis service must implement if derived
   * from AbstractSpeechSynthesis -
   * 
   * Use protected addVoice(name, gender, lang, voiceProvider) to add voices
   * Voice.voiceProvider allows a serializable key to map MRL's Voice to a
   * implementation of a voice
   * 
   * FIXME create voices based on type, some types support different languages
   * .. some do not
   * 
   */
  @Override
  public void loadVoices() {

    if (voices.size() > 0) {
      log.info("already loaded voices");
      return;
    }

    Platform platform = Platform.getLocalInstance();

    String voicesText = null;

    if (platform.isWindows()) {

      try {

        List<String> args = new ArrayList<>();

        // windows 10 minimum - power shell interface - output in json
        args.add("Add-Type -AssemblyName System.Speech;");
        args.add("$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;");
        args.add("$speak.GetInstalledVoices() |");
        args.add("Select-Object  -Property * | ");
        // args.add("Select-Object -Property Culture, Name, Gender, Age");
        args.add("ConvertTo-Json ");
        voicesText = Runtime.execute("powershell.exe", args, null, null, null);

        // voicesText = Runtime.execute("cmd.exe", "/c", "\"\"" + ttsPath + "\""
        // + " -V" + "\"");

        log.info("voicesText {}", voicesText);

        int pos0 = voicesText.indexOf("[");
        int pos1 = voicesText.lastIndexOf("]");

        if (pos0 == -1 || pos1 == -1) {
          error("could not get voices - request returned: %s", voicesText);
        }

        String json = voicesText.substring(pos0, pos1 + 1);

        Object[] vo = CodecUtils.decodeArray(json);

        for (Object v : vo) {
          @SuppressWarnings("unchecked")
          Map<String, Object> m = (Map<String, Object>) v;
          @SuppressWarnings("unchecked")
          Map<String, Object> vi = (Map<String, Object>) m.get("VoiceInfo");
          String name = vi.get("Name").toString();
          String gender = vi.get("Gender").toString().equals("1.0") ? "male" : "female";
          String lang = vi.get("Culture").toString();
          addVoice(name, gender, lang, name);
        }

      } catch (Exception e) {
        error(e);
      }
    } else if (platform.isMac()) {
      // https://www.lifewire.com/mac-say-command-with-talking-terminal-2260772
      voicesText = Runtime.execute("bash", "-c", "say -v ?");

      // "say -v ?" outputs a list of available TTS voices under MacOS, oner per
      // line.
      // eg: "Agnes en_US # Isn't it nice to have a computer that will talk to
      // you?"

      Pattern pattern = Pattern.compile("^(\\w+)\\s+(\\w+)\\s+(.+)$");
      String lines[] = voicesText.split("\\r?\\n");

      for (int i = 0; i <= lines.length - 1; i++) {
        Matcher matcher = pattern.matcher(lines[i]);
        if (matcher.find()) {
          addVoice(matcher.group(1).toLowerCase(), "male", matcher.group(2), matcher.group(1).toLowerCase());
        }
      }
    } else if (platform.isLinux()) {
      addVoice("Linus", "male", "en-US", "festival");
    }
  }

  public void removeExt(boolean b) {
    removeExt = b;
  }

  /**
   * @return setEspeak sets the Linux tts to espeak template
   */
  public boolean setEspeak() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "Espeak";
    voices.clear();
    addVoice("espeak", "male", "en-US", "espeak");
    removeExt(false);
    setTtsHack(false);
    setTtsCommand("espeak \"{text}\" -w {filename}");
    return true;
  }

  /**
   * @return setFestival sets the Linux tts to festival template
   */
  public boolean setFestival() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    voices.clear();
    addVoice("Linus", "male", "en-US", "festival");
    c.speechType = "Festival";
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
   * setPico2Wav attempts to switch the sub template of Local Speech
   * 
   * @return true if successfully switched
   */
  public boolean setPico2Wav() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "Pico2Wav";
    removeExt(false);
    setTtsHack(false);

    voices.clear();
    addVoice("de-DE", "female", "de-DE", "pico2wav");
    addVoice("en-GB", "female", "en-GB", "pico2wav");
    addVoice("en-US", "female", "en-US", "pico2wav");
    addVoice("es-ES", "female", "es-ES", "pico2wav");
    addVoice("fr-FR", "female", "fr-FR", "pico2wav");
    addVoice("it-IT", "female", "it-IT", "pico2wav");

    setTtsCommand("pico2wave -l {voice_name} -w {filename} \"{text}\" ");
    if (!Runtime.getPlatform().isLinux()) {
      error("pico2wave only supported on Linux");
      return false;
    }
    broadcastState();
    return true;
  }

  /**
   * String of characters to filter out of text to create the tts command.
   * Typically double quotes should be filtered out of the command as creating
   * the text to speech process command can be broken by double quotes
   * 
   * @param filter
   *          chars to filter.
   * 
   */
  public void addFilter(String target, String replace) {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    if (c.replaceChars == null) {
      c.replaceChars = new HashMap<>();
    }
    c.replaceChars.put(target, replace);
  }

  /**
   * @return setMimic sets the Windows mimic template
   */
  public boolean setMimic() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "Mimic";
    removeExt(false);
    setTtsHack(false);
    if (Runtime.getPlatform().isWindows()) {
      setTtsCommand(mimicPath + " -voice " + getVoice() + " -o {filename} -t \"{text}\"");
    } else {
      setTtsCommand("mimic -voice " + getVoice() + " -o {filename} -t \"{text}\"");
    }
    return true;
  }

  @Deprecated /*
               * use appopriate named setSpeechType setter - use setSpeechType
               */
  public String setType(String type) {
    return setSpeechType(type);
  }

  public String setSpeechType(String speechType) {
    if (types.contains(speechType)) {
      // clever way to demux
      invoke("set" + speechType);
      return speechType;
    }
    error("%s is not a valid type, can be %s", speechType, types);
    return null;
  }

  public String getSpeechType() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    return c.speechType;
  }

  /**
   * Microsoft Speech Synthesis template
   * 
   * @return true if windows
   */
  public boolean setMsSpeech() {
    if (!Runtime.getPlatform().isWindows()) {
      error("microsoft speech is only supported on Windows");
      return false;
    }
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "MsSpeech";

    removeExt(false);
    setTtsHack(false);
    StringBuilder sb = new StringBuilder();

    sb.append("Add-Type -AssemblyName System.Speech;");
    sb.append("$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;");
    sb.append("$speak.SelectVoice(\"{{voice_name} }\");\n");
    sb.append("$speak.SetOutputToWaveFile(\"{filename}\");\n");
    sb.append("$speak.speak(\"{text}\")\n");

    return true;
  }

  /**
   * @return setSay sets the Mac say template
   */
  public boolean setSay() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "Say";
    removeExt(false);
    setTtsHack(false);
    setTtsCommand("/usr/bin/say -v {voice_name} --data-format=LEF32@22050 -o {filename} \"{text}\"");
    if (!Runtime.getPlatform().isMac()) {
      error("say only supported on Mac");
      return false;
    }
    return true;
  }

  /**
   * @return setFestival sets the Windows tts template
   * 
   */
  public boolean setTts() {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    c.speechType = "Tts";
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
   * @param ttsCommand
   *          set the tts command template
   * 
   */
  public void setTtsCommand(String ttsCommand) {
    LocalSpeechConfig c = (LocalSpeechConfig) config;
    info("LocalSpeech speechType %s template is now: %s", c.speechType, ttsCommand);
    this.ttsCommand = ttsCommand;
  }

  public void setTtsHack(boolean b) {
    ttsHack = b;
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

  @Override
  public ServiceConfig apply(ServiceConfig config) {
    LocalSpeechConfig c = (LocalSpeechConfig) super.apply(config);

    // setup the default tts per os
    Platform platform = Runtime.getPlatform();
    if (c.speechType == null) {
      if (platform.isWindows()) {
        setTts();
      } else if (platform.isMac()) {
        setSay();
      } else if (platform.isLinux()) {
        setFestival();
      } else {
        error("%s unknown platform %s", getName(), platform.getOS());
      }
    } else {
      setSpeechType(c.speechType);
    }

    if (c.voice != null) {
      setVoice(c.voice);
    }
    return c;
  }

  public static void main(String[] args) {
    try {

      // Runtime.main(new String[] { "--id", "admin"});
      LoggingFactory.init("INFO");

      // Runtime.startConfig("localspeech-01");

      LocalSpeech mouth = (LocalSpeech) Runtime.start("mouth", "LocalSpeech");
      // mouth.setSay();
      // mouth.speakBlocking("test 1 2 3");
      // mouth.speakBlocking("hello my name is sam, sam i am yet again, how
      // \"are you? do you 'live in a zoo too? ");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean done = true;
      if (done) {
        return;
      }

      // mouth.setMimic();

      String program = "Add-Type -AssemblyName System.Speech";
      // String[] program = new
      // String[]{"powershell.exe","$PSVersionTable.PSVersion"};

      List<String> arguments = new ArrayList<>();
      // arguments.add("$PSVersionTable.PSVersion");
      arguments.add("Add-Type -AssemblyName System.Speech;");
      arguments.add("$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;");
      arguments.add("$speak.speak('HELLO !!!!');");
      Runtime.execute("powershell.exe", arguments, null, null, null);
      // log.info(ret);

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
