package org.myrobotlab.net;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereSocket;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

public class WsConnector implements Decoder<String, Reader>, Encoder<String, Reader> {

  /**
   * Atmosphere client
   */
  transient static private AtmosphereClient client = null;

  /**
   * The underlying client runtime engine (Netty)
   */
  transient static private AsyncHttpClient asc = null;

  transient private Map<String, Socket> sockets = new HashMap<>();

  public WsConnector() {
    if (client == null) {
      init();
    }
  }

  public String getUrl(String url, String id) {
    return String.format("%s/api/messages?id=%s", url, id);
  }

  public void connect(String url, String id) throws IOException {
    connect(url, id, null, null);
  }

  public void connect(String url, String id, String username, String password) throws IOException {
    RequestBuilder<?> request = null;

    // TODO - username & password in json POST ?

    request = client.newRequestBuilder();
    request.method(Request.METHOD.GET);
    request.uri(getUrl(url, id));
    request.encoder(this).decoder(this);
    // Can request for other transports like WEBSOCKET, SSE, STREAMING,
    // LONG_POLLING
    request.transport(Request.TRANSPORT.WEBSOCKET);

    final Socket socket = (AtmosphereSocket) client.create(client.newOptionsBuilder().runtime(asc).build());
    socket.on(Event.CLOSE.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // System.out.println("CLOSE " + t);
      }
    }).on(Event.REOPENED.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // System.out.println("REOPENED " + t);
      }
    }).on(Event.ERROR.name(), new Function<String>() {
      @Override
      public void on(String t) {
        System.out.println("ERROR " + t);
      }
    }).on(Event.MESSAGE.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // all messages
        // System.out.println("MESSAGE {}" + t);
      }
    }).on(new Function<IOException>() {
      @Override
      public void on(IOException ioe) {
        System.out.println(ioe.getMessage());
        // socket = null;
        // ioe.printStackTrace();
      }
    }).on(Event.STATUS.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // System.out.println("STATUS " + t);
      }
    }).on(Event.HEADERS.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // System.out.println("HEADERS " + t);
      }
    }).on(Event.MESSAGE_BYTES.name(), new Function<String>() {
      @Override
      public void on(String msg) {
        // System.out.println("MESSAGE_BYTES " + t);
        System.out.println(msg);
      }
    }).on(Event.OPEN.name(), new Function<String>() {
      @Override
      public void on(String t) {
        // System.out.println("OPEN " + t);
      }
    }).open(request.build());

    sockets.put(id, socket);
  }

  /**
   * initialization of underlying netty runtime engine and wsasync client
   */
  private synchronized static void init() {
    if (client == null) {
      client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

      // Netty Config ..
      NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
      nettyConfig.addProperty("tcpNoDelay", "true");
      nettyConfig.addProperty("keepAlive", "true");
      nettyConfig.addProperty("reuseAddress", true);
      // nettyConfig.addProperty("connectTimeoutMillis",
      // nettyConnectionTimeout);
      nettyConfig.setWebSocketMaxFrameSize(262144);
      nettyConfig.addProperty("child.tcpNoDelay", "true");
      nettyConfig.addProperty("child.keepAlive", "true");
      // nettyConfig.setWebSocketMaxFrameSize(65536);

      // AsyncHttpClientConfig Config
      AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
      b.setFollowRedirect(true).setMaxRequestRetry(-1).setConnectTimeout(-1).setReadTimeout(30000);
      AsyncHttpClientConfig config = b.setAsyncHttpClientProviderConfig(nettyConfig).build();
      asc = new AsyncHttpClient(config);
    }
  }

  public static void main(String[] args) {

  }

  @Override
  public Reader encode(String s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Reader decode(Event e, String s) {
    // TODO Auto-generated method stub
    return null;
  }

}
