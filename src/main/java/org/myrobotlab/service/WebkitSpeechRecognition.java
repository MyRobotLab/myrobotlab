package org.myrobotlab.service;

import java.util.HashMap;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.string.StringUtil;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the chrome web browser this service requires the webgui
 * to be running.
 *
 */
public class WebkitSpeechRecognition extends AbstractSpeechRecognizer {
  public HashMap<String, String> languagesList = new HashMap<String, String>();
  // used for angular
  public String currentWebkitLanguage;

  /**
   * TODO: make it's own class. TODO: merge this data structure with the programab oob stuff?
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

  boolean autoListen;
  boolean continuous;

  HashMap<String, Command> commands = new HashMap<String, Command>();

  private boolean listening = false;
  private boolean speaking = false;

  private long lastAutoListenEvent = System.currentTimeMillis();
  public boolean stripAccents = false;
  private boolean lockOutAllGrammar = false;
  private String lockPhrase = "";

  public WebkitSpeechRecognition(String reservedKey) {
    super(reservedKey);
    languagesList.put("en-US", "English - United States");
    languagesList.put("en-GB", "English - British");
    languagesList.put("af-ZA", "Afrikaans");
    languagesList.put("id-ID", "Bahasa Indonesia");
    languagesList.put("ms-MY", "Bahasa Melayu");
    languagesList.put("ca-ES", "Català");
    languagesList.put("cs-CZ", "Čeština");
    languagesList.put("da-DK", "Dansk");
    languagesList.put("de-DE", "Deutsch");
    languagesList.put("en-AU", "English - Australia");
    languagesList.put("en-CA", "English - Canada");
    languagesList.put("en-IN", "English - India");
    languagesList.put("en-NZ", "English - New Zealand");
    languagesList.put("en-ZA", "English - South Africa");
    languagesList.put("en-GB", "English - United Kingdom");
    languagesList.put("en-US", "English - United States");
    languagesList.put("es-AR", "Español - Argentina");
    languagesList.put("es-BO", "Español - Bolivia");
    languagesList.put("es-CL", "Español - Chile");
    languagesList.put("es-CO", "Español - Colombia");
    languagesList.put("es-CR", "Español - Costa Rica");
    languagesList.put("es-EC", "Español - Ecuador");
    languagesList.put("es-SV", "Español - El Salvador");
    languagesList.put("es-ES", "Español - España");
    languagesList.put("es-US", "Español - Estados Unidos");
    languagesList.put("es-GT", "Español - Guatemala");
    languagesList.put("es-HN", "Español - Honduras");
    languagesList.put("es-MX", "Español - México");
    languagesList.put("es-NI", "Español - Nicaragua");
    languagesList.put("es-PA", "Español - Panamá");
    languagesList.put("es-PY", "Español - Paraguay");
    languagesList.put("es-PE", "Español - Perú");
    languagesList.put("es-PR", "Español - Puerto Rico");
    languagesList.put("es-DO", "Español - República Dominicana");
    languagesList.put("es-UY", "Español - Uruguay");
    languagesList.put("es-VE", "Español - Venezuela");
    languagesList.put("eu-ES", "Euskara");
    languagesList.put("fil-PH", "Filipino");
    languagesList.put("fr-FR", "Français");
    languagesList.put("gl-ES", "Galego");
    languagesList.put("hi-IN", "Hindi - हिंदी");
    languagesList.put("hr_HR", "Hrvatski");
    languagesList.put("zu-ZA", "IsiZulu");
    languagesList.put("is-IS", "Íslenska");
    languagesList.put("it-IT", "Italiano - Italia");
    languagesList.put("it-CH", "Italiano - Svizzera");
    languagesList.put("lt-LT", "Lietuvių");
    languagesList.put("hu-HU", "Magyar");
    languagesList.put("nl-NL", "Nederlands");
    languagesList.put("nb-NO", "Norsk bokmål");
    languagesList.put("pl-PL", "Polski");
    languagesList.put("pt-BR", "Português - Brasil");
    languagesList.put("pt-PT", "Português - Portugal");
    languagesList.put("ro-RO", "Română");
    languagesList.put("sl-SI", "Slovenščina");
    languagesList.put("sk-SK", "Slovenčina");
    languagesList.put("fi-FI", "Suomi");
    languagesList.put("sv-SE", "Svenska");
    languagesList.put("vi-VN", "Tiếng Việt");
    languagesList.put("tr-TR", "Türkçe");
    languagesList.put("el-GR", "Ελληνικά");
    languagesList.put("bg-BG", "български");
    languagesList.put("ru-RU", "Pусский");
    languagesList.put("sr-RS", "Српски");
    languagesList.put("uk-UA", "Українська");
    languagesList.put("ko-KR", "한국어");
    languagesList.put("cmn-Hans-CN", "中文 - 普通话 (中国大陆)");
    languagesList.put("cmn-Hans-HK", "中文 - 普通话 (香港)");
    languagesList.put("cmn-Hant-TW", "中文 - 中文 (台灣)");
    languagesList.put("yue-Hant-HK", "中文 - 粵語 (香港)");
    languagesList.put("ja-JP", "日本語");
    languagesList.put("th-TH", "ภาษาไทย");

    currentWebkitLanguage = Runtime.getInstance().getLanguage();
    if (!languagesList.containsKey(currentWebkitLanguage)) {
      currentWebkitLanguage = "en-US";
    }
  }

  @Override
  public String publishText(String text) {
    return recognized(text);
  }

  @Override
  public String recognized(String text) {
    log.info("Recognized : >{}< ", text);
    // invoke("publishText", text); - we don't even need to do this
    // because the WebkitSpeechRecognition.js calls publishText

    String cleanedText = text.toLowerCase().trim();
    if (isStripAccents()) {
      cleanedText = StringUtil.removeAccents(cleanedText);
    }
    if (text.equalsIgnoreCase(lockPhrase)) {
      clearLock();
    }
    lastThingRecognized = cleanedText;
    broadcastState();
    if (!lockOutAllGrammar) {
      if (commands.containsKey(cleanedText)) {
        // If we have a command. send it when we recognize...
        Command cmd = commands.get(cleanedText);
        send(cmd.name, cmd.method, cmd.params);
      }
      return cleanedText;
    } else {
      log.info("Speech recognizer is locked by keyword : {}", lockPhrase);
    }
    return "";
  }

  @Override
  public void listeningEvent(Boolean event) {
    listening = event;
    broadcastState();
    return;
  }

  @Override
  public void pauseListening() {
    stopListening();
  }

  @Override
  public void resumeListening() {
    startListening();
  }

  @Override
  public void startListening() {
    log.debug("Start listening event seen.");
    this.listening = true;
    broadcastState();
  }

  @Override
  public void stopListening() {
    log.debug("Stop listening event seen.");
    boolean commError = false;
    if (this.autoListen && !this.speaking) {

      if (System.currentTimeMillis() - lastAutoListenEvent > 300) {
        startListening();
      } else {
        // loop if there is multiple chrome tabs OR no internet...
        if (listening) {
          error("Please close zombie tabs and check Internet connection");
          commError = true;
        }
      }
      lastAutoListenEvent = System.currentTimeMillis();
    } else {
      log.debug("micNotListening");
      listening = false;
    }
    if (!commError) {
      broadcastState();
    }
  }

  @Override
  public void setAutoListen(boolean autoListen) {
    this.autoListen = autoListen;
    broadcastState();
  }

  public boolean getautoListen() {
    return this.autoListen;
  }

  /**
   * If setContinuous is False, this speedup recognition processing If setContinuous is True, you have some time to speak again, in
   * case of error
   */
  public void setContinuous(boolean continuous) {
    this.continuous = continuous;
    broadcastState();
  }

