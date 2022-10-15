package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

import py4j.GatewayServer;

public class Py4j extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Py4j.class);

  private transient GatewayServer server = null;

  public Py4j(String n, String id) {
    super(n, id);
  }

  /**
   * start the gateway service listening on port
   */
  public void start() {
    if (server == null) {
      server = new GatewayServer(this);
      server.start();
      info("server started listening on %s:%d", server.getAddress(), server.getListeningPort());
    } else {
      log.info("Py4j gateway server already started");
    }
  }

  /**
   * stop the gateway service
   */
  public void stop() {
    if (server != null) {
      server.shutdown();
      server = null;
    } else {
      log.info("Py4j gateway server already started");
    }
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    return c;
  }

  @Override
  public ServiceConfig getConfig() {
    return config;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("servo", "Servo");
      Py4j py4j = (Py4j) Runtime.start("py4j", "Py4j");
      py4j.start();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
