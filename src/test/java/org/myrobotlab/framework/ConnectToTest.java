package org.myrobotlab.framework;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Agent;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

@Ignore // temporarily ignoring until wasync does a clean shutdown ...
public class ConnectToTest extends AbstractTest {
  public final static Logger log = LoggerFactory.getLogger(ConnectToTest.class);

  // TODO - 2 modes - synchronous (service) and asynchronous (message)
  // TODO - this is default ... ? try atmosphere client ?
  static public void connectTo3(String url) {
    try {
      Client client = ClientFactory.getDefault().newClient();
      // what benefits are there with the atmosphere client ?
      // AtmosphereClient client =
      // ClientFactory.getDefault().newClient(AtmosphereClient.class);

      RequestBuilder<?> request = client.newRequestBuilder().method(Request.METHOD.GET).uri(url).encoder(new Encoder<String, Reader>() { // Stream
        @Override
        public Reader encode(String s) {
          log.info("=========== encode -----> ===========");
          log.info("encoding [{}]", s);
          return new StringReader(s);
        }
      }).decoder(new Decoder<String, Reader>() {
        @Override
        public Reader decode(Event type, String s) {
          log.info("=========== decode <----- ===========");
          log.info("decoding [{} - {}]", type, s);
          return new StringReader(s);
        }
      }).transport(Request.TRANSPORT.WEBSOCKET) // Try WebSocket
          .transport(Request.TRANSPORT.LONG_POLLING); // Fallback to
                                                      // Long-Polling

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

      // AsyncHttpClientConfig Config
      AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
      b.setFollowRedirect(true).setMaxRequestRetry(-1).setConnectTimeout(-1).setReadTimeout(30000);
      AsyncHttpClientConfig config = b.setAsyncHttpClientProviderConfig(nettyConfig).build();
      AsyncHttpClient asc = new AsyncHttpClient(config);

      Socket socket = client.create(client.newOptionsBuilder().runtime(asc).build());
      socket.on(Event.CLOSE.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("CLOSE {}", t);
        }
      }).on(Event.REOPENED.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("REOPENED {}", t);
        }
      }).on(Event.MESSAGE.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("MESSAGE {}", t);
        }
      }).on(new Function<IOException>() {
        @Override
        public void on(IOException ioe) {
          ioe.printStackTrace();
        }
      }).on(Event.STATUS.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("STATUS {}", t);
        }
      }).on(Event.HEADERS.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("HEADERS {}", t);
        }
      }).on(Event.MESSAGE_BYTES.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("MESSAGE_BYTES {}", t);
        }
      }).on(Event.OPEN.name(), new Function<String>() {
        @Override
        public void on(String t) {
          log.info("OPEN {}", t);
        }
      }).open(request.build());

      Message msg = Message.createMessage("client", "runtime", "getUptime", null);
      String payload = CodecUtils.toJson(msg);
      socket.fire(payload);

      log.info("here");

      msg = Message.createMessage("client", "runtime", "getUptime", null);
      payload = CodecUtils.toJson(msg);
      socket.fire(payload);
      
      socket.close();

    } catch (Exception e) {
      log.error("something threw", e);
    }
  }

  public static void main(String[] args) {
    // LoggingFactory.init(Level.INFO);

    try {

      ConnectToTest ct = new ConnectToTest();
      ct.connectTo();

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @Test
  public void connectTo() throws Exception {
    Agent agent = (Agent)Runtime.start("agent", "Agent");
    agent.startWebGui();
    // connectTo3("http://localhost:8887/api/messages");
    log.info("end");
  }

}
