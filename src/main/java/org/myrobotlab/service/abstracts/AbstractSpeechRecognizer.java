package org.myrobotlab.service.abstracts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;

public abstract class AbstractSpeechRecognizer extends Service implements SpeechRecognizer {

  /**
   * text and confidence (and any additional meta data) to be published
   */
  public static class ListeningEvent {
    public long ts;
    public Double confidence;
    public Boolean isSpeaking;
    public Boolean isListening;
    public Boolean isRecording;
    public Boolean isFinal;
    public String text;
    public Boolean isAwake; // assume awake

    /**
     * determines if the listening event will trigger a publishText - default is
     * false because there are a variety of meta-messages that would not be
     * suitable to publish
     */
    public Boolean publishText = false;

    public ListeningEvent() {
      ts = System.currentTimeMillis();
    }
  }

  /**
   * generalized list of languages and their codes - if useful
   */
  protected Map<String, Locale> locales = new HashMap<>();

  private static final long serialVersionUID = 1L;

  /**
   * all currently attached services
   */
  protected Set<String> attached = new TreeSet<>();

  protected HashMap<String, Message> commands = new HashMap<>();

  /**
   * status of listening
   */
  protected boolean isListening = false;

  /**
   * status when wake word is used and is ready to publish recognized events
   */
  protected boolean isAwake = true;

  /**
   * status of publishing recognized text
   */
  protected boolean isRecording = false;

  @Deprecated /* remove ! - is from webkit - should be handled in js */
  protected long lastAutoListenEvent = System.currentTimeMillis();

  protected String lastThingRecognized = null;

  /**
   * prevents execution of command unless a lock out phrase is recognized
   */
  @Deprecated /* use wake word */
  protected boolean lockOutAllGrammar = false;

  /**
   * the current locale this service is set to e.g. en-US it-IT fr etc...
   */
  protected Locale locale;

  /**
   * phrase to unlock commands such as "power up"
   */
  @Deprecated /* use wake word */
  protected String lockPhrase = "";

  protected boolean lowerCase = true;

  /**
   * Used in conjunction with a SpeechSythesis
   */
  // FIXME - probably not needed - what needs to happen is SpeechSynthesis
  // should "only" affect recognizing NOT listening
  // and an end user affect listening ...
  // protected boolean speaking = false;

  /**
   * Wake word functionality is activated when it is set (ie not null) This
   * means recognizing events will be processed "after" it hears the wake word.
   * It will continue to publish events until a idle timeout period is reached.
   * It can continue to listen after this, but it will not publish. It fact, it
   * 'must' keep listening since in this idle state it needs to search for the
   * wake word
   */
  protected String wakeWord = null;

  /**
   * number of seconds of silence after the initial wake word is used that it
   * the wake word will be needed to activate again null == unlimited
   */
  protected Integer wakeWordIdleTimeoutSeconds = 10;

  protected Long lastWakeWordTs = null;

  protected boolean isSpeaking = false;

  /**
   * wait for 1 sec after my speaking has ended
   */
  protected long afterSpeakingPauseMs = 2000;

  protected boolean normalize = true;

  protected boolean removeWakeWord = true;

  public AbstractSpeechRecognizer(String n, String id) {
    super(n, id);
    locales = getLocales();
    locale = Runtime.getInstance().getLocale();
    if (!locales.containsKey(locale.toString())) {
      locale = new Locale("en-US");
    }
  }

  public void addCommand(String actionPhrase, String name, String method, Object... params) {
    actionPhrase = actionPhrase.toLowerCase().trim();
    Message msg = Message.createMessage(getName(), name, method, params);
    commands.put(actionPhrase, msg);
  }

  @Override
  @Deprecated /* use attachSpeechSynthesis(SpeechSynthesis mouth) */
  public void addMouth(SpeechSynthesis mouth) {
    attachSpeechSynthesis(mouth);
  }

  @Override
  public void addTextListener(TextListener service) {
    attachTextListener(service);
  }