  public boolean getContinuous() {
    return this.continuous;
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

    Runtime.start("gui", "SwingGui");
    WebkitSpeechRecognition webkitspeechrecognition = (WebkitSpeechRecognition) Runtime.start("webkitspeechrecognition", "WebkitSpeechRecognition");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/service/webkitspeechrecognition");
    webkitspeechrecognition.setAutoListen(true);
  }

  /**
   * This static method returns all the details of the class without it having to be constructed. It has description, categories,
   * dependencies, and peer definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WebkitSpeechRecognition.class.getCanonicalName());
    meta.addDescription("Speech recognition using Google Chrome webkit");
    meta.addCategory("speech recognition");
    return meta;
  }

  @Override
  public void lockOutAllGrammarExcept(String lockPhrase) {
    log.info("Ear locked now, please use command " + lockPhrase + " to unlock");
    lockOutAllGrammar = true;
    this.lockPhrase = lockPhrase;
  }

  @Override
  public void clearLock() {
    log.warn("Ear unlocked by " + lockPhrase);
    lockOutAllGrammar = false;
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

  /**
   * track the state of listening process
   */
  @Override
  public boolean isListening() {
    return this.listening;
  }

  public void setStripAccents(boolean stripAccents) {
    this.stripAccents = stripAccents;
  }

  public void setcurrentWebkitLanguage(String l) {
    currentWebkitLanguage = l;
    broadcastState();

  }

  @Override
  public void stopService() {
    super.stopService();
    autoListen = false;
    stopListening();
  }

}