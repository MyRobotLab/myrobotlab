package org.myrobotlab.service;

import java.io.IOException;
import java.net.URISyntaxException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.WsClient;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.ConnectionManager;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * @author MaVo (MyRobotLab) / LunDev (GitHub)
 */

public class WebSocketConnector extends Service<ServiceConfig> implements RemoteMessageHandler, ConnectionManager, TextPublisher {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(WebSocketConnector.class);

  transient private WsClient client;

  public WebSocketConnector(String n, String id) {
    super(n, id);
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  /**
   * Open the connection to a websocket
   * 
   * @param url
   *          the url of the websocket
   * @throws URISyntaxException
   *           boom
   * @throws IOException
   *           boom
   */
  public void connect(String url) throws URISyntaxException, IOException {

    client = new WsClient();
    client.connect(this, url);

  }

  /**
   * Send a message over the websocket
   * 
   * @param message
   *          the message to send
   * @throws IOException
   *           boom
   * 
   */
  public void send(String message) throws IOException {
    if (client != null) {
      client.send(message);
    } else {
      error("client not ready - connect first");
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    Runtime.start("swing", "SwingGui");
    Runtime.start("python", "Python");
    WebSocketConnector wsc = (WebSocketConnector) Runtime.start("wsc", "WebSocketConnector");
  }

  @Override
  public void addConnection(String uuid, String id, Connection attributes) {
    // TODO - use if you want to add to runtime routing table and use this
    // service as a gateway
  }

  @Override
  public void removeConnection(String uuid) {
    // TODO - use if you want to add to runtime routing table and use this
    // service as a gateway
  }

  @Override
  public void onRemoteMessage(String uuid, String data) {
    invoke("publishText", data);
  }

}
