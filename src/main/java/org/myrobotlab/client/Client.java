package org.myrobotlab.client;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Clock;
import org.slf4j.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

/**<pre>
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
</pre>
*/
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * This class is a minimal Java websocket client which can attach to a MyRobotLab running instance.
 * From the command line it should be capable of sending any command using the Cli Api notation.
 * There are system commands such as ls, lp, cd, etc - but most invoking of service methods will be of the
 * form
 * 
 *  /{service}/{method}/{param0}/{param1}....
 *  
 * @author GroG
 *
 */
@Command(mixinStandardHelpOptions = true, name = "myrobotlab-client.jar", version = "0.0.1")
public class Client {
  
  public final static Logger log = LoggerFactory.getLogger(Client.class);

  @Option(names = "--option", description = "Some option.")
  String option;

  @Option(names = { "-a", "--alias" }, description = "alias of url")
  String alias;

  @Option(names = { "-u", "--url" }, arity = "1..*", description = "client endpoints")
  // String[] urls = new String[] { "http://192.168.0.73:8887/api/cli" };
  String url = "http://127.0.0.1:8887/api/cli";
  // String url = "http://192.168.0.73:8887/api/cli";
  // String[] urls = new String[] { "http://127.0.0.1:8887/api/cli" };

  @Option(names = { "-p", "--password" }, description = "password to authenticate")
  String password = null;

  @Option(names = { "-v", "--verbose" }, description = "Be verbose.")
  boolean verbose = false;

  String currentUuid = null;

  final transient Worker worker = new Worker();

  // FIXME - this should be promoted to Gateway !!!
  public static interface RemoteMessageHandler {
    public void onRemoteMessage(String uuid, String data);
  }

  public class Endpoint implements Decoder<String, Reader> {
    public String uuid;
    public String uri;
    transient public Socket socket;

    @Override
    public Reader decode(Event e, String dataIn) {
      //public Reader decode(Event type, String data) {
        // System.out.println("=========== decode <----- ===========");
        // System.out.println("decoding [{} - {}]", type, s);
      String data = (String) dataIn;
        if (data != null && "X".equals(data)) {
          // System.out.println("MESSAGE - X");
          return null;
        }
        if ("OPEN".equals(data)) {
          return null;
        }

        // main response
        //System.out.println(data);
        for (RemoteMessageHandler handler : handlers) {
          handler.onRemoteMessage(uuid, data);
        }

        // response
        // System.out.println("OPENED" + s);

        return new StringReader(data);
        //return null;
      }
  }

  Set<RemoteMessageHandler> handlers = new HashSet<>();
  
  public class AutoConnector implements Runnable {
    transient Thread worker;
    boolean isRunning = false;
    long interval = 1000;
    
    public void run() {
      isRunning = true;
      while(isRunning) {
        try {
        Thread.sleep(interval);
        } catch(Exception e) {
          isRunning = false;
        }
        for (String url : endpoints.keySet()) {
          try {
            if (endpoints.get(url).socket == null) {
              connect(url);
            }
          } catch(Exception e) {
            log.error("could not connect to {}", url, e);
          }
        }
      }
      isRunning = false;
      worker = null;
    }
    
    synchronized public void start() {
      if (worker == null) {
        worker = new Thread(this, "auto-connector");
        worker.start();
      }
    }
    
   synchronized public void stop() {
     isRunning = false;
    }
  }
  

  public void addResponseHandler(RemoteMessageHandler handler) {
    handlers.add(handler);
  }

  // FIXME !! - 2 modes - messages non-blocking and blocking service mode !! -
  // service mode more useful ?
  public static void main(String[] args) {
    try {
      // Logger logger = (Logger)
      // LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      // logger.setLevel(Level.INFO);

      // Logger.getRootLogger().setLevel(Level.INFO);

      Client client = CommandLine.populateCommand(new Client(), args);

      UUID uuid = java.util.UUID.randomUUID();
      client.connect(uuid.toString(), client.url);

      // if interactive vs non-interactive which will pretty much be curl ;P BUT
      // BLOCKING !!! (ie useful)

      client.startInteractiveMode();

      // System.out.println("password {}", password);

    } catch (Exception e) {
      // log.error("main threw", e);
      e.printStackTrace();
    }
  }

