package org.myrobotlab.service.interfaces;

public interface SpeechRecognizer extends NameProvider {

  /**
   * Event is sent when the listening Service is actually listening. There is
   * some delay when it initially loads.
   */
  public void listeningEvent();

  /**
   * method to suppress recognition listening events This is important when a
   * Speech Recognizer is listening --> then Speaking, typically you don't want
   * the STT to listen to its own speech, it causes a feedback loop and with STT
   * not really very accurate, it leads to weirdness -- additionally it does not
   * recreate the speech processor - so its not as heavy handed
   */
  public void pauseListening();

  public String recognized(String word);

  public void resumeListening();

  public void startListening();

  public void stopListening();

  // This method should listen for
  public void addMouth(SpeechSynthesis mouth);

  public void onStartSpeaking(String utterance);

  public void onEndSpeaking(String utterance);

  // TODO: rationalize these 2 interfaces maybe re-name them?
  public void lockOutAllGrammarExcept(String lockPhrase);

  public void clearLock();

}
