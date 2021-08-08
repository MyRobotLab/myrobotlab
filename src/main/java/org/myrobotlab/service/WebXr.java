package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class WebXr extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebXr.class);

  public WebXr(String n, String id) {
    super(n, id);
  }

  public static void main(String[] args) {
    try {

      // Runtime.main(new String[] { "--from-launcher", "--log-level", "debug"});
      Runtime.main(new String[] { "--from-launcher"});

      LoggingFactory.init(Level.DEBUG);

      Runtime.start("webxr", "WebXr");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      webgui.startService();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
