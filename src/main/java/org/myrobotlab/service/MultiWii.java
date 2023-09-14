package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
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
public class MultiWii extends Service<ServiceConfig> {

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

  public MultiWii(String n, String id) {
    super(n, id);
  }

}
