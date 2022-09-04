package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer.ListeningEvent;

public interface SpeechRecognizer extends NameProvider, TextPublisher, LocaleProvider, SpeechListener {

  /**
   * This method should listen for Mouth events
   * 
   * FIXME - should be deprecated - use Attach Pattern !
   * 
   * @param mouth
   *          m
   */
  @Deprecated /* use attachSpeechSynthesis(SpeechSynthesis mouth) */
  public void addMouth(SpeechSynthesis mouth);

  @Deprecated /* use attachTextListener(TextListener listener) */
  public void addTextListener(TextListener listener);

  /**
   * This typically will suppress listening to itself when it speaks creating an
   * endless self dialog :P
   * 
   * @param mouth
   *          the speech synthesis to attach
   */
  public void attachSpeechSynthesis(SpeechSynthesis mouth);

  /**
   * Set up subscriptions/listeners to publish recognized text too this text
   * listener
   * 
   */
  public void attachTextListener(TextListener listener);

  /**
   * This will unlock lockOutAllGrammarExcept(lockPhrase)
   */
  @Deprecated /* legacy pre-wake word */
  public void clearLock();

  /**
   * track the state of listening process
   * 
   * @return true if listening
   */
  public boolean isListening();

  /**
   * @param event
   *          Event is sent when the listening Service is actually listening or
   *          not.
   */
  @Deprecated /* use publishListening(boolean event) */
  public void listeningEvent(Boolean event);

  /**
   * method to suppress recognition listening events This is important when a
   * Speech Recognizer is listening --&gt; then Speaking, typically you don't
   * want the STT to listen to its own speech, it causes a feedback loop and
   * with STT not really very accurate, it leads to weirdness -- additionally it
   * does not recreate the speech processor - so its not as heavy handed
   */
  @Deprecated /* legacy sphinx - use stop/start listening */
  public void pauseListening();

  /**
   * Publish event when listening or not listening ...
   * 
   * @param event
   *          e
   * @return the event
   * 
   */
  public boolean publishListening(boolean event);

  /**
   * the recognized text
   * 
   * @param text
   *          text to be published
   * @return the text
   * 
   */
  public String publishRecognized(String text);

  /**
   * the text in addition to any meta data like confidence rating
   * 
   * @param result
   *          r
   * @return listening event
   * 
   */
  public ListeningEvent publishListeningEvent(ListeningEvent result);

  @Deprecated /* should use standard publishRecognized */
  public String recognized(String word);

  @Deprecated /* use stopListening() and startListening() */
  public void resumeListening();

  /**
   * Start recognizing allows recognized events to be published
   */
  public void startListening();

  /**
   * Stop recognizing continues listening and recording audio, but will not
   * publish recognized events
   */
  public void stopListening();

  /**
   * Start recording begins recording and initially starts recognizing unless a
   * wake word is used. If a wake word is used - recording starts but listening
   * and publishing recognized speech is prevented from publishing until the
   * wake word is recognized
   */
  public void startRecording();

  /**
   * Stop listening stops the recording and and any possibility of recognizing
   * incoming audio
   */
  public void stopRecording();

  /**
   * Setting the wake word - wake word behaves as a switch to turn on "active
   * listening" similar to "hey google"
   * 
   * @param word
   *          wake word to set
   * 
   */
  public void setWakeWord(String word);

  /**
   * @return Get the current wake word
   * 
   */
  public String getWakeWord();

  /**
   * Stop wake word functionality .. after being called stop and start
   */
  public void unsetWakeWord();

  public void lockOutAllGrammarExcept(String lockPhrase);

}
