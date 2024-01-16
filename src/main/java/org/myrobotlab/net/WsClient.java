package org.myrobotlab.net;

import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.ConnectionEventListener;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.slf4j.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Simple best websocket client for mrl. TODO - use it as a cli to remote
 * interface
 * 
 * @author GroG
 *
 */
public class WsClient extends WebSocketListener {

  public final static Logger log = LoggerFactory.getLogger(WsClient.class);

  private final transient OkHttpClient client = new OkHttpClient.Builder()
          .readTimeout(60000, TimeUnit.MILLISECONDS)
          .build();
  /**
   * service if it exists
   */
  transient private ServiceInterface si = null;
  transient private ConnectionEventListener listener = null;
  transient private WebSocket socket = null;
  protected String url = null;
  /**
   * unique identifier for this client
   */
  protected String uuid = java.util.UUID.randomUUID().toString();
  /**
   * callback handler if it exists
   */
  transient private RemoteMessageHandler handler = null;

  public WsClient() {
    // Best to keep the default constructor for explicitness
  }

  /**
   * connect to a listening websocket e.g. connect("ws://localhost:8888")
   * 
   * @param url - url to connect to
   */
  public void connect(String url) {
    connect(null, url);
  }

  /**
   * get this clients unique id
   * 
   * @return - a uuid
   */
  public String getId() {
    return uuid;
  }

  public void connect(Object si, String url) {
    
    this.url = url;

    if (url == null) {
      error("url cannot be null");
      return;
    }
    
    if (si instanceof Service) {
      this.si = (Service)si;  
    }

    if (si instanceof RemoteMessageHandler) {
      handler = (RemoteMessageHandler) si;
    }

    if (si instanceof ConnectionEventListener) {
      listener = (ConnectionEventListener) si;
    }

    Request request = new Request.Builder().url(url).build();
    socket = client.newWebSocket(request, this);

    // Trigger shutdown of the dispatcher's executor so this process can exit
    // cleanly.
    client.dispatcher().executorService().shutdown();
  }

  private void error(String error) {
    if (si != null) {
      si.error(error);
    } else {
      log.error(error);
    }
  }

  public void send(String json) {
    if (socket == null) {
      error("must connect first");
      return;
    }
    // log.error(json);
    socket.send(json);
  }

  public void send(ByteString bytes) {
    if (socket == null) {
      error("must connect first");
      return;
    }
    socket.send(bytes);
  }

  // FIXME Need to add @NonNull to overriden method params once we standardize on an annotation lib

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    log.info("ONOPEN: ");
    // socket = webSocket;
    if (listener != null) {
      listener.onOpen(webSocket, response);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("MESSAGE: %s", text));
    }
    if (handler != null) {
      if ("X".equals(text)) {
        // ignore Atmosphere does a weird sending of X characters I assume
        // to make sure the connection is unbroken
        return;
      } 
      handler.onRemoteMessage(uuid, text);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    log.info("BYTE MESSAGE: " + bytes.hex());
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    webSocket.close(1000, null);
    log.info("CLOSE: " + code + " " + reason);
    if (listener != null) {
      listener.onClosing(webSocket, code, reason);
    }
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    if (listener != null) {
      listener.onFailure(webSocket, t, response);
    } else {
      t.printStackTrace();
      error(new Exception(t));
    }
  }

  private void error(Exception t) {
    if (si != null) {
      si.error(t);
    } else {
      log.error("on thrown failure", t);
    }
  }

  public void close() {
    if (socket != null) {
      socket.close(1000, "request to close");
    }
  }

  public static void main(String[] args) throws Exception {

    new WsClient().connect("ws://localhost:6437");
    // ws.sendText("Hello!", true);
    log.info("done");
  }

}