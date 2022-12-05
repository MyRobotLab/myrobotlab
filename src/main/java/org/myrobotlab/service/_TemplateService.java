package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

public class _TemplateService extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

  public _TemplateService(String n, String id) {
    super(n, id);
  }

  /**
   * The methods apply and getConfig can be used, if more complex configuration handling is needed.
   * By default, the framework takes care of most of it, including subscription handling.
   * <pre>
  @Override
  public ServiceConfig apply(ServiceConfig c) {
    // _TemplateServiceConfig config = (_TemplateService)super.apply(c);
    // if more complex config handling is needed
    return c;
  }

  @Override
  public ServiceConfig getConfig() {
    // _TemplateServiceConfig config = (_TemplateService)super.getConfig();
    return config;
  }
  </pre>
  **/

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
