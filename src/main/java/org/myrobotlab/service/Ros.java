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
      Ros ros = new Ros("ros");
      ros.startService();
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Ros(String n) {
    super(n);
  }

  static public String[] getCategories() {
    return new String[] { "bridge" };
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

    ServiceType meta = new ServiceType(Ros.class.getCanonicalName());
    meta.addDescription("interface to Ros");
    meta.addCategory("bridge");
    meta.addPeer("serial", "Serial", "serial");
    meta.setAvailable(false);

    return meta;
  }

}
