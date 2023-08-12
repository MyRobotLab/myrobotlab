package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;
/**
 * 
 * 
 * 
 * 
 * @author GroG
 *
 */
public class OakD extends Service<ServiceConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OakD.class);

  public OakD(String n, String id) {
    super(n, id);
  }
  

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("camera", "OakD");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
