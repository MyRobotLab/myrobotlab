package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * BeagleBoardBlack - Skeleton of Beagle Board Black service. Primarily this
 * service will allow access through Java to the GPIO of the BBB. Needs a Pi4J
 * code to be ported to a BBB4J library.
 */
public class BeagleBoardBlack extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(BeagleBoardBlack.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {
      BeagleBoardBlack bbb = new BeagleBoardBlack("bbb");
      bbb.startService();

      Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public BeagleBoardBlack(String n) {
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

    ServiceType meta = new ServiceType(BeagleBoardBlack.class.getCanonicalName());
    meta.addDescription("service to access the beagle board black hardware");
    meta.addCategory("microcontroller");
    meta.setAvailable(false);
    return meta;
  }

}
