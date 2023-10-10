package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.vertx.ApiVerticle;
import org.slf4j.Logger;

import io.vertx.core.VertxOptions;

/**
 * Vertx gateway - used to support a http and websocket gateway for myrobotlab.
 * Write business logic in Verticles. Also, try not to write any logic besides initialization inside start() method.
 * 
 * It currently does not utilize the Vertx event bus - which is pretty much the most important part of Vertx.
 * TODO: take advantage of publishing on the event bus
 * 
 * @see https://medium.com/@pvub/https-medium-com-pvub-vert-x-workers-6a8df9b2b9ee
 * 
 * @author greg
 *
 */
public class Vertx extends Service {

  private static final long serialVersionUID = 1L;

  private transient io.vertx.core.Vertx vertx = null;

  public final static Logger log = LoggerFactory.getLogger(Vertx.class);

  public Vertx(String n, String id) {
    super(n, id);
  }

  /**
   * deploys a http and websocket verticle on a secure TLS channel with self signed certificate
   */
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
  
  @Override
  public void startService() {
    super.startService();
    start();
  }
  
  @Override
  public void stopService() {
    super.stopService();
    stop();
  }


  /**
   * 
   */
  public void stop() {
    log.info("stopping driver");
    Set<String> ids = vertx.deploymentIDs();
    for (String id : ids) {
      vertx.undeploy(id, (result) -> {
        if (result.succeeded()) {
          log.info("succeeded");
        } else {
          log.error("failed");
        }
      });
    }
  }

  public static class Matrix {
    public String name;
    public HashMap<String, Float> matrix;

    public Matrix() {
    };
  }

  public Matrix publishMatrix(Matrix data) {
    // log.info("publishMatrix {}", data.name);
    return data;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Vertx vertx = (Vertx) Runtime.start("vertx", "Vertx");
      vertx.start();

       InMoov2 i01 = (InMoov2)Runtime.start("i01", "InMoov2");
       // i01.startSimulator();
       JMonkeyEngine jme = (JMonkeyEngine)i01.startPeer("simulator");
//       Runtime.start("python", "Python");
//      
       WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
       // webgui.setSsl(true);
       webgui.autoStartBrowser(false);
       webgui.setPort(8888);
       webgui.startService();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
