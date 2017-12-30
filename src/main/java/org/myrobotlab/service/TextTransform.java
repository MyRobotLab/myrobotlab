package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class TextTransform extends Service implements TextListener, TextPublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TextTransform.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("transform", "TextTransform");

      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public TextTransform(String n) {
    super(n);
  }

  @Override
  public void onText(String text) {
    // TODO Auto-generated method stub

  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
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

    ServiceType meta = new ServiceType(TextTransform.class.getCanonicalName());
    meta.addDescription("TextTransform");
    meta.addCategory("data", "filter");
   
    // FIXME - this thing is at least 3 years old .. and does nothing I think :P
    meta.setAvailable(false);
    return meta;
  }

}
