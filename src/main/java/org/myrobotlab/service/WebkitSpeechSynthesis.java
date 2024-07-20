package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.SpeechSynthesisConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * 
 * WebkitSpeechSynthesis -
 * https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis
 *
 * @author GroG
 *
 */
public class WebkitSpeechSynthesis extends AbstractSpeechSynthesis<SpeechSynthesisConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesis.class);

  Map<String, Integer> nameToIndex = new HashMap<>();

  /**
   * the index of current voice in the browser
   */
  protected int voiceIndex = 0;

  public WebkitSpeechSynthesis(String n, String id) {
    super(n, id);

    /**
     * speechSynthesis.speak() without user activation is no longer allowed
     * since M71, around December 2018. See
     * https://www.chromestatus.com/feature/5687444770914304 for more details
     * speechSynthesisMessage
     * 
     * We start this service as mute until the user presses the unmute button
     */

    // setMute(true);
  }

  /**
   * webkit currently cannot generate audio data - would be cool to download the
   * file if possible
   */
  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {
    // done in the web browser - we don't get audioData - it would be nice if we
    // did
    // perhaps it can be downloaded ....

    // send message to browser to speak
    invoke("webkitSpeak", toSpeak);
    return null;
  }

  public String webkitSpeak(String text) {
    return text;
  }

  /**
   * This method is called by the browser, and it populates the list of voices.
   * 
   * @param index
   *          i
   * @param name
   *          n
   * @param lang
   *          l
   * @param def
   *          d
   * 
   */
  public void addWebKitVoice(Integer index, String name, String lang, Boolean def) {
    nameToIndex.put(name, index);
    addVoice(name, null, lang, null);
  }

  @Override
  public boolean setVoice(String name) {
    if (voices.containsKey(name)) {
      voice = voices.get(name);
      voiceIndex = nameToIndex.get(name);
      // invoke("publishVoiceIndex", voiceIndex);
      broadcastState();
      return true;
    }

    error("could not set voice %s - valid voices are %s", name, String.join(", ", getVoiceNames()));
    return false;
  }

  public Integer publishVoiceIndex(Integer voiceIndex) {
    return voiceIndex;
  }

  /**
   * https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis/getVoices
   */
  @Override
  public void loadVoices() throws Exception {
    // done in the webbrowser - this method is a NOOP

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
//      Platform.setVirtual(true);
//      Runtime.main(new String[] { "--interactive", "--id", "inmoov" });

      Runtime.start("python", "Python");
      
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      WebkitSpeechSynthesis webkit = (WebkitSpeechSynthesis) Runtime.start("webkit", "WebkitSpeechSynthesis");
      
      
      boolean done = true;
      if (done) {
        return;
      }
      

      for (int i = 0; i < 1000; ++i) {
        webkit.setVoice("Google UK English Female");
        webkit.speak("how now brown cow");
        webkit.setVoice("Google UK English Male");
        webkit.speak("how now brown cow");
        webkit.setVoice("Google français");
        webkit.speak("Ah, la vache! Chercher la petite bête");
        webkit.setVoice("Google Deutsch");
        webkit.speak("Da liegt der Hund begraben.");
        webkit.setVoice("Google Nederlands");
        webkit.speak("Nu komt de aap uit de mouw");
        webkit.setVoice("Google Nederlands");
        webkit.speak("Nu komt de aap uit de mouw");
        webkit.setVoice("Google italiano");
        webkit.speak("Ubriaco come una scimmia");

      }


    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
