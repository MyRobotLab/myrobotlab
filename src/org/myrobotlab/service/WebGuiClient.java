package org.myrobotlab.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.OptionsBuilder;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
// import org.atmosphere.wasync.m;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.DefaultOptions;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

public class WebGuiClient extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebGuiClient.class);

  transient AtmosphereClient client;

  transient Map<String, Socket> connections = new HashMap<String, Socket>();

  String defaultPrefix;

  transient private Socket socket;

  public WebGuiClient(String n) {
    super(n);
    defaultPrefix = String.format("%s.", n);
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

    ServiceType meta = new ServiceType(WebGuiClient.class.getCanonicalName());
    meta.addDescription("used as a general webguiclient");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    // FIXME - not quite ready for prime-time, need good network overlay which won't
    // happen until post-manticore release
    meta.addCategory("general");
    return meta;
  }

  public void connect(String url) throws IOException, URISyntaxException {

    // Atmosphere - FIXME - be more general when things begin working !!!
    // like below..
    // Client client =
    // ClientFactory.getDefault().newClient(AtmosphereClient.class);
    // TODO - security needs to be done here ..

    final URI uri = new URI(url);
    client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

    RequestBuilder<?> request = client.newRequestBuilder()
        // .method(Request.METHOD.GET) // get is required initially for
        // websockets ?
        .uri(url) // the url
        .trackMessageLength(false) // Atmosphere feature of max frame size ?
        .header("id", Runtime.getId()) // should be MRL.id static field value
        .encoder(new Encoder<Message, String>() {

          @Override
          public String encode(Message data) {
            return CodecUtils.toJson(data);
          }
        }).decoder(new Decoder<String, Message>() {

          @Override
          public Message decode(Event type, String data) {

            data = data.trim();

            // Padding from Atmosphere, skip
            if (data.length() == 0) {
              return null;
            }

            if (type.equals(Event.MESSAGE)) {

              log.info("============= Msg !!! ==================");
              log.info("{}", data);

              // double decode ? - api.process(msg) ??
              Message msg = CodecUtils.fromJson(data, Message.class);
              return msg;

            } else {
              return null;
            }
          }
        }).transport(Request.TRANSPORT.WEBSOCKET).transport(Request.TRANSPORT.SSE).transport(Request.TRANSPORT.LONG_POLLING);

    /*
     * RequestBuilder<?> requestBuilder =
     * client.newRequestBuilder().method(Request.METHOD.GET).uri(url)
     * .transport(Request.TRANSPORT.WEBSOCKET).transport(Request.TRANSPORT. SSE)
     * .transport(Request.TRANSPORT.LONG_POLLING);
     */

    // ================================================================

    NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
    nettyConfig.addProperty("child.tcpNoDelay", "true");
    nettyConfig.addProperty("child.keepAlive", "true");
    nettyConfig.addProperty("child.reuseAddress", true);
    nettyConfig.addProperty("child.connectTimeoutMillis", 60000);
    nettyConfig.setWebSocketMaxFrameSize(131072);

    AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setAsyncHttpClientProviderConfig(nettyConfig).build();

    // AsyncHttpClientConfig c = new
    // AsyncHttpClientConfig().setAsyncHttpClientProviderConfig(config).build();
    AsyncHttpClient ahc = new AsyncHttpClient(new NettyAsyncHttpProvider(config));
    // HERE ===> Socket socket =
    // client.create(client.newOptionsBuilder().runtime(ahc).build());

    // ================================================================

    OptionsBuilder<?, ?> optionsBuilder = client.newOptionsBuilder();
    optionsBuilder.requestTimeoutInSeconds(-1);
    optionsBuilder.runtime(ahc);
    // optionsBuilder.re
    optionsBuilder.runtimeShared(true);

    DefaultOptions options = new DefaultOptions(optionsBuilder);

    socket = client.create(options);

    // socket.open(requestBuilder.build());

    socket.on("message", new Function<Message>() {

      @Override
      public void on(Message msg) {
        // Date d = new Date(t.getTime());
        log.info("msg {}", msg);
        // if (msg.method.equals("onRegistered")) {
        if (msg.method.equals("onLocalServices")) {
          
        }
        
        if (msg.method.equals("onRegistered")) {
          Object[] msgData = msg.data;
          ServiceInterface si = null;

          // ALLOWED TO BE NULL - establishes initial contact & a
          // ServiceEnvironment
          if (msgData != null) {
            si = (ServiceInterface) msg.data[0];
            si.setInstanceId(uri);
            String xForwardDataName = String.format("%s%s", getPrefix(uri), si.getName());
            si.setName(xForwardDataName);
            send(Runtime.getInstance().getName(), "register", si, uri);
          }

        }
      }
    }).on(new Function<Throwable>() {

      @Override
      public void on(Throwable t) {
        t.printStackTrace();
      }
    }).on(Event.OPEN.name(), new Function<String>() {

      @Override
      public void on(String message) {
        log.info("on OPEN {}", message);
      }
    }).on(new Function<String>() {

      @Override
      public void on(String message) {
        log.info("onFunction<String>");
      }
    }).on(new Function<Request.TRANSPORT>() {

      @Override
      public void on(Request.TRANSPORT t) {
        log.info("on TRANSPORT {}", t);
      }
    }).on(Event.CLOSE.name(), new Function<String>() {
      @Override
      public void on(String t) {
        log.info("on CLOSE {}", t);
      }
    }).open(request.build());

    // socket.

    // connections.put(url, socket);

    // FIXME - publishConnect(uri) -> Gateway !!!
  }

  public String getPrefix(URI protocolKey) {
    return defaultPrefix;
  }

  public void sendRemote(final String urlKey, final Message msg) throws IOException, URISyntaxException {
    if (!connections.containsKey(urlKey)) {
      connect(urlKey);
    }

    Socket socket = connections.get(urlKey);

    socket.fire(msg);

    // socket.close();

  }

  public void sendRemote(final String urlKey, final String name, final String method) throws IOException {
    sendRemote(urlKey, name, method, (Object[]) null);
  }

  // FIXME - gateway interface should be final and should allow throw...
  public void sendRemote(final String urlKey, final String name, final String method, final Object... data) throws IOException {
    Message msg = Message.createMessage(this, name, method, data);
    socket.fire(CodecUtils.toJson(msg));
  }

  public void sendRemote(final URI key, final Message msg) {

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.DEBUG);

      WebGuiClient client = (WebGuiClient) Runtime.start("webguiclient", "WebGuiClient");
      client.connect("http://localhost:8888/api/messages");
      
      // this returns with a bajillion registry entries .. which need to be digested and 
      // prefixed
      
      // perhaps its best to specificall request
      // 1. hello
      // 2. register me
      // 3. ask for register'able service
      // currently the server immediately dumps all info ... dunno

      // returns - the fact its a ws &
      // returns - Platform or "Hello" of server
      /*
       * for (int i = 0; i < 10000; ++i) { // send -> say hello
       * 
       * // block <- on return hello
       * 
       * // send -> list services
       * 
       * // block <- list services (not block - but event handled)
       * 
       */
      
      client.subscribe("runtime", "getUptime");
      // client.socket.fire(Message.createMessage(client, "runtime", "getUptime", null));
      
      for (int i = 0; i < 100000; ++i) {
        client.socket.fire(Message.createMessage(client, "runtime", "getUptime", null));
        Message msg = Message.createMessage(client, "runtime", "getUptime", null);
      }
      
      // client.socket.fire(msg);
      // client.connect("http://localhost:8888/api/messages");
      // client.sendRemote("http://localhost:8888/api/messages",
      // "runtime", "getUptime");
      // Message msg = client.createMessage("runtime", "getUptime", null);
      // client.socket.fire(msg);
      // log.info("here");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
