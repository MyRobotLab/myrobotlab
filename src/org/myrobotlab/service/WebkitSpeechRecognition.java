package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.string.StringUtil;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the
 * chrome web browser this service requires the webgui to be running.
 *
 */
public class WebkitSpeechRecognition extends Service implements SpeechRecognizer, TextPublisher {

  /**
   * TODO: make it's own class. TODO: merge this data structure with the
   * programab oob stuff?
   *
   */
  public class Command {
    public String name;
    public String method;
    public Object[] params;

    Command(String name, String method, Object[] params) {
      this.name = name;
      this.method = method;
      this.params = params;
    }
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public String lastThingRecognized = "";
  private String language = "en-US";
  private boolean autoListen = false;

  HashMap<String, Command> commands = new HashMap<String, Command>();

  // track the state of the webgui, is it listening? maybe?
  public boolean listening = false;
  private boolean speaking = false;
  public boolean continuous = true;
  private long lastAutoListenEvent = System.currentTimeMillis();
  public boolean stripAccents = false;

  public WebkitSpeechRecognition(String reservedKey) {
    super(reservedKey);
  }

  @Override
  public String publishText(String text) {
    log.info("Publish Text : {}", text);
    // TODO: is there a better place to do this? maybe recognized?
    // TODO: remove this! it probably should be invoking the command on publish
    // text.. only on recognized?!
    // not sure.
    String cleantext = text.toLowerCase().trim();
    if (isStripAccents()) {
      cleantext = StringUtil.removeAccents(cleantext);
      log.info("Cleaned Text {}", cleantext);
    }
    /*
     * 
     * Double Speak FIX - I don't think a cmd should be sent from here because
     * it's not 'recognized' - recognized sends commands this method should be
     * subscribed too - GroG
     * 
     * if (commands.containsKey(cleantext)) { // If we have a command. send it
     * when we recognize... Command cmd = commands.get(cleantext);
     * send(cmd.name, cmd.method, cmd.params); }
     */

    return cleantext;
  }

  @Override
  public void listeningEvent() {
    // TODO Auto-generated method stub
    // temporary debug to show real mic status
    log.info("micIsListening");
    listening = true;
    broadcastState();
    return;
  }

  @Override
  public void pauseListening() {

    if (this.autoListen && !this.speaking) {
      // bug if there is multiple tabs

      if (System.currentTimeMillis() - lastAutoListenEvent > 50) {
        startListening();
      } else {
        error("WebkitSpeech : TOO MANY EVENTS, please close zombie tabs !");
        sleep(500);
      }
      lastAutoListenEvent = System.currentTimeMillis();
    } else {
      log.info("micNotListening");
      listening = false;
    }
    broadcastState();
  }

  @Override
  public String recognized(String text) {
    log.info("Recognized : >{}<", text);
    String cleanedText = text.toLowerCase().trim();
    if (isStripAccents()) {
      cleanedText = StringUtil.removeAccents(cleanedText);
    }
    if (commands.containsKey(cleanedText)) {
      // If we have a command. send it when we recognize...
      Command cmd = commands.get(cleanedText);
      send(cmd.name, cmd.method, cmd.params);
    }
    lastThingRecognized = cleanedText;
    broadcastState();
    return cleanedText;
  }

  @Override
  public void resumeListening() {
    log.info("Resume listening event seen.");
    this.listening = true;
    broadcastState();
  }

  @Override
  public void startListening() {
    log.info("Start listening event seen.");
    this.listening = true;
    broadcastState();
  }

  @Override
  public void stopListening() {
    log.info("Stop listening event seen.");
    this.listening = false;
    broadcastState();
  }

  public void setLanguage(String language) {
    // Here we want to set the language string and broadcast the update to the
    // web gui so that it knows to update the language on webkit speech
    this.language = language;
    broadcastState();
  }

  public void setAutoListen(boolean autoListen) {
    // Here we want to set the language string and broadcast the update to the
    // web gui so that it knows to update the language on webkit speech
    this.autoListen = autoListen;
    broadcastState();
  }

  public boolean getautoListen() {
    return this.autoListen;
  }

  public void setContinuous(boolean continuous) {
    // Here we want to set the language string and broadcast the update to the
    // web gui so that it knows to update the language on webkit speech
    this.continuous = continuous;
    broadcastState();
  }

  public boolean getContinuous() {
    return this.continuous;
  }

  public String getLanguage() {
    // a getter for it .. just in case.
    return this.language;
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  @Override
  public void addMouth(SpeechSynthesis mouth) {
    mouth.addEar(this);
    subscribe(mouth.getName(), "publishStartSpeaking");
    subscribe(mouth.getName(), "publishEndSpeaking");

    // TODO : we can implement the "did you say x?"
    // logic like sphinx if we want here.
    // when we add the ear, we need to listen for request confirmation

  }

  @Override
  public void onStartSpeaking(String utterance) {
    // at this point we should subscribe to this in the webgui
    // so we can pause listening.
    this.speaking = true;
    stopListening();
  }

  @Override
  public void onEndSpeaking(String utterance) {
    // need to subscribe to this in the webgui
    // so we can resume listening.
    this.speaking = false;
    startListening();
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      Runtime.start("webgui", "WebGui");
      WebkitSpeechRecognition w = (WebkitSpeechRecognition) Runtime.start("webkitspeechrecognition", "WebkitSpeechRecognition");
      w.setStripAccents(true);
    } catch (Exception e) {
      Logging.logError(e);
    }
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

    ServiceType meta = new ServiceType(WebkitSpeechRecognition.class.getCanonicalName());
    meta.addDescription("Speech recognition using Google Chrome webkit");
    meta.addCategory("speech recognition");
    // meta.addPeer("tracker", "Tracking", "test tracking");
    return meta;
  }

  @Override
  public void lockOutAllGrammarExcept(String lockPhrase) {
    log.warn("Lock out grammar not supported on webkit, yet...");
  }

  @Override
  public void clearLock() {
    log.warn("clear lock out grammar not supported on webkit, yet...");
  }

  // TODO - should this be in Service ?????
  public void addCommand(String actionPhrase, String name, String method, Object... params) {
    actionPhrase = actionPhrase.toLowerCase().trim();
    if (commands == null) {
      commands = new HashMap<String, Command>();
    }
    commands.put(actionPhrase, new Command(name, method, params));
  }

  // TODO: this might need to go into the interface if we want to support it.
  public void addComfirmations(String... txt) {
    log.warn("Confirmation support not enabled in webkit speech.");
  }

  // TODO: this might need to go into the interface if we want to support it.
  public void addNegations(String... txt) {
    log.warn("Negations not enabled in webkit speech.");
  }

  public void startListening(String grammar) {
    log.warn("Webkit speech doesn't listen for a specific grammar.  use startListening() instead. ");
    startListening();
  }

  public boolean isStripAccents() {
    return stripAccents;
  }

  public void setStripAccents(boolean stripAccents) {
    this.stripAccents = stripAccents;
  }

  @Override
  public void stopService() {
    super.stopService();
    autoListen = false;
    stopListening();
  }
}
