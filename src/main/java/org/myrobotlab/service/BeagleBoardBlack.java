package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

/**
 * BeagleBoardBlack - Skeleton of Beagle Board Black service. Primarily this
 * service will allow access through Java to the GPIO of the BBB. Needs a Pi4J
 * code to be ported to a BBB4J library.
 */
public class BeagleBoardBlack extends Service<ServiceConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(BeagleBoardBlack.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {
      BeagleBoardBlack bbb = (BeagleBoardBlack) Runtime.start("bbb", "BeagleBoardBlack");// new
                                                                                         // BeagleBoardBlack("bbb");
      bbb.startService();

      Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public BeagleBoardBlack(String n, String id) {
    super(n, id);
  }

}
