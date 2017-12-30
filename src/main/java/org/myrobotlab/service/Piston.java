package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Piston extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Piston.class);

  public Piston(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(Piston.class.getCanonicalName());
    meta.addDescription("used as a general template");
    meta.setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Piston piston = (Piston)Runtime.start("piston", "Piston");
      Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
      Runtime.start("gui", "SwingGui");
      
      // piston.attach()

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
