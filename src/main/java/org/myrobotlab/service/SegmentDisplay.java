package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

public class SegmentDisplay extends Service<ServiceConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(SegmentDisplay.class);

  public SegmentDisplay(String n, String id) {
    super(n, id);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