  Map<String, Endpoint> endpoints = new TreeMap<>();

  public Endpoint connect(String url) {
    return connect(null, url);
  }

  public Endpoint connect(String uuid, String url) {
    try {

      Endpoint endpoint = new Endpoint();
      
      if (uuid == null) {
        UUID u = java.util.UUID.randomUUID();
        uuid = u.toString();
      }

      endpoint.uuid = uuid;
      endpoints.put(uuid, endpoint);

      org.atmosphere.wasync.Client client = ClientFactory.getDefault().newClient();
      // what benefits are there with the atmosphere client ?
      // AtmosphereClient client =
      // ClientFactory.getDefault().newClient(AtmosphereClient.class);

      RequestBuilder<?> request = client.newRequestBuilder();
      request.method(Request.METHOD.GET);
      request.uri(url);
      request.encoder(new Encoder<String, Reader>() { // Stream
        @Override
        public Reader encode(String s) {
          // System.out.println("=========== encode -----> ===========");
          // System.out.println("encoding [{}]", s);
          return new StringReader(s);
        }
      }).decoder(endpoint
          ).transport(Request.TRANSPORT.WEBSOCKET) // Try WebSocket
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
      // nettyConfig.setWebSocketMaxFrameSize(65536);

      // AsyncHttpClientConfig Config
      AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
      b.setFollowRedirect(true).setMaxRequestRetry(-1).setConnectTimeout(-1).setReadTimeout(30000);
      AsyncHttpClientConfig config = b.setAsyncHttpClientProviderConfig(nettyConfig).build();
      AsyncHttpClient asc = new AsyncHttpClient(config);
      

      Socket socket = client.create(client.newOptionsBuilder().reconnect(true).reconnectAttempts(999).runtime(asc).build());
      socket.on(Event.CLOSE.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("CLOSE " + t);
        }
      }).on(Event.REOPENED.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("REOPENED " + t);
        }
      }).on(Event.MESSAGE.name(), new Function<String>() {
        @Override
        public void on(String t) {
          // all messages
          // System.out.println("MESSAGE {}", t);
        }
      }).on(new Function<IOException>() {
        @Override
        public void on(IOException ioe) {
          ioe.printStackTrace();
        }
      }).on(Event.STATUS.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("STATUS " + t);
        }
      }).on(Event.HEADERS.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("HEADERS " + t);
        }
      }).on(Event.MESSAGE_BYTES.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("MESSAGE_BYTES " + t);
        }
      }).on(Event.OPEN.name(), new Function<String>() {
        @Override
        public void on(String t) {
          System.out.println("OPEN " + t);
        }
      }).open(request.build());

  
      endpoint.socket = socket;
      currentUuid = uuid;

      return endpoint;

    } catch (Exception e) {
      log.error("connect {} threw", url, e);
    }
    return null;
  }
  

  public class Worker implements Runnable {

    Thread myThread = null;

    public Worker() {
    }

    public void start() {
      if (myThread == null) {
        myThread = new Thread(this, "client-stdin-worker");
        myThread.start();
      }
    }

    @Override
    public void run() {
      try {
        int c = '\n';
        String readLine = "";
        while ((c = System.in.read()) != 0x04 /* ctrl-d 0x04 ctrl-c 0x03 '\n' */) {
          System.out.print((char) c);
          readLine += (char) c;
          if (c == '\n') {
            Endpoint resource = endpoints.get(currentUuid);
            resource.socket.fire(readLine);
            readLine = "";
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      myThread = null;
    }

    public void stop() {
      if (myThread != null) {
        myThread.interrupt();
      }
    }
  }

  public void send(String uuid, String raw) {
    try {
      Endpoint resource = endpoints.get(uuid);
      resource.socket.fire(raw);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // FIXME - initial hello with Runtime authentication authorization - then
  // redirection to a cli its name?
  // default to "cli"
  // FIXME !!! easy subscribe / unsubscribe and auto subscribe to cli ? if cli
  // exists ???
  public void startInteractiveMode() {
    worker.start();
  }

  public void stopInteractiveMode() {
    worker.stop();
  }
  
  public void broadcast(String raw) {
    for (Endpoint endpoint : endpoints.values()) {
      try {
        endpoint.socket.fire(raw);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
