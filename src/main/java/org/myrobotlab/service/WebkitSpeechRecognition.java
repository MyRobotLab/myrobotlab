package org.myrobotlab.service;

import java.util.Map;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.data.Locale;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the
 * chrome web browser this service requires the webgui to be running.
 *
 */
public class WebkitSpeechRecognition extends AbstractSpeechRecognizer {

  private static final long serialVersionUID = 1L;

  /**
   * mic image
   */
  String img = "../WebkitSpeechRecognition/mic.png";

  /**
   * current status of the webkit recognizer
   */
  String status = null;

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
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    // Runtime.start("gui", "SwingGui");
    WebkitSpeechRecognition webkit = (WebkitSpeechRecognition) Runtime.start("webkit", "WebkitSpeechRecognition");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();

    // webkit.stopRecognizing();
    // webkit.stopListening();

    // webgui.startBrowser("http://localhost:8888/#/service/webkitspeechrecognition");

  }

  @Deprecated /* webkit automatically uses "continuous" */
  boolean continuous = true;

  @Deprecated /* doesn't belong here - should be in a text processor */
  boolean stripAccents = false;

  public WebkitSpeechRecognition(String n, String id) {
    super(n, id);
  }

  @Deprecated /* artifact implementation of webkit */
  public boolean getContinuous() {
    return this.continuous;
  }

  @Deprecated /* wrongly placed ? not in speech recognition ? */
  public boolean isStripAccents() {
    return stripAccents;
  }

  /**
   * If setContinuous is False, this speedup recognition processing If
   * setContinuous is True, you have some time to speak again, in case of error
   */
  @Deprecated /* artifact of webkit */
  public void setContinuous(boolean continuous) {
    this.continuous = continuous;
    broadcastState();
  }

  @Deprecated /* should not be here */
  public void setStripAccents(boolean stripAccents) {
    this.stripAccents = stripAccents;
  }

  @Override
  public Map<String, Locale> getLocales() {
    Map<String, Locale>  ret = Locale.getLocaleMap("en-US", "en-GB", "af-ZA", "id-ID", "ms-MY", "ca-ES", "cs-CZ", "da-DK", "de-DE", "en-AU", "en-CA", "en-IN", "en-NZ", "en-ZA", "en-GB", "en-US", "es-AR", "es-BO",
        "es-CL", "es-CO", "es-CR", "es-EC", "es-SV", "es-ES", "es-US", "es-GT", "es-HN", "es-MX", "es-NI", "es-PA", "es-PY", "es-PE", "es-PR", "es-DO", "es-UY", "es-VE", "eu-ES",
        "fil-PH", "fr-FR", "gl-ES", "hi-IN", "hr_HR", "zu-ZA", "is-IS", "it-IT", "it-CH", "lt-LT", "hu-HU", "nl-NL", "nb-NO", "pl-PL", "pt-BR", "pt-PT", "ro-RO", "sl-SI", "sk-SK",
        "fi-FI", "sv-SE", "vi-VN", "tr-TR", "el-GR", "bg-BG", "ru-RU", "sr-RS", "uk-UA", "ko-KR", "ja-JP", "th-TH", "zh-cmn-Hans-HK", "zh-cmn-Hant-TW", "zh-yue-Hant-HK", "zh-cmn-Hans-CN");
    return ret;
  }

}