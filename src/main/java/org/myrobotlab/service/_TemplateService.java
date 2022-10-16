package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.slf4j.Logger;

public class _TemplateService extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

  public _TemplateService(String n, String id) {
    super(n, id);
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    ServoConfig config = (ServoConfig) c;
    return c;
  }

  @Override
  public ServiceConfig getConfig() {
    return config;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
