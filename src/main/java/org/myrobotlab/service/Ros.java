package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Ros extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Ros.class.getCanonicalName());

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);
    try {
      Ros ros = (Ros) Runtime.start("ros","Ros");
      ros.startService();
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Ros(String n, String id) {
    super(n, id);
  }

  static public String[] getCategories() {
    return new String[] { "bridge" };
  }

}
