package org.myrobotlab.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.atmosphere.wasync.*;

import org.myrobotlab.framework.Service;
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

  private Client client;
  private Socket socket;

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
   * @throws IOException 
   */
  public void connect(String url) throws URISyntaxException, IOException {

    client = ClientFactory.getDefault().newClient();
    socket = null;

    RequestBuilder request = client.newRequestBuilder()
            .method(Request.METHOD.GET)
            .uri(url)
            .encoder(new Encoder<String, Reader>() {        // Stream the request body
                @Override
                public Reader encode(String s) {
                    return new StringReader(s);
                }
            })
            .decoder(new Decoder<String, Reader>() {
                @Override
                public Reader decode(Event type, String s) {
                    return new StringReader(s);
                }
            })
            .transport(Request.TRANSPORT.WEBSOCKET)                        // Try WebSocket
            .transport(Request.TRANSPORT.LONG_POLLING);                    // Fallback to Long-Polling

    Socket socket = client.create();
    socket.on(new Function<Reader>() {
        @Override
        public void on(Reader r) {
            // Read the response
        }
    }).on(new Function<IOException>() {

        @Override
        public void on(IOException ioe) {
            // Some IOException occurred
        }

    }).open(request.build())
        .fire("echo")
        .fire("bong");
  }

  /**
   * Send a message over the websocket
   * 
   * @param message
   * @throws IOException 
   */
  public void send(String message) throws IOException {
    socket.fire(message);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    Runtime.start("swing", "SwingGui");
    Runtime.start("python", "Python");
    WebSocketConnector wsc = (WebSocketConnector) Runtime.start("wsc", "WebSocketConnector");
  }

 
  @Override
  public void attachTextListener(TextListener service) {
    addListener("publishText", service.getName());
  }
}
