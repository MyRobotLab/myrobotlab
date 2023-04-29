package org.myrobotlab.service.interfaces;

public interface Translator {

  /**
   * Translate the incoming text from incoming language to target language This
   * method will rely on the internal state of the translation service to
   * determine incoming and target language
   * 
   * @param text
   * @return
   */
  public String

      translate(String text);

  /**
   * Translate text from fromLang to toLang explicitly
   * 
   * @param text
   *          - text to be translated
   * @param fromLang
   *          - en, es, fr, etc - incoming language
   * @param toLang
   *          - en, es, fr etc - outgoing language
   * @return
   */
  public String translate(String text, String fromLang, String toLang);
  
  /**
   * Set the source language type en, es, fr...
   * @param toLang
   */
  public void setToLanguage(String toLang);

  /**
   * Set the target language type en, es, fr...
   * @param fromLang
   */
  public void setFromLanguage(String fromLang);
  
}