  /**
   * routable attach handles attaching based on type info
   */
  public void attach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      attachSpeechSynthesis((SpeechSynthesis) attachable);
    } else {
      error("do not know how to attach %s", attachable.getName());
    }
  }

  /**
   * subscribe to speech synthesis events so we wont be "re"-publishing
   * recognitions we spoke and fall into the infinite loop of internal dialog
   * talkig to ourselves ...
   * 
   * @param mouth
   */
  public void attachSpeechSynthesis(SpeechSynthesis mouth) {
    if (mouth == null) {
      log.warn("{}.attachSpeechSynthesis(null)", getName());
      return;
    }

    if (isAttached(mouth.getName())) {
      log.info("{} already attached", mouth.getName());
    }
    subscribe(mouth.getName(), "publishStartSpeaking");
    subscribe(mouth.getName(), "publishEndSpeaking");

    // mouth.attachSpeechRecognizer(ear);
    attached.add(mouth.getName());
  }

  public void attachTextListener(TextListener service) {
    if (service == null) {
      log.warn("{}.attachTextListener(null)");
      return;
    }
    addListener("publishText", service.getName());
  }

  @Override
  @Deprecated /* use wake word */
  public void clearLock() {
    log.warn("Ear unlocked by " + lockPhrase);
    lockOutAllGrammar = false;
  }

  /**
   * Get the current wake word
   * 
   * @return
   */
  public String getWakeWord() {
    return wakeWord;
  }

  public boolean isAttached(Attachable attachable) {
    return isAttached(attachable.getName());
  }

  public boolean isAttached(String attachable) {
    return attached.contains(attachable);
  }

  /**
   * track the state of listening process
   */
  @Override
  public boolean isListening() {
    return isListening;
  }

  @Override
  @Deprecated /* use publishListening(boolean event) */
  public void listeningEvent(Boolean event) {
    isListening = event;
    broadcastState();
    return;
  }

  @Override
  @Deprecated /* use wake word */
  public void lockOutAllGrammarExcept(String lockPhrase) {
    log.info("Ear locked now, please use command " + lockPhrase + " to unlock");
    lockOutAllGrammar = true;
    this.lockPhrase = lockPhrase;
  }

  /**
   * Default implementation of onEndSpeaking event from a SpeechSythesis service
   * is to start listening when speaking
   */
  @Override
  public void onEndSpeaking(String utterance) {
    log.info("onEndSpeaking - isSpeaking {} - utterance -{}", isSpeaking, utterance);

    // need to subscribe to this in the webgui
    // so we can resume listening.
    // this.speaking = false;
    // startListening(); - user controls "listening" - SpeechSynthesis can
    // affect "recognizing"
    // FIXME - add a deta time after ...

    if (afterSpeakingPauseMs > 0) {
      // remove previous one shot - because we are "sliding" the window of
      // stopping the publishing of recognized words
      addTaskOneShot(afterSpeakingPauseMs, "setSpeaking", new Object[] { false });
      log.warn("isSpeaking = false will occur in {} ms", afterSpeakingPauseMs);
    } else {
      setSpeaking(false, null);
    }
  }

  public boolean setSpeaking(boolean b) {
    return setSpeaking(b, null);
  }

  // start publishing recognized events
  // startRecognizing();

  public boolean setSpeaking(boolean b, String utterance) {
    log.info("setSpeaking - isSpeaking {} is now {} - publish listening event utterance - {}", isSpeaking, b, utterance);

    isSpeaking = b;

    ListeningEvent event = new ListeningEvent();
    event.isRecording = isRecording;
    event.isListening = isListening;
    event.isAwake = isAwake;
    event.isSpeaking = isSpeaking;

    event.text = utterance;
    invoke("publishListeningEvent", event);

    if (isSpeaking) {
      log.warn("======================= started speaking - stopped listening  =======================================");
    } else {
      log.warn("======================= stopped speaking - started listening  =======================================");
    }
    return b;
  }

  @Override
  public String onStartSpeaking(String utterance) {
    log.info("onStartSpeaking - isSpeaking {} utterance - {}", isSpeaking, utterance);
    // remove any currently pending "no longer listening" delay tasks, because
    // we started a new isSpeaking = true, so the pause window after has moved
    purgeTask("setSpeaking");
    // isSpeaking = true;
    setSpeaking(true, utterance);
    return utterance;
  }

  @Override
  @Deprecated /*
               * uset stopListening() and startListening() or stopRecognizing
               * startRecognizing
               */
  public void pauseListening() {
    stopListening();
  }

  public ListeningEvent[] processResults(ListeningEvent[] results) {
    // at the moment its simply invoking other methods, but if a new speech
    // recognizer is created - it might need more processing

    for (int i = 0; i < results.length; ++i) {
      ListeningEvent event = results[i];
      event.isRecording = isRecording;
      event.isListening = isListening;
      event.isAwake = isAwake;
      event.isSpeaking = isSpeaking;

      if (normalize) {
        event.text = (event.text != null) ? event.text.trim().toLowerCase().replace("\\p{P}", "") : null;
      }

      if (wakeWord != null && !isAwake) {

        // case : is asleep, and wake word matches in text
        if (event.text != null && event.text.toLowerCase().contains(wakeWord.toLowerCase())) {
          info("wake word match on %s in %s, idle timer starts to sleep in {} seconds", wakeWord, event.text, wakeWordIdleTimeoutSeconds);
          String originalText = event.text;
          if (removeWakeWord) {
            event.text = event.text.replace(wakeWord.toLowerCase(), "");
          }

          purgeTask("setAwake");
          addTaskOneShot(wakeWordIdleTimeoutSeconds * 1000, "setAwake", false);
          lastWakeWordTs = System.currentTimeMillis();
          
          // setting awake
          setAwake(true, originalText);
          event.isAwake = true;

        }

      } else if (wakeWord != null && isAwake) {
        // slide window of wake word idle time
        purgeTask("setAwake");
        addTaskOneShot(wakeWordIdleTimeoutSeconds * 1000, "setAwake", false);
      }

      lastThingRecognized = event.text;

      if (!isSpeaking && event.text != null && event.isAwake) {
        event.publishText = true;
      } else {
        log.info("===== NOT going to invoke publishText on recognized \"{}\"  ======", event.text);
      }
      // if (result.text != null && !result.isSpeaking && result.isAwake) {
      invoke("publishListeningEvent", event);

    } // for each listening event

    return results;
  }

  public void setAwake(boolean b) {
    setAwake(b, null);
  }

  public void setAwake(boolean b, String text) {

    if (!b && isSpeaking) {
      log.info("bot is speaking - bot doesn't get tired when talking about self sliding idle timeout");
      purgeTask("setAwake");
      addTaskOneShot(wakeWordIdleTimeoutSeconds * 1000, "setAwake", false);
      return;
    }
    // don't go to sleep if speaking
    // instead slide window

    isAwake = b;

    ListeningEvent event = new ListeningEvent();
    event.isRecording = isRecording;
    event.isListening = isListening;
    event.isAwake = isAwake;
    event.isSpeaking = isSpeaking;
    event.text = text;

    invoke("publishListeningEvent", event);
  }

  @Override
  public boolean publishListening(boolean listening) {
    this.isListening = listening;
    return listening;
  }

  @Override
  public String publishRecognized(String text) {
    return text;
  }

  @Override
  public ListeningEvent publishListeningEvent(ListeningEvent result) {
    log.warn("publishListeningEvent \"{}\" !!!! ======", result.text);
    if (result.publishText) {
      invoke("publishRecognized", result.text);
      invoke("publishText", result.text);
    }
    return result;
  }

  @Override
  public String publishText(String text) {
    log.info("publishText -> {}", text);
    return text;
  }

  @Override
  @Deprecated /* use standard publishRecognized to setup subscriptions */
  public String recognized(String text) {
    log.info("recognized  : >{}< ", text);
    // invoke("publishText", text); - we don't even need to do this
    // because the WebkitSpeechRecognition.js calls publishText

    String cleanedText = (lowerCase) ? text.toLowerCase().trim() : text;
    /*
     * should not be done here ... if (isStripAccents()) { cleanedText =
     * StringUtil.removeAccents(cleanedText); }
     */

    if (text.equalsIgnoreCase(lockPhrase)) {
      clearLock();
    }
    lastThingRecognized = cleanedText;
    broadcastState();
    if (!lockOutAllGrammar) {
      if (commands.containsKey(cleanedText)) {
        // If we have a command. send it when we recognize...
        Message msg = commands.get(cleanedText);
        send(msg);
      }
      return cleanedText;
    } else {
      log.info("Speech recognizer is locked by keyword : {}", lockPhrase);
    }
    return "";
  }

  @Override
  @Deprecated /* use stopListening() and startListening() */
  public void resumeListening() {
    startListening();
  }

  public void setLowerCase(boolean b) {
    lowerCase = b;
  }

  /**
   * setting the wake word - wake word behaves as a switch to turn on "active
   * listening" similar to "hey google"
   * 
   * @param word
   */
  public void setWakeWord(String word) {
    if (word == null || word.trim().length() == 0) {
      word = null;
      log.info("unsetting wake word");
      purgeTask("wakeWordIdleTimeoutSeconds");
      setAwake(true);
    } else {
      setAwake(false);
    }
    this.wakeWord = word.trim();
    broadcastState();
  }

  /**
   * length of idle time or silence until the wake word is needed to activate
   * again
   * 
   * @param wakeWordTimeoutSeconds
   */
  public void setWakeWordTimeout(Integer wakeWordTimeoutSeconds) {
    wakeWordIdleTimeoutSeconds = wakeWordTimeoutSeconds;
    broadcastState();
  }

  @Override
  public void startListening() {
    log.debug("Start listening event seen.");
    isListening = true;
    broadcastState();
  }

  /**
   * for webkit - startRecognizer consists of setting a property and
   * broadcasting self to the webgui
   */
  @Override
  public void startRecording() {
    isRecording = true;
    broadcastState();
  }

  public void startService() {
    super.startService();
    startRecording();
    startListening();
  }

  /**
   * prevents the publishing of recognized events - does NOT prevent audio being
   * recorded and processed for webkit - startRecognizer consists of setting a
   * property and broadcasting self to the webgui
   */
  @Override
  public void stopListening() {
    log.debug("stopListening()");
    isListening = false;
    broadcastState();
  }

  /**
   * This will prevent audio from being recorded
   */

  @Override
  public void stopRecording() {
    isRecording = false;
    broadcastState();
  }

  public void stopService() {
    super.stopService();
    stopListening();
    stopRecording();
  }

  public long setAfterSpeakingPause(long ms) {
    afterSpeakingPauseMs = ms;
    return afterSpeakingPauseMs;
  }

  public long getAfterSpeakingPause() {
    return afterSpeakingPauseMs;
  }

  /**
   * Stop wake word functionality .. after being called stop and start
   */
  public void unsetWakeWord() {
    setWakeWord(null);
  }

  @Override
  public void setLocale(String code) {
    locale = new Locale(code);
  }

  @Override
  public String getLanguage() {
    return locale.getLanguage();
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

}
