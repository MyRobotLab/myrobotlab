package org.myrobotlab.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * @author MaVo (MyRobotLab) / LunDev (GitHub)
 */

public class WebSocketConnector extends Service implements TextPublisher {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(WebSocketConnector.class);

  private WebsocketClientEndpoint client;

  public WebSocketConnector(String n, String id) {
    super(n, id);
  }

  @Deprecated /* use attachTextListener */
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
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
   */
  public void connect(String url) throws URISyntaxException {
    client = new WebsocketClientEndpoint(new URI(url));
  }

  /**
   * Send a message over the websocket
   * 
   * @param message
   */
  public void send(String message) {
    client.sendMessage(message);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    Runtime.start("swing", "SwingGui");
    Runtime.start("python", "Python");
    WebSocketConnector wsc = (WebSocketConnector) Runtime.start("wsc", "WebSocketConnector");
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(WebSocketConnector.class.getCanonicalName());
    meta.addDescription("connect to a websocket");
    // meta.addCategory("");
    meta.addDependency("javax.websocket", "javax.websocket-api", "1.1");
    /*
    meta.addDependency("org.glassfish.tyrus", "tyrus-client", "1.1");
    meta.addDependency("org.glassfish.tyrus", "tyrus-container-grizzly", "1.1");
    */
    return meta;
  }

  @ClientEndpoint
  public class WebsocketClientEndpoint {

    Session userSession = null;

    public WebsocketClientEndpoint(URI endpointURI) {
      try {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, endpointURI);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession
     *          the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
      log.info("opening websocket");
      this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession
     *          the userSession which is getting closed.
     * @param reason
     *          the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
      log.info("closing websocket");
      this.userSession = null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a
     * client send a message.
     *
     * @param message
     *          The text message
     */
    @OnMessage
    public void onMessage(String message) {
      invoke("publishText", message);
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
      this.userSession.getAsyncRemote().sendText(message);
    }
  }

  @Override
  public void attachTextListener(TextListener service) {
    addListener("publishText", service.getName());
  }
}
