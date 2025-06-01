package org.myrobotlab.service.config;

public class SpeechRecognizerConfig extends ServiceConfig {

  /**
   * is actively listening and will publish recognized events
   */
  public boolean listening = false;
  
  /**
   * allows a wake word to be recognized and listening to begin
   */
  public boolean recording = false;
  
  // probably should be removed as listeners[] already has this info
  @Deprecated /* use ServiceConfig.listeners */
  public String[] textListeners;

  /**
   * number of seconds of silence after the initial wake word is used that it
   * the wake word will be needed to activate again null == unlimited
   */
  public Integer wakeWordIdleTimeoutSeconds = 10;

  /**
   * wait for this number of milliseconds after my speaking has ended
   */
  public long afterSpeakingPauseMs = 2000;
  
  /**
   * Wake word functionality is activated when it is set (ie not null) This
   * means recognizing events will be processed "after" it hears the wake word.
   * It will continue to publish events until a idle timeout period is reached.
   * It can continue to listen after this, but it will not publish. It fact, it
   * 'must' keep listening since in this idle state it needs to search for the
   * wake word
   */
  public String wakeWord = null;
  


}
