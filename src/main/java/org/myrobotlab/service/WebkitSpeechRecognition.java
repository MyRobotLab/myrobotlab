package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the
 * chrome web browser this service requires the webgui to be running.
 *
 */
public class WebkitSpeechRecognition extends AbstractSpeechRecognizer {

  private static final long serialVersionUID = 1L;

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
    WebkitSpeechRecognition webkitspeechrecognition = (WebkitSpeechRecognition) Runtime.start("webkitspeechrecognition", "WebkitSpeechRecognition");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/tabs/webkitspeechrecognition");

  }

  @Deprecated /* webkit automatically uses "continuous" */
  boolean continuous = true;

  @Deprecated /* doesn't belong here - should be in a text processor */
  boolean stripAccents = false;

  boolean startRecognizer = true;

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

  /**
   * for webkit - startRecognizer consists of setting a property and
   * broadcasting self to the webgui
   */
  @Override
  public void startRecognizer() {
    startRecognizer = true;
    broadcastState();
  }

  /**
   * for webkit - startRecognizer consists of setting a property and
   * broadcasting self to the webgui
   */
  @Override
  public void stopRecognizer() {
    startRecognizer = false;
    broadcastState();
  }

}