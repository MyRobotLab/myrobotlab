package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * 
 * MultiWii - this is a skeleton service intended as a place holder to support
 * controling the MultiWii
 *
 * MultiWii is a general purpose software to control a multirotor RC model.
 * http://www.multiwii.com/
 */
public class MultiWii extends Service {

  transient public SerialDevice serial;

  transient public SerialDevice uart;

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(MultiWii.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("template", "_TemplateService");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public MultiWii(String n) {
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

    ServiceType meta = new ServiceType(MultiWii.class.getCanonicalName());
    meta.addDescription("MultiWii interface");
    meta.addCategory("control");
    return meta;
  }

}
