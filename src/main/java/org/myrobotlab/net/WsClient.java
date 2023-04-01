package org.myrobotlab.net;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//import com.ning.http.client.AsyncHttpClient;
//import com.ning.http.client.AsyncHttpClientConfig;
//import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
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
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.slf4j.Logger;

/**
 * functional class of a websocket client
 *
 */
public class WsClient implements Decoder<String, Reader> {

  public final static Logger log = LoggerFactory.getLogger(WsClient.class);

  protected String uuid = null;
  protected transient Socket socket = null;
  protected transient AsyncHttpClient asc = null;

  /**
   * The wAsync client that actually handles the connection
   * and socket lifecycle. By default, this is initialized to
   * {@link org.atmosphere.wasync.impl.DefaultClient}, but that
   * can be changed with the system property {@code wasync.client},
   * which should be set to the fully-qualified class name of the desired
   * client.
   */
  // Suppressing rawtypes because the generics of Client are a mess.
  // We're using the default factory w/ the default client, but that
  // signature just uses wildcards, and one of the bounds is also a raw type
  // anyway.
  @SuppressWarnings("rawtypes")
  protected transient Client client = null;
  protected transient Set<RemoteMessageHandler> handlers = new HashSet<>();

  public AsyncHttpClient getAsyncClient() {

    // ClientFactory.getDefault().newClient();
    //
    // // Netty Config ..
    // NettyAsyncHttpProviderConfig nettyConfig = new
    // NettyAsyncHttpProviderConfig();
    // nettyConfig.addProperty("tcpNoDelay", "true");
    // nettyConfig.addProperty("keepAlive", "true");
    // nettyConfig.addProperty("reuseAddress", true);
    // // nettyConfig.addProperty("connectTimeoutMillis",
    // // nettyConnectionTimeout);
    // nettyConfig.setWebSocketMaxFrameSize(262144);
    // nettyConfig.addProperty("child.tcpNoDelay", "true");
    // nettyConfig.addProperty("child.keepAlive", "true");
    // // nettyConfig.setWebSocketMaxFrameSize(65536);
    //
    // // AsyncHttpClientConfig Config
    // AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
    // b.setFollowRedirect(true).setMaxRequestRetry(-1).setConnectTimeout(-1).setReadTimeout(30000);
    // AsyncHttpClientConfig config =
    // b.setAsyncHttpClientProviderConfig(nettyConfig).build();
    // AsyncHttpClient asc = new AsyncHttpClient(config);

    asc = Dsl.asyncHttpClient();

    return asc;
  }

  // Suppressing unchecked since client is using raw types
  @SuppressWarnings("unchecked")
  public Connection connect(RemoteMessageHandler handler, String gatewayFullName, String srcId, String url) {
    try {

      if (!url.contains("api/messages")) {
        // websocket endpoint
        url += "/api/messages";
      }

      if (!url.contains("id=")) {
        url += "?id=" + srcId;
      }

      this.handlers.add(handler);
      this.client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

      UUID u = java.util.UUID.randomUUID();
      this.uuid = u.toString();

      RequestBuilder<?> request = client.newRequestBuilder();
      request.method(Request.METHOD.GET);
      request.uri(url);
      // Stream
      // System.out.println("=========== encode -----> ===========");
      // System.out.println("encoding [{}]", s);
      request.encoder((Encoder<String, Reader>) StringReader::new).decoder(this).transport(Request.TRANSPORT.WEBSOCKET); // Try
                                                               // WebSocket
      // .transport(Request.TRANSPORT.LONG_POLLING); // Fallback to
      // Long-Polling

      // client.create(client.newOptionsBuilder().reconnect(true).reconnectAttempts(999).runtime(asc).build());
      // this.socket =
      // client.create(client.newOptionsBuilder().reconnect(false).runtime(getAsyncClient()).build());
      asc = getAsyncClient();
      this.socket = client.create(client.newOptionsBuilder()./* runtime(asc). */build());
      socket
              .on(Event.CLOSE.name(), t -> System.out.println("CLOSE " + t))
              .on(Event.REOPENED.name(), t -> System.out.println("REOPENED " + t))
              .on(Event.MESSAGE.name(), t -> {
                // all messages
                // System.out.println("MESSAGE {}", t);
              })
              .on((Function<IOException>) Throwable::printStackTrace)
              .on(Event.STATUS.name(), t -> System.out.println("STATUS " + t))
              .on(Event.HEADERS.name(), t -> System.out.println("HEADERS " + t))
              .on(Event.MESSAGE_BYTES.name(), t -> System.out.println("MESSAGE_BYTES " + t))
              .on(Event.OPEN.name(), t -> System.out.println("OPEN " + t))
              .open(request.build());

      // put as many attribs as possible in
      Connection connection = new Connection(uuid, srcId, gatewayFullName);

      // connection specific
      connection.put("c-type", "Runtime");
      // attributes.put("c-endpoint", endpoint);
      connection.put("c-client", this);

      // cli specific
      connection.put("cwd", "/");
      connection.put("url", url);
      connection.put("uri", url); // not really correct
      connection.put("user", "root");
      connection.put("host", "local");

      // addendum
      connection.put("User-Agent", "runtime-client");

      // send describe
      // clientRemote.send(uuid.toString(), CodecUtils.toJson(msg));

      return connection;

    } catch (Exception e) {
      log.error("connect {} threw", url, e);
    }
    return null;
  }

  @Override
  public Reader decode(Event e, String dataIn) {
    // public Reader decode(Event type, String data) {
    // System.out.println("=========== decode <----- ===========");
    // System.out.println("decoding [{} - {}]", type, s);

    // Null check handled by equals()
    // This checks for the heartbeat, we just ignore it
    // cause the heartbeat just keeps the connection up
    if ("X".equals(dataIn)) {
      // System.out.println("MESSAGE - X");
      return null;
    }

    // Ignores all the other events like OPENED or CLOSED
    if(!(e.equals(Event.MESSAGE) || e.equals(Event.MESSAGE_BYTES))) {
      return null;
    }

    // main response
    // System.out.println(data);
    for (RemoteMessageHandler handler : handlers) {
      handler.onRemoteMessage(uuid, dataIn);
    }

    // response
    // System.out.println("OPENED" + s);

    return new StringReader(dataIn);
    // return null;
  }

  // FIXME - should be Message type ...
  //  and WsClient should encode it !!!
  public void send(String raw) {
    try {
      socket.fire(raw);
    } catch (Exception e) {
      log.error("send threw", e);
    }
  }

  public String getUuid() {
    return uuid;
  }

  public void close() {
    if (socket != null) {
      socket.close();
    }
    if (asc != null) {
      try {
        asc.close();
      } catch (IOException e) { /* don't care */
      }
    }
  }

}
