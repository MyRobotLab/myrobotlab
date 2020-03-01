package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * 
 * WebkitSpeechSynthesis - webkit from chrome
 *
 * @author GroG
 *
 */
public class WebkitSpeechSynthesis extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesis.class);
  
  Map<Integer, String> indexToName = new HashMap<>();
  
  /**
   * the index of current voice in the browser
   */
  protected int voiceIndex = 0;

  public WebkitSpeechSynthesis(String n, String id) {
    super(n, id);
  }

  /**
   * webkit currently cannot generate audio data - would be cool to download the file if possible
   */
  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {
    // done in the web browser - we don't get audioData - it would be nice if we did 
    // perhaps it can be downloaded ....
    return null;
  }
  
  
  public void addWebKitVoice(Integer index, String name, String lang, Boolean def) {
    indexToName.put(index, name);
    addVoice(name, null, lang, null);
  }
  

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = AbstractSpeechSynthesis.getMetaData(WebkitSpeechSynthesis.class.getCanonicalName());

    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addCategory("speech", "sound");
    return meta;
  }
  
  public boolean setVoice(String name) {
    if (voices.containsKey(name)) {
      voice = voices.get(name);
      voiceIndex = 5;
      invoke("publishVoiceIndex", 5);
      broadcastState();
      return true;
    }

    error("could not set voice %s - valid voices are %s", name, String.join(", ", getVoiceNames()));
    return false;
  }



  @Override
  protected void loadVoices() throws Exception {
    // done in the webbrowser
    
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Platform.setVirtual(true);
      Runtime.main(new String[] { "--interactive", "--id", "inmoov" });

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      
      Runtime.start("speech", "WebkitSpeechSynthesis");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
