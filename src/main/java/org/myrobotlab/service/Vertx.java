package org.myrobotlab.service;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.config.VertxConfig;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.vertx.ApiVerticle;
import org.slf4j.Logger;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.ServerWebSocket;

/**
 * Vertx gateway - used to support a http and websocket gateway for myrobotlab.
 * Write business logic in Verticles. Also, try not to write any logic besides
 * initialization inside start() method.
 * 
 * It currently does not utilize the Vertx event bus - which is pretty much the
 * most important part of Vertx. TODO: take advantage of publishing on the event
 * bus
 * 
 * see
 * https://medium.com/@pvub/https-medium-com-pvub-vert-x-workers-6a8df9b2b9ee
 * vertx workers
 * 
 * @author GroG
 *
 */
public class Vertx extends Service<VertxConfig> implements Gateway {

  private static final long serialVersionUID = 1L;

  private transient io.vertx.core.Vertx vertx = null;

  /**
   * If listening currently on port
   */
  protected boolean listening = false;

  public final static Logger log = LoggerFactory.getLogger(Vertx.class);

  public Vertx(String n, String id) {
    super(n, id);
  }

  /**
   * deploys a http and websocket verticle on a secure TLS channel with self
   * signed certificate
   */
  public void start() {
    log.info("starting driver");

    /**
     * TODO - relevant ? - shutdown hook for vertx
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

    // vertx = io.vertx.core.Vertx.vertx(new VertxOptions().setWorkerPoolSize(125).setBlockedThreadCheckInterval(100000));    vertx = io.vertx.core.Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(100000));
    vertx.deployVerticle(new ApiVerticle(this));

    if (config.autoStartBrowser) {
      log.info("auto starting default browser");
      String startUrl = (String.format((config.ssl) ? "https:" : "http:") + String.format("//localhost:%d/index.html", config.port));
      BareBonesBrowserLaunch.openURL(startUrl);
    }
    listening = true;
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
   * Undeploy the verticle serving http and ws
   */
  public void stop() {
    log.info("stopping driver");
    Set<String> ids = vertx.deploymentIDs();
    for (String id : ids) {
      vertx.undeploy(id, (result) -> {
        if (result.succeeded()) {
          log.info("undeploy succeeded");
        } else {
          log.error("undeploy failed");
        }
      });
    }
    listening = false;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Vertx vertx = (Vertx) Runtime.start("vertx", "Vertx");
      vertx.start();

      InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
      // i01.startSimulator();
      JMonkeyEngine jme = (JMonkeyEngine) i01.startPeer("simulator");
      // Runtime.start("python", "Python");
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

  // FIXME - refactor to bare minimum

  @Override /*
             * FIXME "Gateway" is server/service oriented not connecting thing -
             * remove this
             */
  public void connect(String uri) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  @Override /*
             * FIXME not much point of these - as they are all consistently
             * using Runtime's centralized connection info
             */
  public List<String> getClientIds() {
    return Runtime.getInstance().getClientIds();
  }

  @Override /*
             * FIXME not much point of these - as they are all consistently
             * using Runtime's centralized connection info
             */
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  @Override /*
             * FIXME this is the one and probably "only" relevant method for
             * Gateway - perhaps a handle(Connection c)
             */
  public void sendRemote(Message msg) throws Exception {
    log.info("sendRemote {}", msg.toString());
    // FIXME MUST BE DIRECT THREAD FROM BROADCAST NOT OUTBOX !!!
    msg.addHop(getId());
    Map<String, Connection> clients = getClients();
    for (Connection c : clients.values()) {
      try {
        ServerWebSocket socket = (ServerWebSocket) c.get("websocket");
        String json = CodecUtils.toJsonMsg(msg);
        socket.writeTextMessage(json);
      } catch (Exception e) {
        error(e);
      }
    }
    // broadcastMode - iterate through clients send all
  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  /**
   * Restart service on different listening port
   * 
   * @param port
   */
  public void setPort(int port) {
    config.port = port;
    boolean wasListening = listening;
    if (listening) {
      stop();
    }
    sleep(2000);
    if (wasListening) {
      start();
    }
  }

  /**
   * Starts browser when server starts
   * 
   * @param autoStartBrowser
   */
  public void setAutoStartBrowser(boolean autoStartBrowser) {
    config.autoStartBrowser = autoStartBrowser;
  }

  public void setSsl(boolean ssl) {
    config.ssl = ssl;
  }

}
