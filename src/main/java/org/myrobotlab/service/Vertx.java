package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.VertxConfig;
import org.myrobotlab.vertx.WsServer;
import org.slf4j.Logger;

public class Vertx extends Service {

  private static final long serialVersionUID = 1L;

  private transient WsServer server = null;

  private transient io.vertx.core.Vertx vertx = null;

  public final static Logger log = LoggerFactory.getLogger(Vertx.class);

  public Vertx(String n, String id) {
    super(n, id);
  }


  public void start() {
    if (server == null) {
      server = new WsServer();
      VertxConfig c = (VertxConfig) config;
      server.setPort(c.port);
      vertx = io.vertx.core.Vertx.vertx();
      vertx.deployVerticle(server);
    } else {
      log.info("{} already started", getName());
    }
  }

  public void stop() {
    if (server != null) {
      vertx.undeploy(server.getDeploymentId());
    } else {
      log.info("{} already stopped", getName());
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // server = new WsServer();

      Runtime.start("vertx", "Vertx");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
