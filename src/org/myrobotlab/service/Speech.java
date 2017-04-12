package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Speech extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Speech.class);

  public Speech(String n) {
    super(n);
  }

  /**
   * TODO - default mrl Speech service ..
   *        can proxy other Speech services like Amazon, Azure, Google via cloud
   *        default to MarySpeech because it's open source
   *        
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Speech.class.getCanonicalName());
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("speech", "Speech");      
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
