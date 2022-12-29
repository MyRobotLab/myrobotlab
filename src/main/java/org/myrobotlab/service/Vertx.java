package org.myrobotlab.service;

import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.vertx.ApiVerticle;
import org.slf4j.Logger;

import io.vertx.core.VertxOptions;

public class Vertx extends Service {

  private static final long serialVersionUID = 1L;

  private transient io.vertx.core.Vertx vertx = null;

  public final static Logger log = LoggerFactory.getLogger(Vertx.class);

  public Vertx(String n, String id) {
    super(n, id);
  }

  public void start() {
    log.info("starting driver");

    /**
     * FIXME - might have to revisit this This is a block comment, but takes
     * advantage of javadoc pre non-formatting in ide to preserve the code
     * formatting
     * 
     * <pre>
     * 
     * final Vertx that = this;
     * 
     * java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
     *   public void run() {
     *     System.out.println("Running Shutdown Hook");
     *     that.stop();
     *   }
     * });
     * 
     * </pre>
     */

    vertx = io.vertx.core.Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(100000));
    vertx.deployVerticle(new ApiVerticle(this));

  }

  public void stop() {
    log.info("stopping driver");
    Set<String> ids = vertx.deploymentIDs();
    for (String id : ids) {
      vertx.undeploy(id, (result) -> {
        if (result.succeeded()) {

        } else {

        }
      });
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // server = new WsServer();

      Vertx vertx = (Vertx) Runtime.start("vertx", "Vertx");
      vertx.start();
      Runtime.start("servo", "Servo");
      // Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
