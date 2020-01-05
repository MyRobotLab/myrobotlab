package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer.RecognizedResult;

public interface SpeechRecognizer extends NameProvider {

  /**
   * This method should listen for Mouth events
   * 
   * FIXME - should be deprecated - use Attach Pattern !
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
   */
  public void attachSpeechSynthesis(SpeechSynthesis mouth);

  /**
   * Set up subscriptions/listeners to publish recognized text too this text
   * listener
   * 
   * @param listener
   */
  public void attachTextListener(TextListener listener);

  /**
   * This will unlock lockOutAllGrammarExcept(lockPhrase)
   */
  @Deprecated /* legacy pre-wake word */
  public void clearLock();

  /**
   * track the state of listening process
   */
  public boolean isListening();

  /**
   * Event is sent when the listening Service is actually listening or not.
   */
  @Deprecated /* use publishListening(boolean event) */
  public void listeningEvent(Boolean event);


  /**
   * speech synthesis interface - to not listen while speaking
   * 
   * @param utterance
   */
  public void onEndSpeaking(String utterance);

  /**
   * speech synthesis interface - to not listen while speaking
   * 
   * @param utterance
   */
  public void onStartSpeaking(String utterance);

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
   * @return
   */
  public boolean publishListening(boolean event);

  /**
   * the recognized text
   * 
   * @param text
   * @return
   */
  public String publishRecognized(String text);

  /**
   * the text in addition to any meta data like confidence rating
   * 
   * @param result
   * @return
   */
  public RecognizedResult publishRecognizedResult(RecognizedResult result);

  @Deprecated /* should use standard publishRecognized */
  public String recognized(String word);

  @Deprecated /* use stopListening() and startListening() */
  public void resumeListening();

  /**
   * Start listening is a command to make the Speech Recognizer begin listening
   * - however, in preferred implementation this "really" means - start
   * publishing recogized events. When it is desired to completely stop
   * listening - stopRecognizer should be used or stop/release the service
   */
  public void startListening();

  /**
   * recognizer may still be active, and possibly looking for wake word - but no events will be published
   * until start listening is called by user or automatically after recognizing wake word
   */
  public void stopListening();
  
  /**
   * start recognizer so wake word can be recognized, or recogized event can potentially be published
   * - requires startListening for events to be published
   */
  public void startRecognizer();

  /**
   * shut down recognizer - no events will be published, no audio will be captured
   */
  public void stopRecognizer();


  /**
   * setting the wake word - wake word behaves as a switch to turn on "active
   * listening" similar to "hey google"
   * 
   * @param word
   */
  public void setWakeWord(String word);

  /**
   * Get the current wake word
   * 
   * @return
   */
  public String getWakeWord();

  /**
   * Stop wake word functionality .. after being called stop and start
   */
  public void unsetWakeWord();
  
  public void lockOutAllGrammarExcept(String lockPhrase);

}
